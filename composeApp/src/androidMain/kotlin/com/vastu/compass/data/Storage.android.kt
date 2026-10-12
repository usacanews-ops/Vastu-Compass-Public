package com.vastu.compass.data

import java.io.File

actual class LocalStorage actual constructor() {
    private val cacheFile = File(System.getProperty("java.io.tmpdir"), "vastu_offline_cache.json")

    actual fun saveData(key: String, data: String) {
        try {
            cacheFile.writeText(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun loadData(key: String): String? {
        return try {
            if (cacheFile.exists()) cacheFile.readText() else null
        } catch (e: Exception) {
            null
        }
    }
}
