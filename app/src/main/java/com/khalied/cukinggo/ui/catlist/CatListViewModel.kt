package com.khalied.cukinggo.ui.catlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.di.AppContainer
import com.khalied.cukinggo.domain.model.Cat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CatListViewModel(
    private val catRepository: CatRepository
) : ViewModel() {

    /**
     * Daftarnya di-cache di scope ViewModel, bukan dikumpulkan ulang setiap kali
     * layarnya disusun lagi: tanpa itu, menggulir jauh lalu memutar layar akan
     * memuat ulang dari halaman pertama.
     */
    val cats: Flow<PagingData<Cat>> = catRepository.pagingAllCats().cachedIn(viewModelScope)

    /**
     * Jumlah seluruh catatan, termasuk yang belum termuat di daftar.
     *
     * null berarti angkanya belum kebaca, dan chip jumlahnya disembunyikan selama
     * itu: angka 0 yang muncul sebentar sebelum catatannya terbaca terbaca seperti
     * "koleksimu kosong".
     */
    val catCount: StateFlow<Int?> = catRepository.observeCatCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Sama seperti di Home: kartu yang di-swipe hilang dari daftar tanpa dialog. */
    fun deleteCat(cat: Cat) {
        viewModelScope.launch { catRepository.deleteCat(cat) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { CatListViewModel(container.catRepository) }
        }
    }
}
