package com.example.campingapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity le dice a Room que esto será una tabla en SQLite llamada "favorites"
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int // Guardaremos el ID del camping como clave primaria
)