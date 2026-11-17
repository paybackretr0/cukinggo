package com.khalied.cukinggo.ui.detail

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khalied.cukinggo.R
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.di.AppContainer
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.sticker.StickerMaker
import com.khalied.cukinggo.sticker.StickerResult
import com.khalied.cukinggo.util.ImageStorageHelper
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [Missing] hanya berarti datanya sudah tidak ada di database, sedangkan
 * "belum termuat" diwakili [Loading]. Dua hal itu tidak lagi dipaksa satu nilai.
 */
sealed interface CatDetailUiState {
    data object Loading : CatDetailUiState

    /** Cuking beserta seluruh penemuannya, urut terbaru dulu. */
    data class Content(val cat: Cat) : CatDetailUiState

    data object Missing : CatDetailUiState
    data object Failed : CatDetailUiState
}

/**
 * Keadaan pembuatan stiker susulan.
 *
 * [workingSightingId] berisi penemuan yang stikernya sedang dipotong, supaya tombol
 * di baris lain tidak ikut berputar, dan [errorRes] hanya terisi kalau percobaan
 * terakhir gagal. Keduanya kosong berarti tidak ada yang sedang dikerjakan.
 */
data class DetailStickerState(
    val workingSightingId: Long? = null,
    @StringRes val errorRes: Int? = null
)

/**
 * Semua pintu masuk ke layar ini menunjuk satu penemuan, bukan satu cuking:
 * kartu di daftar, penanda di peta, widget, dan kabar "dekat cuking" semuanya
 * mewakili satu momen ketemu. Jadi [sightingId] yang diterima, lalu cukingnya
 * dicari dari situ, dan layarnya menampilkan seluruh riwayat cuking itu.
 */
class CatDetailViewModel(
    val sightingId: Long,
    private val catRepository: CatRepository,
    private val stickerMaker: StickerMaker,
    private val imageStorageHelper: ImageStorageHelper
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CatDetailUiState> = refreshTrigger
        .flatMapLatest {
            catRepository.observeCatOfSighting(sightingId)
                .map<Cat?, CatDetailUiState> { cat ->
                    if (cat == null) CatDetailUiState.Missing else CatDetailUiState.Content(cat)
                }
                .catch { emit(CatDetailUiState.Failed) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CatDetailUiState.Loading
        )

    private val _stickerState = MutableStateFlow(DetailStickerState())
    val stickerState: StateFlow<DetailStickerState> = _stickerState.asStateFlow()

    fun retry() {
        refreshTrigger.value += 1
    }

    /**
     * Memotong latar foto penemuan lama dan menyimpannya sebagai stiker.
     *
     * Yang dikerjakan sama persis dengan tombol "Buat stiker" setelah menjepret,
     * cuma sumbernya foto yang sudah tersimpan. Hasilnya ditulis ke cache dulu, dan
     * baru pindah ke storage tetap setelah berhasil dicatat ke database, jadi
     * percobaan yang gagal tidak meninggalkan stiker yatim.
     */
    fun makeSticker(sighting: CatSighting) {
        if (_stickerState.value.workingSightingId != null) return

        viewModelScope.launch {
            _stickerState.value = DetailStickerState(workingSightingId = sighting.id)
            val photo = File(sighting.photoPath)
            if (!photo.isFile) {
                _stickerState.value = DetailStickerState(errorRes = R.string.add_error_no_photo)
                return@launch
            }

            val target = imageStorageHelper.createStickerTempFile()
            when (val result = stickerMaker.makeSticker(photo, target)) {
                is StickerResult.Ready -> {
                    val saved = runCatching {
                        catRepository.setSightingSticker(sighting.id, result.file)
                    }
                    _stickerState.value = if (saved.isSuccess) {
                        DetailStickerState()
                    } else {
                        imageStorageHelper.discardCapture(result.file)
                        DetailStickerState(errorRes = R.string.add_sticker_error_failed)
                    }
                }

                is StickerResult.NothingDetected -> {
                    imageStorageHelper.discardCapture(target)
                    _stickerState.value =
                        DetailStickerState(errorRes = R.string.add_sticker_error_empty)
                }

                is StickerResult.Failed -> {
                    imageStorageHelper.discardCapture(target)
                    _stickerState.value =
                        DetailStickerState(errorRes = R.string.add_sticker_error_failed)
                }
            }
        }
    }

    /**
     * Menghapus satu kegiatan, yaitu satu penemuan saja.
     *
     * Beda dengan [deleteCat]: yang dibuang di sini cuma penemuan yang dipilih,
     * sedangkan cukingnya tetap ada selama masih punya penemuan lain. Kalau ia
     * penemuan terakhir, lapisan data yang menghapus profilnya, dan layarnya
     * menampilkan keadaan "sudah tidak ada" seperti biasa.
     */
    fun deleteSighting(sighting: CatSighting) {
        viewModelScope.launch { catRepository.deleteSighting(sighting) }
    }

    /**
     * Dari layar ini yang dihapus adalah cukingnya beserta seluruh penemuannya,
     * bukan cuma penemuan yang sedang dibuka: layar ini memang layar cukingnya,
     * dan satu-satunya tombol hapus di sini tidak boleh menghapus sebagian tanpa
     * pengguna tahu.
     */
    fun deleteCat(onDeleted: () -> Unit) {
        viewModelScope.launch {
            (uiState.value as? CatDetailUiState.Content)?.cat?.let { catRepository.deleteCat(it) }
            onDeleted()
        }
    }

    companion object {
        fun factory(sightingId: Long, container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    CatDetailViewModel(
                        sightingId = sightingId,
                        catRepository = container.catRepository,
                        stickerMaker = container.stickerMaker,
                        imageStorageHelper = container.imageStorageHelper
                    )
                }
            }
    }
}
