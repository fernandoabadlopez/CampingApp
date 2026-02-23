package com.campingapp.repository

import android.content.Context
import com.campingapp.api.RetrofitClient
import com.campingapp.model.Camping
import com.campingapp.model.Comment
import com.campingapp.database.AppDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CampingRepository(private val context: Context) {

    private val commentDao = AppDatabase.getInstance(context).commentDao()

    // ── Local JSON ────────────────────────────────────────────────────────────

    /**
     * Loads the list of campings from the bundled JSON asset file.
     * The file is expected at `assets/campings.json` and contains an array of [Camping] objects.
     */
    suspend fun getCampingsFromJson(): List<Camping> = withContext(Dispatchers.IO) {
        val json = context.assets.open("campings.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Camping>>() {}.type
        Gson().fromJson(json, type) ?: emptyList()
    }

    // ── Web Service ───────────────────────────────────────────────────────────

    /**
     * Fetches campings from the Generalitat Valenciana open data web service.
     */
    suspend fun getCampingsFromApi(): List<Camping> = withContext(Dispatchers.IO) {
        RetrofitClient.apiService.getCampings().results
    }

    // ── Comments (Room) ───────────────────────────────────────────────────────

    fun getComments(campingId: String): Flow<List<Comment>> =
        commentDao.getCommentsForCamping(campingId)

    suspend fun addComment(campingId: String, text: String) {
        commentDao.insertComment(Comment(campingId = campingId, text = text))
    }

    suspend fun deleteComment(comment: Comment) {
        commentDao.deleteComment(comment)
    }
}
