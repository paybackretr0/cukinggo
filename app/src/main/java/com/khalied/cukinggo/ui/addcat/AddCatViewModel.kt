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
import com.khalied.cukinggo.location.LocationHelper
import com.khalied.cukinggo.util.ImageStorageHelper
import com.khalied.cukinggo.util.catStreak
import com.khalied.cukinggo.util.streakToCelebrate
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

class AddCatViewModel(
    private val catRepository: CatRepository,
    private val locationHelper: LocationHelper,
    private val imageStorageHelper: ImageStorageHelper
) : ViewModel() {

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
                catRepository.addCat(
                    photoPath = photoPath,
                    name = name,
                    description = description,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
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
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddCatViewModel(
                    catRepository = container.catRepository,
                    locationHelper = container.locationHelper,
                    imageStorageHelper = container.imageStorageHelper
                )
            }
        }
    }
}
