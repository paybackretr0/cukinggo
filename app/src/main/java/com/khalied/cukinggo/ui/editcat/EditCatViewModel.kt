package com.khalied.cukinggo.ui.editcat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.di.AppContainer
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Nilai awal yang diisi ke form, dibaca sekali dari catatan yang mau diedit. */
data class EditCatForm(
    val catId: Long,
    val name: String,
    val description: String,
    val photoPath: String,
    /** Video yang tersimpan di catatan ini, null kalau penemuan ini foto saja. */
    val videoPath: String?
)

/**
 * Keadaan layar edit.
 *
 * [Ready] membawa [saving] dan [error] sekali pakai di dalamnya, bukan sebagai
 * keadaan terpisah, supaya form yang sedang diisi tidak pernah hilang dari layar
 * cuma karena tombol simpannya sedang ditekan atau simpanannya gagal.
 */
sealed interface EditCatUiState {
    data object Loading : EditCatUiState
    data object Missing : EditCatUiState
    data object Failed : EditCatUiState

    data class Ready(
        val form: EditCatForm,
        val saving: Boolean = false,
        val error: Boolean = false
    ) : EditCatUiState

    data object Saved : EditCatUiState
}

/**
 * Layar edit satu penemuan: nama cukingnya, catatan kegiatannya, dan fotonya.
 *
 * Dibuka dengan id penemuan, sama seperti layar detail, karena yang bisa disentuh
 * pengguna di daftar dan peta memang satu penemuan.
 */
class EditCatViewModel(
    private val sightingId: Long,
    private val catRepository: CatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditCatUiState>(EditCatUiState.Loading)
    val uiState: StateFlow<EditCatUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _uiState.value = EditCatUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching { catRepository.observeCatOfSighting(sightingId).first() }
                .onSuccess { cat ->
                    val sighting = cat?.sightings?.firstOrNull { it.id == sightingId }
                    _uiState.value = if (cat == null || sighting == null) {
                        EditCatUiState.Missing
                    } else {
                        EditCatUiState.Ready(
                            form = EditCatForm(
                                catId = cat.id,
                                name = cat.name.orEmpty(),
                                description = sighting.description.orEmpty(),
                                photoPath = sighting.photoPath,
                                videoPath = sighting.videoPath
                            )
                        )
                    }
                }
                .onFailure { _uiState.value = EditCatUiState.Failed }
        }
    }

    /**
     * Menyimpan perubahan sekaligus. [newPhotoCapture] null berarti fotonya tidak
     * diganti, jadi permintaan yang cuma mengganti nama atau catatan tidak ikut
     * menyentuh file foto.
     *
     * Video punya satu kemungkinan tambahan yang tidak dimiliki foto: dilepas.
     * [removeVideo] dipakai untuk itu, dan hanya berlaku kalau tidak ada
     * [newVideoCapture] yang menggantikannya.
     */
    fun save(
        name: String,
        description: String,
        newPhotoCapture: File?,
        newVideoCapture: File? = null,
        removeVideo: Boolean = false
    ) {
        val ready = _uiState.value as? EditCatUiState.Ready ?: return
        if (ready.saving) return

        _uiState.value = ready.copy(saving = true, error = false)
        viewModelScope.launch {
            runCatching {
                catRepository.updateCatName(ready.form.catId, name)
                catRepository.updateSighting(
                    sightingId = sightingId,
                    description = description,
                    newPhotoCapture = newPhotoCapture,
                    newVideoCapture = newVideoCapture,
                    removeVideo = removeVideo
                )
            }.onSuccess {
                _uiState.value = EditCatUiState.Saved
            }.onFailure {
                _uiState.value = ready.copy(saving = false, error = true)
            }
        }
    }

    companion object {
        fun factory(sightingId: Long, container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { EditCatViewModel(sightingId, container.catRepository) }
            }
    }
}
