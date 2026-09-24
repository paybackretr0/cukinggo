package com.khalied.cukinggo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CatEntity::class], version = 2, exportSchema = false)
abstract class CatDatabase : RoomDatabase() {

    abstract fun catDao(): CatDao

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

        @Volatile
        private var instance: CatDatabase? = null

        fun getInstance(context: Context): CatDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
