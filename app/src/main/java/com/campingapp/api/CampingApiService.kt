package com.campingapp.api

import com.campingapp.model.Camping
import retrofit2.http.GET
import retrofit2.http.Query

interface CampingApiService {

    /**
     * Fetches campings from the Generalitat Valenciana open data service.
     * Endpoint based on the GVA open data portal for tourism campings in Comunitat Valenciana.
     */
    @GET("records")
    suspend fun getCampings(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): CampingApiResponse
}

data class CampingApiResponse(
    val total_count: Int = 0,
    val results: List<Camping> = emptyList()
)
