package com.khalied.cukinggo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CatEntity::class, CatSightingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CatDatabase : RoomDatabase() {

    abstract fun catDao(): CatDao

    abstract fun catSightingDao(): CatSightingDao

    companion object {
        private const val DB_NAME = "cukinggo.db"

        /**
         * Kolom nama cuking ditambahkan sebagai kolom terakhir yang boleh null.
         * Catatan lama tidak perlu diisi apa-apa, jadi datanya utuh setelah update.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cats ADD COLUMN name TEXT")
            }
        }

        /**
         * Satu tabel `cats` yang lama dipecah jadi dua: profil cuking, dan
         * penemuan yang bisa berulang.
         *
         * Tiap catatan lama jadi satu profil plus satu penemuan, dan id lamanya
         * dipakai ulang di kedua tabel. Itu yang membuat geofence dan widget yang
         * sudah terpasang tetap menunjuk catatan yang sama setelah update.
         *
         * Urutannya sengaja begini: tabel lama diganti nama dulu (waktu itu belum
         * ada tabel yang menunjuk ke sana, jadi aman di versi SQLite berapa pun),
         * tabel profil yang baru dibuat dengan nama aslinya, baru tabel penemuan
         * dibuat dan menunjuk ke tabel profil yang baru itu. Kalau tabel penemuan
         * dibuat sebelum penggantian nama, definisi foreign key-nya bisa ikut
         * menunjuk nama tabel sementara di SQLite lama.
         *
         * Isinya disalin dua kali dari tabel lama, dan yang menyalin penemuannya
         * adalah barisnya sendiri: satu catatan lama memang satu penemuan.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cats RENAME TO cats_old")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cats` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO cats (id, name, createdAt) " +
                        "SELECT id, name, timestamp FROM cats_old"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cat_sightings` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`catId` INTEGER NOT NULL, " +
                        "`photoPath` TEXT NOT NULL, " +
                        "`description` TEXT, " +
                        "`latitude` REAL NOT NULL, " +
                        "`longitude` REAL NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`catId`) REFERENCES `cats`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "INSERT INTO cat_sightings " +
                        "(id, catId, photoPath, description, latitude, longitude, timestamp) " +
                        "SELECT id, id, photoPath, description, latitude, longitude, timestamp " +
                        "FROM cats_old"
                )

                db.execSQL("DROP TABLE cats_old")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cat_sightings_catId` " +
                        "ON `cat_sightings` (`catId`)"
                )
            }
        }

        @Volatile
        private var instance: CatDatabase? = null

        fun getInstance(context: Context): CatDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
