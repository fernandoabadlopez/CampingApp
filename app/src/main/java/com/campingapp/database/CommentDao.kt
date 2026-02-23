package com.campingapp.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.campingapp.model.Comment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE campingId = :campingId ORDER BY createdAt DESC")
    fun getCommentsForCamping(campingId: String): Flow<List<Comment>>

    @Insert
    suspend fun insertComment(comment: Comment): Long

    @Delete
    suspend fun deleteComment(comment: Comment)
}
