package com.campingapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    /**
     * Base URL for the Generalitat Valenciana open data API for campings in Comunitat Valenciana.
     * Dataset: turisme_v_campings_a_f
     */
    private const val BASE_URL =
        "https://portaldadesobertes.gva.es/api/explore/v2.1/catalog/datasets/turisme_v_campings_a_f/"

    val apiService: CampingApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampingApiService::class.java)
    }
}
