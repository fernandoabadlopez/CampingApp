package com.example.campingapp

data class Camping(
    val id: Int,
    val nombre: String,
    val municipio: String,
    val provincia: String,
    val categoria: String,
    val plazas: Int,
    val direccion: String,
    val web: String,
    val email: String,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var distanceToUser: Float? = null // Guardará la distancia en metros
)