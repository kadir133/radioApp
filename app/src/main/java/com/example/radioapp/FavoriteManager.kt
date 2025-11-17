package com.example.radioapp

import android.content.Context
import android.content.SharedPreferences

class FavoriteManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("RadioAppPrefs", Context.MODE_PRIVATE)

    private val FAVORITES_KEY = "favorites"

    // Favori olup olmadığını kontrol et
    fun isFavorite(stationId: Int): Boolean {
        val favorites = getFavorites()
        return favorites.contains(stationId)
    }

    // Favorilere ekle
    fun addFavorite(stationId: Int) {
        val favorites = getFavorites().toMutableSet()
        favorites.add(stationId)
        saveFavorites(favorites)
    }

    // Favorilerden çıkar
    fun removeFavorite(stationId: Int) {
        val favorites = getFavorites().toMutableSet()
        favorites.remove(stationId)
        saveFavorites(favorites)
    }

    // Favori durumunu toggle et
    fun toggleFavorite(stationId: Int): Boolean {
        return if (isFavorite(stationId)) {
            removeFavorite(stationId)
            false
        } else {
            addFavorite(stationId)
            true
        }
    }

    // Tüm favorileri al
    fun getFavorites(): Set<Int> {
        val favoritesString = prefs.getString(FAVORITES_KEY, "") ?: ""
        return if (favoritesString.isEmpty()) {
            emptySet()
        } else {
            favoritesString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        }
    }

    // Favorileri kaydet
    private fun saveFavorites(favorites: Set<Int>) {
        val favoritesString = favorites.joinToString(",")
        prefs.edit().putString(FAVORITES_KEY, favoritesString).apply()
    }
}