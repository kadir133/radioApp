package com.example.radioapp

data class Category(
    val id: Int,
    val name: String,
    val icon: String,
    val stations: List<RadioStation>
)