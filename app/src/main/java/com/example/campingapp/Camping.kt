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
    val email: String
)