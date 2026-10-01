package com.khalied.cukinggo.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.khalied.cukinggo.data.local.CatDao
import com.khalied.cukinggo.data.local.CatEntity
import com.khalied.cukinggo.data.local.CatSightingDao
import com.khalied.cukinggo.data.local.CatSightingEntity
import com.khalied.cukinggo.data.local.CatSightingRow
import com.khalied.cukinggo.data.local.CatWithSightings
import com.khalied.cukinggo.data.local.toDomain
import com.khalied.cukinggo.data.local.toDomainOrNull
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.domain.model.CatProfile
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.domain.model.latestPerCat
import java.io.File
import com.khalied.cukinggo.util.ImageStorageHelper
import com.khalied.cukinggo.util.blankToNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Jumlah catatan per halaman di halaman "Semua cuking".
 *
 * Dua puluh: cukup banyak untuk mengisi layar tanpa membuat pemuatan pertamanya
 * terasa berat, dan sisanya menyusul sendiri saat daftarnya digulir.
 */
const val CAT_PAGE_SIZE = 20

/**
 * Satu-satunya pintu masuk ke data cuking: Room DB + file foto lokal.
 *
 * Dua istilah yang dipakai konsisten di sini: satu cuking punya satu profil
 * ([Cat]) dan banyak penemuan ([CatSighting]). Yang ditambah tiap kali pengguna
 * ketemu cuking lagi adalah penemuannya, dan yang ditambah tiap kali pengguna
 * menemukan cuking baru adalah profilnya sekaligus penemuan pertamanya.
 */
