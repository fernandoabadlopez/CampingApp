package com.campingapp.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Camping(
    @SerializedName("cd_camping")
    val id: String = "",

    @SerializedName("denominacion")
    val name: String = "",

    @SerializedName("domicilio")
    val address: String = "",

    @SerializedName("municipio")
    val municipality: String = "",

    @SerializedName("provincia")
    val province: String = "",

    @SerializedName("cp")
    val postalCode: String = "",

    @SerializedName("telefono")
    val phone: String = "",

    @SerializedName("mail")
    val email: String = "",

    @SerializedName("url")
    val website: String = "",

    @SerializedName("categoria")
    val category: String = "",

    @SerializedName("plazas")
    val capacity: String = "",

    @SerializedName("latitud")
    val latitude: String = "",

    @SerializedName("longitud")
    val longitude: String = ""
) : Parcelable
