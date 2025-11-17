package com.example.radioapp

data class RadioStation(
    val id: Int,
    val name: String,
    val url: String,
    val description: String = "",
    val logo: String? = null
)