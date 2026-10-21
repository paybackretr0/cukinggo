package com.khalied.cukinggo.ui.stickerpack

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khalied.cukinggo.data.local.StickerPackSelectionStore
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.di.AppContainer
import com.khalied.cukinggo.whatsapp.StickerPackExport
import com.khalied.cukinggo.whatsapp.StickerPackExporter
import com.khalied.cukinggo.whatsapp.StickerPackRejection
import com.khalied.cukinggo.whatsapp.StickerPackRules
import com.khalied.cukinggo.whatsapp.StickerPackSelection
import com.khalied.cukinggo.whatsapp.StickerSource
import com.khalied.cukinggo.whatsapp.WhatsAppStickers
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keadaan layar paket stiker.
 *
 * [stickers] hanya berisi stiker yang filenya benar-benar masih ada: kalau
 * dihitung dari catatan saja, angkanya bisa bilang "5 stiker" sementara yang
 * bisa dikirim cuma 2, dan tombolnya akan terlihat siap padahal tidak.
 *
 * [selected] disimpan sebagai path, dan yang dipakai saat mengekspor adalah
 * irisan antara pilihan itu dan [stickers] — jadi stiker yang catatannya dihapus
 * tidak bisa tertinggal di dalam paket.
 */
data class StickerPackUiState(
    val stickers: List<StickerSource> = emptyList(),
    val selected: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val exportFailed: Boolean = false,
    val lastExport: StickerPackExport? = null,
    val whatsAppInstalled: Boolean = true,
    val addedToWhatsApp: Boolean = false,
    /** Penolakan dari WhatsApp, kalau dialognya mengembalikannya. */
    val rejection: StickerPackRejection? = null
) {
    /** Stiker yang benar-benar ikut, urutannya sesuai tampilan (terbaru dulu). */
    val chosen: List<StickerSource>
        get() = stickers.filter { it.stickerPath in selected }

    val canExport: Boolean
        get() = !isExporting && StickerPackRules.canExport(chosen.size)

    /** Semua stiker yang ada sudah dipilih, jadi tidak ada lagi yang bisa ditambah. */
    val selectionFull: Boolean
        get() = selected.size >= StickerPackRules.MAX_STICKERS
}

/**
 * Menyiapkan paket stiker WhatsApp dari stiker yang sudah tersimpan.
 *
 * Layar ini tidak menyimpan daftar stiker sendiri: yang ditampilkan adalah
 * stiker yang benar-benar ada saat ini. Yang disimpan cuma pilihannya, sebagai
 * path file, supaya pilihan yang sama masih terpakai saat layar dibuka lagi.
 */
