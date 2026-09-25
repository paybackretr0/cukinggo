package com.khalied.cukinggo.ui.addcat

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
import com.khalied.cukinggo.location.LocationHelper
import com.khalied.cukinggo.util.ImageStorageHelper
import com.khalied.cukinggo.util.catStreak
import com.khalied.cukinggo.util.streakToCelebrate
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Id cuking yang berarti "bikin cuking baru", bukan menambah penemuan ke cuking
 * yang sudah ada. Nol dipakai karena Room memang tidak pernah memberi id nol.
 */
const val NEW_CAT_ID = 0L

sealed interface AddCatUiState {
    data object Idle : AddCatUiState
    data object Locating : AddCatUiState
    data object Saving : AddCatUiState

    /**
     * [streakDays] berisi panjang rentetan harian kalau catatan barusan
     * memanjangkannya, dan null kalau tidak. Layar memakainya untuk memilih
     * versi perayaannya, jadi keadaannya tetap satu dan bukan dua keadaan
     * "berhasil" yang berbeda.
     */
    data class Saved(val streakDays: Int? = null) : AddCatUiState

    data class Error(@StringRes val messageRes: Int) : AddCatUiState
}

/**
 * Layar kamera melayani dua hal: menandai cuking baru, dan menambah penemuan
 * untuk cuking yang sudah ada. Yang membedakan cuma [catId], dan yang berbeda di
 * alurnya cuma nama: nama cuma ditanyakan saat cukingnya memang baru.
 */
class AddCatViewModel(
    private val catRepository: CatRepository,
    private val locationHelper: LocationHelper,
    private val imageStorageHelper: ImageStorageHelper,
    private val catId: Long = NEW_CAT_ID
) : ViewModel() {

    val isNewCat: Boolean = catId == NEW_CAT_ID

    /**
     * Cuking yang sedang ditambahi penemuan, dipakai layar untuk menyebut namanya
     * supaya jelas penemuan ini masuk ke cuking yang mana. Null untuk cuking baru.
     */
    val targetCat: StateFlow<Cat?> = if (isNewCat) {
        MutableStateFlow(null)
    } else {
        catRepository.observeCat(catId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    private val _uiState = MutableStateFlow<AddCatUiState>(AddCatUiState.Idle)
    val uiState: StateFlow<AddCatUiState> = _uiState.asStateFlow()

    /**
     * Ambil GPS saat simpan (tanpa input manual), pindahkan foto ke internal
     * storage, lalu simpan catatannya ke Room.
     */
    fun saveCat(captureFile: File?, name: String, description: String) {
        if (captureFile == null || !captureFile.exists()) {
            _uiState.value = AddCatUiState.Error(R.string.add_error_no_photo)
            return
        }
        if (_uiState.value is AddCatUiState.Locating || _uiState.value is AddCatUiState.Saving) return

        viewModelScope.launch {
            _uiState.value = AddCatUiState.Locating
            val location = locationHelper.getCurrentLocation()
            if (location == null) {
                _uiState.value = AddCatUiState.Error(R.string.add_error_location)
                return@launch
            }

            // Cukingnya bisa saja sudah dihapus dari layar lain selagi layar ini
            // terbuka. Tanpa pemeriksaan ini, penemuannya jadi penemuan yatim.
            if (!isNewCat && catRepository.getCat(catId) == null) {
                _uiState.value = AddCatUiState.Error(R.string.add_error_cat_missing)
                return@launch
            }

            _uiState.value = AddCatUiState.Saving

            // Rentetan dihitung sebelum dan sesudah menyimpan, dari waktu
            // catatan yang sudah ada, sama seperti chip di Home dan lencana di
            // widget. Selisih keduanya yang menjawab "apakah rentetannya baru
            // saja memanjang", tanpa perlu ada angka tersimpan yang bisa salah.
            val today = LocalDate.now()
            val streakBefore = catStreak(catRepository.getAllTimestamps(), today)

            runCatching {
                val photoPath = withContext(Dispatchers.IO) {
                    imageStorageHelper.moveCaptureToInternalStorage(captureFile)
                }
                if (isNewCat) {
                    catRepository.addCat(
                        photoPath = photoPath,
                        name = name,
                        description = description,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    catRepository.addSighting(
                        catId = catId,
                        photoPath = photoPath,
                        description = description,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            }.onSuccess {
                val streakAfter = catStreak(catRepository.getAllTimestamps(), today)
                _uiState.value = AddCatUiState.Saved(
                    streakDays = streakToCelebrate(streakBefore, streakAfter)
                )
            }.onFailure {
                _uiState.value = AddCatUiState.Error(R.string.add_error_save)
            }
        }
    }

    fun clearError() {
        if (_uiState.value is AddCatUiState.Error) {
            _uiState.value = AddCatUiState.Idle
        }
    }

    companion object {
        fun factory(container: AppContainer, catId: Long = NEW_CAT_ID): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AddCatViewModel(
                        catRepository = container.catRepository,
                        locationHelper = container.locationHelper,
                        imageStorageHelper = container.imageStorageHelper,
                        catId = catId
                    )
                }
            }
    }
}