class CatRepository(
    private val catDao: CatDao,
    private val catSightingDao: CatSightingDao,
    private val imageStorageHelper: ImageStorageHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /**
     * Dijalankan setiap daftar catatan berubah. Repository sendiri tidak tahu
     * siapa yang mendengarkan, jadi urusan widget tetap di luar lapisan data.
     */
    private val onCatsChanged: () -> Unit = {}
) {

    /** Seluruh penemuan, urut terbaru dulu: isi daftar di Home dan penanda peta. */
    fun observeSightings(): Flow<List<CatSighting>> =
        catSightingDao.observeAllSightings()
            .map { rows -> rows.map(CatSightingRow::toDomain) }

    /**
     * Seluruh penemuan sebagai halaman-halaman untuk halaman daftar.
     *
     * Halaman pertamanya baru dimuat saat flow-nya dikumpulkan, jadi layar yang
     * tidak dibuka tidak membaca apa-apa.
     */
    fun pagingSightings(): Flow<PagingData<CatSighting>> =
        Pager(
            config = PagingConfig(
                pageSize = CAT_PAGE_SIZE,
                // Bawaan Paging memuat tiga halaman sekaligus di permintaan
                // pertama; di sini satu halaman sudah lebih dari satu layar penuh,
                // jadi sisanya dibiarkan menyusul saat digulir.
                initialLoadSize = CAT_PAGE_SIZE,
                // Tanpa placeholder: baris kosong sementara di daftar yang isinya
                // foto terbaca seperti catatan yang gagal dimuat.
                enablePlaceholders = false
            ),
            pagingSourceFactory = { catSightingDao.pagingSourceAllSightings() }
        ).flow.map { pagingData -> pagingData.map { row -> row.toDomain() } }

    /** Jumlah seluruh penemuan, dipakai chip jumlah. */
    fun observeSightingCount(): Flow<Int> = catSightingDao.observeSightingCount()

    /**
     * Profil semua cuking tanpa penemuannya, dipakai rekap mingguan untuk
     * menghitung berapa cuking yang baru ditandai.
     */
    fun observeCatProfiles(): Flow<List<CatProfile>> =
        catDao.observeAllCats().map { entities ->
            entities.map { entity ->
                CatProfile(id = entity.id, name = entity.name, createdAt = entity.createdAt)
            }
        }

    /** Penemuan terbaru, dipakai widget "Cuking terakhir". */
    suspend fun latestSighting(): CatSighting? = withContext(ioDispatcher) {
        catSightingDao.getLatestSighting()?.toDomain()
    }

    /** Seluruh penemuan sekali baca, dipakai widget "Cuking hari ini". */
    suspend fun getAllSightingsOnce(): List<CatSighting> = withContext(ioDispatcher) {
        catSightingDao.getAllSightings().map(CatSightingRow::toDomain)
    }

    /**
     * Satu penemuan terbaru per cuking: "cuking ini sekarang di mana".
     *
     * Dipakai widget "Cuking terdekat" dan pemasangan area pantauan kabar dekat,
     * keduanya karena yang ditanyakan memang cukingnya, bukan tiap penemuannya.
     */
    suspend fun latestSightingsPerCat(): List<CatSighting> = withContext(ioDispatcher) {
        catSightingDao.getAllSightings()
            .map(CatSightingRow::toDomain)
            .latestPerCat()
    }

    /** Waktu semua penemuan, dipakai menghitung rentetan harian di widget. */
    suspend fun getAllTimestamps(): List<Long> = withContext(ioDispatcher) {
        catSightingDao.getAllTimestamps()
    }

    /**
     * Penemuan terbaru dari cuking-cuking yang disebutkan, dipakai kabar "dekat
     * cuking" yang cuma tahu id cuking dari request ID geofence.
     */
    suspend fun getLatestSightingsForCats(catIds: List<Long>): List<CatSighting> =
        withContext(ioDispatcher) {
            catSightingDao.getSightingsByCatIds(catIds)
                .map(CatSightingRow::toDomain)
                .latestPerCat()
        }

    /** Satu cuking beserta seluruh penemuannya, dipakai layar detail. */
    fun observeCat(catId: Long): Flow<Cat?> =
        catDao.observeCatWithSightings(catId).map { it?.toDomainOrNull() }

    /**
     * Cuking yang punya penemuan ini, beserta riwayat penemuannya.
     *
     * Semua tempat yang bisa disentuh pengguna menunjuk ke satu penemuan, bukan
     * ke cukingnya: kartu di daftar, penanda di peta, widget, dan kabar dekat
     * semuanya mewakili satu momen ketemu. Jadi id penemuan yang dipakai untuk
     * membuka layar detail, dan dari situ cukingnya dicari.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCatOfSighting(sightingId: Long): Flow<Cat?> =
        catSightingDao.observeSightingCatId(sightingId)
            .flatMapLatest { catId ->
                if (catId == null) flowOf(null) else observeCat(catId)
            }

    suspend fun getCat(catId: Long): Cat? = withContext(ioDispatcher) {
        catDao.getCatWithSightings(catId)?.toDomainOrNull()
    }

    /**
     * Cuking baru: profilnya dibuat, lalu penemuan pertamanya langsung ikut
     * tersimpan dengan waktu yang sama.
     *
     * Yang dikembalikan adalah id cukingnya, karena itu identitas yang dipakai
     * layar lain.
     */
    suspend fun addCat(
        photoPath: String,
        name: String?,
        description: String?,
        latitude: Double,
        longitude: Double,
        videoPath: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(ioDispatcher) {
        val catId = catDao.insertCat(
            CatEntity(
                name = blankToNull(name),
                createdAt = timestamp
            )
        )
        catSightingDao.insertSighting(
            CatSightingEntity(
                catId = catId,
                photoPath = photoPath,
                videoPath = videoPath,
                description = blankToNull(description),
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
        )
        onCatsChanged()
        catId
    }

    /**
     * Penemuan baru untuk cuking yang sudah ada. Namanya tidak ikut ditulis,
     * karena satu cuking cuma punya satu nama dan itu tinggal di profilnya.
     *
     * Yang dikembalikan adalah id penemuannya.
     */
    suspend fun addSighting(
        catId: Long,
        photoPath: String,
        description: String?,
        latitude: Double,
        longitude: Double,
        videoPath: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(ioDispatcher) {
        val sightingId = catSightingDao.insertSighting(
            CatSightingEntity(
                catId = catId,
                photoPath = photoPath,
                videoPath = videoPath,
                description = blankToNull(description),
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
        )
        onCatsChanged()
        sightingId
    }

    /**
     * Mengganti nama panggilan satu cuking.
     *
     * Yang dikirim ke database sudah lewat [blankToNull], jadi nama yang isinya
     * cuma spasi tersimpan sebagai "belum diisi" dan UI cukup memeriksa null.
     */
    suspend fun updateCatName(catId: Long, name: String?) = withContext(ioDispatcher) {
        catDao.updateName(catId, blankToNull(name))
        onCatsChanged()
    }

    /**
     * Mengedit satu penemuan: catatan kegiatannya, fotonya, dan videonya. Video
     * punya tiga kemungkinan yang tidak dimiliki foto: dibiarkan, diganti, atau
     * dilepas. Foto tidak punya "dilepas" karena ia yang jadi wujud penemuan di
     * daftar dan widget.
     *
     * Urutan pemindahan filenya penting: file baru dipindah ke internal storage
     * dan dicatat ke database dulu, baru file lama dihapus. Kalau dibalik, ada
     * saat file lama sudah hilang sedangkan file baru belum tersimpan, dan itu
     * satu-satunya cara catatan ini bisa kehilangan medianya.
     */
    suspend fun updateSighting(
        sightingId: Long,
        description: String?,
        newPhotoCapture: File?,
        newVideoCapture: File? = null,
        removeVideo: Boolean = false
    ) = withContext(ioDispatcher) {
        catSightingDao.updateDescription(sightingId, blankToNull(description))
        if (newPhotoCapture != null && newPhotoCapture.exists()) {
            val previousPath = catSightingDao.getPhotoPath(sightingId)
            val newPath = imageStorageHelper.moveCaptureToInternalStorage(newPhotoCapture)
            catSightingDao.updatePhotoPath(sightingId, newPath)
            imageStorageHelper.deleteStoredFile(previousPath)
        }
        if (newVideoCapture != null && newVideoCapture.exists()) {
            val previousVideo = catSightingDao.getVideoPath(sightingId)
            val newPath = imageStorageHelper.moveVideoToInternalStorage(newVideoCapture)
            catSightingDao.updateVideoPath(sightingId, newPath)
            imageStorageHelper.deleteStoredFile(previousVideo)
        } else if (removeVideo) {
            val previousVideo = catSightingDao.getVideoPath(sightingId)
            catSightingDao.updateVideoPath(sightingId, null)
            imageStorageHelper.deleteStoredFile(previousVideo)
        }
        onCatsChanged()
    }

    /**
     * Menghapus satu penemuan.
     *
     * Kalau itu penemuan terakhir cukingnya, profilnya ikut terhapus: profil
     * tanpa penemuan tidak punya apa pun untuk ditampilkan.
     */
    suspend fun deleteSighting(sighting: CatSighting) = withContext(ioDispatcher) {
        catSightingDao.deleteSightingById(sighting.id)
        imageStorageHelper.deleteStoredFile(sighting.photoPath)
        imageStorageHelper.deleteStoredFile(sighting.videoPath)
        if (catSightingDao.countSightingsForCat(sighting.catId) == 0) {
            catDao.deleteCatById(sighting.catId)
        }
        onCatsChanged()
    }

    /** Menghapus satu cuking beserta seluruh penemuannya, fotonya, dan videonya. */
    suspend fun deleteCat(cat: Cat) = withContext(ioDispatcher) {
        cat.sightings.forEach { sighting ->
            imageStorageHelper.deleteStoredFile(sighting.photoPath)
            imageStorageHelper.deleteStoredFile(sighting.videoPath)
        }
        // Penemuannya dihapus lebih dulu, bukan mengandalkan CASCADE: urutannya
        // jadi benar walau penegakan foreign key di SQLite sedang tidak aktif.
        catSightingDao.deleteSightingsForCat(cat.id)
        catDao.deleteCatById(cat.id)
        onCatsChanged()
    }
}
