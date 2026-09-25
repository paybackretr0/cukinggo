package com.khalied.cukinggo.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Potongan query yang dipakai berulang di file ini.
 *
 * Ditulis sekali karena bentuk barisnya harus sama di semua tempat: daftar,
 * peta, widget, dan kabar dekat semuanya membaca penemuan yang sama, dan yang
 * berbeda cuma penyaring serta urutannya.
 */
private const val SIGHTING_WITH_NAME =
    "SELECT s.id AS id, s.catId AS catId, c.name AS catName, s.photoPath AS photoPath, " +
        "s.description AS description, s.latitude AS latitude, s.longitude AS longitude, " +
        "s.timestamp AS timestamp FROM cat_sightings s INNER JOIN cats c ON c.id = s.catId"

/** Terbaru dulu, dan id dipakai sebagai pemutus seri supaya urutannya pasti. */
private const val NEWEST_FIRST = " ORDER BY s.timestamp DESC, s.id DESC"

/**
 * Satu penemuan cuking. Tabelnya hanya bertambah dan berkurang, tidak pernah
 * diubah isinya.
 *
 * Beberapa pembaca di sini menyebut nama cukingnya sekalian, karena kartu di
 * daftar, penanda peta, dan widget semuanya menampilkan nama itu di sebelah
 * fotonya.
 */
@Dao
interface CatSightingDao {

    @Insert
    suspend fun insertSighting(sighting: CatSightingEntity): Long

    /** Seluruh penemuan, dipakai daftar di Home. */
    @Query(SIGHTING_WITH_NAME + NEWEST_FIRST)
    fun observeAllSightings(): Flow<List<CatSightingRow>>

    /** Seluruh penemuan sekali baca, dipakai widget dan area pantauan. */
    @Query(SIGHTING_WITH_NAME + NEWEST_FIRST)
    suspend fun getAllSightings(): List<CatSightingRow>

    /** Penemuan paling baru dari seluruh koleksi, dipakai widget "Cuking terakhir". */
    @Query(SIGHTING_WITH_NAME + NEWEST_FIRST + " LIMIT 1")
    suspend fun getLatestSighting(): CatSightingRow?

    /** Penemuan paling baru dari satu cuking, dipakai kabar "dekat cuking". */
    @Query(SIGHTING_WITH_NAME + " WHERE s.catId = :catId" + NEWEST_FIRST + " LIMIT 1")
    suspend fun getLatestSightingForCat(catId: Long): CatSightingRow?

    /**
     * Seluruh penemuan untuk halaman "Semua cuking", sehalaman per permintaan.
     *
     * Room yang mengurus batas barisnya, dan PagingSource ini ditandai basi
     * sendiri saat isi tabelnya berubah, jadi penemuan yang baru ditambah atau
     * dihapus tidak perlu diberitahukan dari luar.
     */
    @Query(SIGHTING_WITH_NAME + NEWEST_FIRST)
    fun pagingSourceAllSightings(): PagingSource<Int, CatSightingRow>

    /** Jumlah seluruh penemuan, dipakai chip jumlah di Home dan halaman daftar. */
    @Query("SELECT COUNT(*) FROM cat_sightings")
    fun observeSightingCount(): Flow<Int>

    /**
     * Hanya waktu penemuan, dipakai menghitung rentetan harian. Kolomnya dipilih
     * satu saja karena yang dibutuhkan cuma tanggalnya.
     */
    @Query("SELECT timestamp FROM cat_sightings")
    suspend fun getAllTimestamps(): List<Long>

    /**
     * Cuking mana yang punya penemuan ini. Null kalau penemuannya sudah dihapus,
     * dan itu berarti layar detailnya memang sudah tidak ada isinya.
     */
    @Query("SELECT catId FROM cat_sightings WHERE id = :id")
    fun observeSightingCatId(id: Long): Flow<Long?>

    /** Dipakai kabar "dekat cuking", yang cuma tahu id dari request ID geofence. */
    @Query(SIGHTING_WITH_NAME + " WHERE s.catId IN (:catIds)" + NEWEST_FIRST)
    suspend fun getSightingsByCatIds(catIds: List<Long>): List<CatSightingRow>

    /**
     * Berapa penemuan yang dimiliki satu cuking. Dipakai untuk tahu apakah
     * profilnya masih pantas disimpan setelah satu penemuannya dihapus.
     */
    @Query("SELECT COUNT(*) FROM cat_sightings WHERE catId = :catId")
    suspend fun countSightingsForCat(catId: Long): Int

    @Query("DELETE FROM cat_sightings WHERE id = :id")
    suspend fun deleteSightingById(id: Long)

    @Query("DELETE FROM cat_sightings WHERE catId = :catId")
    suspend fun deleteSightingsForCat(catId: Long)
}
