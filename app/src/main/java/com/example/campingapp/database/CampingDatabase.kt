package com.example.campingapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class CampingDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: CampingDatabase? = null

        // Este método nos da acceso a la base de datos desde cualquier lugar de la app
        fun getDatabase(context: Context): CampingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CampingDatabase::class.java,
                    "camping_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}