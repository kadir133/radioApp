package com.example.radioapp

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class RadioDataManager(private val context: Context) {

    private val gson = Gson()
    private val cacheFileName = "radios_cache.json"
    private val githubUrl = "https://raw.githubusercontent.com/kadir133/radioApp/refs/heads/main/radio_stations.json"

    companion object {
        private const val TAG = "RadioDataManager"
        private const val PREFS_NAME = "RadioDataPrefs"
        private const val KEY_LAST_VERSION = "last_version"
        private const val KEY_LAST_CHECK = "last_check"
    }

    // Lokal cache dosyası
    private fun getCacheFile(): File {
        Log.i(TAG,context.filesDir.canonicalPath)
        return File(context.filesDir, cacheFileName)
    }

    // Cache'den oku
    private fun readFromCache(): RadioConfig? {
        return try {
            val cacheFile = getCacheFile()
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                gson.fromJson(json, RadioConfig::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cache okuma hatası: ${e.message}")
            null
        }
    }

    // Cache'e yaz
    private fun writeToCache(config: RadioConfig) {
        try {
            val json = gson.toJson(config)
            getCacheFile().writeText(json)

            // Versiyonu kaydet
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_LAST_VERSION, config.version).apply()
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            Log.d(TAG, "Cache güncellendi, version: ${config.version}")
        } catch (e: Exception) {
            Log.e(TAG, "Cache yazma hatası: ${e.message}")
        }
    }

    // GitHub'dan indir
    private suspend fun downloadFromGithub(): RadioConfig? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "GitHub'dan indiriliyor...")
            val json = URL(githubUrl).readText()
            val config = gson.fromJson(json, RadioConfig::class.java)
            Log.d(TAG, "İndirme başarılı, version: ${config.version}")
            config
        } catch (e: Exception) {
            Log.e(TAG, "GitHub indirme hatası: ${e.message}")
            null
        }
    }

    // Ana fonksiyon: Radyo verilerini al
    suspend fun getRadioData(): RadioConfig? = withContext(Dispatchers.IO) {
        // Önce cache'den oku
        val cachedConfig = readFromCache()

        // GitHub'dan güncelleme kontrolü yap
        val githubConfig = downloadFromGithub()

        if (githubConfig != null) {
            // GitHub'dan veri geldi
            if (cachedConfig == null || githubConfig.version > cachedConfig.version) {
                // Yeni versiyon var, cache'i güncelle
                Log.d(TAG, "Yeni versiyon bulundu, güncelleniyor...")
                writeToCache(githubConfig)
                return@withContext githubConfig
            } else {
                // Cache güncel
                Log.d(TAG, "Cache güncel")
                return@withContext cachedConfig
            }
        } else {
            // GitHub'a ulaşılamadı, cache'i kullan
            Log.d(TAG, "GitHub'a ulaşılamadı, cache kullanılıyor")
            return@withContext cachedConfig
        }
    }

    // İlk kurulum için varsayılan veri
    fun getDefaultData(): RadioConfig {
        return RadioConfig(
            version = 0,
            last_updated = "",
            categories = emptyList()
        )
    }
}