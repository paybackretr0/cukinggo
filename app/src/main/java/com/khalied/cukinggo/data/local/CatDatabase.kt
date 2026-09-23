package com.khalied.cukinggo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CatEntity::class], version = 1, exportSchema = false)
abstract class CatDatabase : RoomDatabase() {

    abstract fun catDao(): CatDao

    companion object {
        private const val DB_NAME = "cukinggo.db"

        @Volatile
        private var instance: CatDatabase? = null

        fun getInstance(context: Context): CatDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
