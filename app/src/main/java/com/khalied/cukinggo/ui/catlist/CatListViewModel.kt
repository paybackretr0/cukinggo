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
import com.khalied.cukinggo.domain.model.CatSighting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Saringan daftar: seluruh catatan, yang belum punya nama, atau yang paling sering
 * ketemu. Ketiganya membaca tabel yang sama, yang berbeda cuma urutan dan syaratnya.
 */
enum class CatListFilter { ALL, UNNAMED, MOST_SEEN }

/**
 * Jeda sebelum kata yang diketik dipakai mencari.
 *
 * Tanpa jeda, tiap huruf membatalkan dan memulai ulang query paging, jadi daftar
 * berkedip di tengah kata. Seperempat detik cukup untuk mengetik beberapa huruf
 * berturut-turut tanpa terasa lambat.
 */
private const val SEARCH_DEBOUNCE_MILLIS = 250L

class CatListViewModel(
    private val catRepository: CatRepository
) : ViewModel() {

    private val searchText = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(CatListFilter.ALL)

    /** Isi kolom cari, dipakai layar supaya kolomnya tetap mencerminkan keadaan. */
    val query: StateFlow<String> = searchText.asStateFlow()
    val filter: StateFlow<CatListFilter> = selectedFilter.asStateFlow()

    /**
     * Daftarnya di-cache di scope ViewModel, bukan dikumpulkan ulang setiap kali
     * layarnya disusun lagi: tanpa itu, menggulir jauh lalu memutar layar akan
     * memuat ulang dari halaman pertama.
     *
     * Pencarian dan saringan jadi kunci flow-nya: setiap perubahan memulai
     * PagingSource baru dengan syarat baru, dan hasil lama dibuang.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val sightings: Flow<PagingData<CatSighting>> =
        combine(searchText.debounce(SEARCH_DEBOUNCE_MILLIS), selectedFilter) { text, filter ->
            text.trim() to filter
        }
            .distinctUntilChanged()
            .flatMapLatest { (text, filter) ->
                catRepository.pagingSightings(
                    search = text,
                    onlyUnnamed = filter == CatListFilter.UNNAMED,
                    mostSeen = filter == CatListFilter.MOST_SEEN
                )
            }
            .cachedIn(viewModelScope)

    /**
     * Benar saat daftar sedang dicari atau disaring. Dipakai layar untuk membedakan
     * "koleksinya memang kosong" dari "yang dicari tidak ketemu": dua keadaan itu
     * butuh kalimat dan jalan keluar yang berbeda.
     */
    val isFiltering: StateFlow<Boolean> =
        combine(searchText, selectedFilter) { text, filter ->
            text.isNotBlank() || filter != CatListFilter.ALL
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Jumlah seluruh catatan, termasuk yang belum termuat di daftar.
     *
     * null berarti angkanya belum kebaca, dan chip jumlahnya disembunyikan selama
     * itu: angka 0 yang muncul sebentar sebelum catatannya terbaca terbaca seperti
     * "koleksimu kosong".
     */
    val sightingCount: StateFlow<Int?> = catRepository.observeSightingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onSearchChange(value: String) {
        searchText.value = value
    }

    fun onFilterChange(value: CatListFilter) {
        selectedFilter.value = value
    }

    /** Melepas pencarian dan saringan sekaligus, dipakai tombol di keadaan kosong. */
    fun clearFilters() {
        searchText.value = ""
        selectedFilter.value = CatListFilter.ALL
    }

    /** Sama seperti di Home: kartu yang di-swipe hilang dari daftar tanpa dialog. */
    fun deleteSighting(sighting: CatSighting) {
        viewModelScope.launch { catRepository.deleteSighting(sighting) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { CatListViewModel(container.catRepository) }
        }
    }
}
