package com.example.radioapp

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("RadioAppSettings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_STATION_ID = "last_station_id"
        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_LAST_VOLUME = "last_volume"
    }

    // Son çalınan radyo ID'sini kaydet
    fun saveLastStation(stationId: Int) {
        prefs.edit().putInt(KEY_LAST_STATION_ID, stationId).apply()
    }

    // Son çalınan radyo ID'sini al
    fun getLastStationId(): Int {
        return prefs.getInt(KEY_LAST_STATION_ID, -1)
    }

    // Otomatik çalma ayarını kaydet
    fun setAutoPlay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
    }

    // Otomatik çalma ayarını al
    fun isAutoPlayEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_PLAY, false)
    }

    // Son ses seviyesini kaydet
    fun saveVolume(volume: Int) {
        prefs.edit().putInt(KEY_LAST_VOLUME, volume).apply()
    }

    // Son ses seviyesini al
    fun getLastVolume(): Int {
        return prefs.getInt(KEY_LAST_VOLUME, 100)
    }
}