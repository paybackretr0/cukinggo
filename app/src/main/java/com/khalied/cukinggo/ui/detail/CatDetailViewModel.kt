package com.khalied.cukinggo.ui.detail

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
 * [Missing] hanya berarti datanya sudah tidak ada di database, sedangkan
 * "belum termuat" diwakili [Loading]. Dua hal itu tidak lagi dipaksa satu nilai.
 */
sealed interface CatDetailUiState {
    data object Loading : CatDetailUiState
    data class Content(val cat: Cat) : CatDetailUiState
    data object Missing : CatDetailUiState
    data object Failed : CatDetailUiState
}

class CatDetailViewModel(
    private val catId: Long,
    private val catRepository: CatRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CatDetailUiState> = refreshTrigger
        .flatMapLatest {
            catRepository.observeCat(catId)
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

    fun retry() {
        refreshTrigger.value += 1
    }

    fun deleteCat(onDeleted: () -> Unit) {
        viewModelScope.launch {
            (uiState.value as? CatDetailUiState.Content)?.cat?.let { catRepository.deleteCat(it) }
            onDeleted()
        }
    }

    companion object {
        fun factory(catId: Long, container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CatDetailViewModel(catId, container.catRepository) }
            }
    }
}
