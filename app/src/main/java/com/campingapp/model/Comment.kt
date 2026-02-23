package com.campingapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val campingId: String,

    val text: String,

    val createdAt: Long = System.currentTimeMillis()
)
