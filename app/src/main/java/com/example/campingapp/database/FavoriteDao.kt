package com.example.campingapp.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :campingId)")
    fun isFavorite(campingId: Int): Flow<Boolean>

    // Sin la palabra suspend, Room genera el código perfecto sin bugs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    fun deleteFavorite(favorite: FavoriteEntity)
}