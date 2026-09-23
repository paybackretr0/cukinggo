package com.khalied.cukinggo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.di.AppContainer
import com.khalied.cukinggo.domain.model.Cat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tiga keadaan yang dipakai layar Home. Dipisah eksplisit supaya "belum termuat"
 * tidak pernah ditampilkan sebagai "belum ada kucing".
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val cats: List<Cat>) : HomeUiState
    data object Failed : HomeUiState
}

class HomeViewModel(
    private val catRepository: CatRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = refreshTrigger
        .flatMapLatest {
            catRepository.getAllCats()
                .map<List<Cat>, HomeUiState> { cats -> HomeUiState.Content(cats) }
                .catch { emit(HomeUiState.Failed) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun deleteCat(cat: Cat) {
        viewModelScope.launch { catRepository.deleteCat(cat) }
    }

    fun retry() {
        refreshTrigger.value += 1
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(container.catRepository) }
        }
    }
}