class StickerPackViewModel(
    // Application, bukan Context: ViewModel bisa hidup lebih lama dari layar,
    // dan menyimpan Context layar di sini berarti membocorkan activity.
    private val app: Application,
    catRepository: CatRepository,
    private val exporter: StickerPackExporter,
    private val selectionStore: StickerPackSelectionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StickerPackUiState(whatsAppInstalled = WhatsAppStickers.isInstalled(app))
    )
    val uiState: StateFlow<StickerPackUiState> = _uiState.asStateFlow()

    /**
     * Pilihan tersimpan, dibaca sekali.
     *
     * `null` berarti pengguna belum pernah memilih; pilihan bawaan baru dipasang
     * setelah daftar stiker pertamanya diketahui.
     */
    private var storedSelection: Set<String>? = selectionStore.load()

    init {
        viewModelScope.launch {
            catRepository.observeSightings()
                .map { sightings ->
                    withContext(Dispatchers.IO) {
                        sightings
                            .sortedByDescending { it.timestamp }
                            .mapNotNull { sighting ->
                                val path = sighting.stickerPath ?: return@mapNotNull null
                                if (!File(path).isFile) return@mapNotNull null
                                StickerSource(stickerPath = path, catName = sighting.catName)
                            }
                    }
                }
                .catch {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .flowOn(Dispatchers.Default)
                .collect { stickers -> applyStickers(stickers) }
        }
        refreshWhatsAppState()
    }

    /**
     * Mengetuk satu stiker: masukkan kalau belum dipilih, keluarkan kalau sudah.
     *
     * Hasilnya langsung disimpan, jadi pilihan tidak hilang saat layar ditutup.
     */
    fun toggleSticker(stickerPath: String) {
        val current = _uiState.value
        val next = StickerPackSelection.toggle(current.selected, stickerPath)
        if (next == current.selected) return

        storedSelection = next
        selectionStore.save(next)
        _uiState.value = current.copy(selected = next)
    }

    /**
     * Dicek ulang setiap layar kembali dibuka: pengguna bisa menambahkan atau
     * melepas paketnya di WhatsApp, dan app ini tidak diberi tahu soal itu.
     */
    fun refreshWhatsAppState() {
        _uiState.value = _uiState.value.copy(
            whatsAppInstalled = WhatsAppStickers.isInstalled(app),
            addedToWhatsApp = WhatsAppStickers.isPackAdded(app)
        )
    }

    /**
     * Menulis paket ke disk, lalu memanggil [onReady] kalau siap dibuka di
     * WhatsApp. Intent-nya sengaja dibuka dari layar, bukan dari sini: mulai
     * activity dari ViewModel membuat hasilnya tidak bisa dikaitkan dengan
     * layarnya.
     */
    fun export(onReady: () -> Unit) {
        val current = _uiState.value
        if (current.isExporting) return
        val sources = current.chosen
        _uiState.value = current.copy(isExporting = true, exportFailed = false)

        viewModelScope.launch {
            val result = runCatching { exporter.export(sources) }.getOrNull()
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportFailed = result == null,
                lastExport = result
            )
            if (result is StickerPackExport.Ready) onReady()
        }
    }

    /** Dipanggil setelah dialog WhatsApp ditutup, untuk memperbarui statusnya. */
    fun onReturnedFromWhatsApp() {
        refreshWhatsAppState()
    }

    /**
     * Menyimpan penolakan kalau WhatsApp mengirimkannya.
     *
     * WhatsApp menaruh penjelasannya di extra `validation_error` saat hasilnya
     * CANCELED. Tanpa ini, dialog yang menutup sendiri cuma terlihat seperti
     * tombol yang tidak bereaksi, padahal app tujuan bilang apa yang salah.
     *
     * Yang disimpan sudah diterjemahkan ke [StickerPackRejection], karena
     * sebagian alasan butuh penjelasan sendiri dan sebagian lain cuma bisa
     * ditampilkan apa adanya.
     */
    fun onAddPackResult(resultCode: Int, data: Intent?) {
        val reason = if (resultCode == Activity.RESULT_CANCELED) {
            data?.getStringExtra(EXTRA_VALIDATION_ERROR)
        } else {
            null
        }
        _uiState.value = _uiState.value.copy(
            rejection = StickerPackRejection.from(reason, WhatsAppStickers.authority(app))
        )
        refreshWhatsAppState()
    }

    /**
     * Memasang daftar stiker terbaru, sekaligus merapikan pilihan.
     *
     * Aturan "path yang hilang dibuang, tapi pilihan yang jadi kosong karena itu
     * diisi ulang dengan bawaan" ada di [StickerPackSelection.resolve], supaya
     * bisa diuji tanpa Android.
     */
    private fun applyStickers(stickers: List<StickerSource>) {
        val available = stickers.map { it.stickerPath }
        val stored = storedSelection
        val selection = StickerPackSelection.resolve(stored, available)

        if (selection != stored) {
            storedSelection = selection
            selectionStore.save(selection)
        }

        _uiState.value = _uiState.value.copy(
            stickers = stickers,
            selected = selection,
            isLoading = false
        )
    }

    companion object {
        private const val EXTRA_VALIDATION_ERROR = "validation_error"

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StickerPackViewModel(
                    app = container.appContext as Application,
                    catRepository = container.catRepository,
                    exporter = container.stickerPackExporter,
                    selectionStore = StickerPackSelectionStore(container.appContext)
                )
            }
        }
    }
}
