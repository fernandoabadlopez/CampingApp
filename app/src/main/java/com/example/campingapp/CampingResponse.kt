package com.example.campingapp

import com.google.gson.annotations.SerializedName

// Representa el objeto raíz del JSON de la GVA
data class CampingResponse(
    val result: CampingResult
)

// Representa el objeto "result" que contiene la lista
data class CampingResult(
    val records: List<CampingNetworkEntity>
)

// Representa cada camping tal cual viene de internet
data class CampingNetworkEntity(
    @SerializedName("_id") val id: Int,
    @SerializedName("Nombre") val nombre: String,
    @SerializedName("Municipio") val municipio: String,
    @SerializedName("Provincia") val provincia: String,
    @SerializedName("Categoria") val categoria: String?,
    @SerializedName("Plazas") val plazas: Int,
    @SerializedName("Direccion") val direccion: String,
    @SerializedName("Web") val web: String?,
    @SerializedName("Email") val email: String?
)