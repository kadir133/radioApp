package com.example.radioapp

data class RadioConfig(
    val version: Int,
    val last_updated: String,
    val categories: List<Category>
)