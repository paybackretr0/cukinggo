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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface AddCatUiState {
    data object Idle : AddCatUiState
    data object Locating : AddCatUiState
    data object Saving : AddCatUiState
    data object Saved : AddCatUiState
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
    fun saveCat(captureFile: File?, description: String) {
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
            runCatching {
                val photoPath = withContext(Dispatchers.IO) {
                    imageStorageHelper.moveCaptureToInternalStorage(captureFile)
                }
                catRepository.addCat(
                    photoPath = photoPath,
                    description = description,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            }.onSuccess {
                _uiState.value = AddCatUiState.Saved
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
