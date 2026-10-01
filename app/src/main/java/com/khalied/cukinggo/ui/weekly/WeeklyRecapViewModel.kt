package com.khalied.cukinggo.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.di.AppContainer
import com.khalied.cukinggo.util.WeeklyRecap
import com.khalied.cukinggo.util.weeklyRecap
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface WeeklyRecapUiState {
    data object Loading : WeeklyRecapUiState
    data class Content(val recap: WeeklyRecap) : WeeklyRecapUiState
    data object Failed : WeeklyRecapUiState
}

/**
 * Rekap tujuh hari terakhir.
 *
 * Angkanya dihitung dari data yang sudah ada, bukan disimpan: catatan yang baru
 * ditandai atau dihapus langsung ikut terhitung, dan tidak ada ringkasan
 * tersimpan yang bisa berbeda dari isi koleksinya.
 */
class WeeklyRecapViewModel(
    catRepository: CatRepository
) : ViewModel() {

    val uiState: StateFlow<WeeklyRecapUiState> = combine(
        catRepository.observeSightings(),
        catRepository.observeCatProfiles()
    ) { sightings, catProfiles ->
        weeklyRecap(
            sightings = sightings,
            catProfiles = catProfiles,
            today = LocalDate.now()
        )
    }
        .map<WeeklyRecap, WeeklyRecapUiState> { recap -> WeeklyRecapUiState.Content(recap) }
        .catch { emit(WeeklyRecapUiState.Failed) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeeklyRecapUiState.Loading
        )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { WeeklyRecapViewModel(container.catRepository) }
        }
    }
}
