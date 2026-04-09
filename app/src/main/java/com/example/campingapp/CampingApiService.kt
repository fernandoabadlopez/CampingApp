package com.example.campingapp

import retrofit2.http.GET
import retrofit2.http.Query

interface CampingApiService {
    // El ID es el que indica el PDF para el dataset de campings
    @GET("api/3/action/datastore_search")
    suspend fun getCampings(
        @Query("id") datasetId: String = "2ddaf823-5da4-4459-aa57-5bfe9f9eb474",
        @Query("limit") limit: Int = 300 // Pedimos todos de golpe
    ): CampingResponse
}