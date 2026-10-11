package com.vastu.compass.storage

import android.content.Context
import java.io.File

class AndroidLocalDataStorage(private val context: Context) : LocalDataStorage {
    private val file: File
        get() = File(context.filesDir, "vastu_data.json")

    override fun saveJson(content: String) {
        file.writeText(content)
    }

    override fun readJson(): String? {
        return if (file.exists()) file.readText() else null
    }

    override fun readDefaultAsset(): String {
        return try {
            context.assets.open("vastu_data.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "{}"
        }
    }
}

lateinit var storageContext: Context

actual fun getLocalDataStorage(): LocalDataStorage = AndroidLocalDataStorage(storageContext)
