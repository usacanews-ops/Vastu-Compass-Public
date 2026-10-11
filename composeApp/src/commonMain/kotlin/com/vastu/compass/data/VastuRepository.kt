package com.vastu.compass.data

import com.vastu.compass.model.VastuDataPackage
import com.vastu.compass.storage.getLocalDataStorage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

object VastuRepository {
    // IMPORTANT: Replace YOUR_GITHUB_USERNAME with your actual GitHub username
    private const val GITHUB_RAW_URL = "https://raw.githubusercontent.com/YOUR_GITHUB_USERNAME/vastu-compass/main/data/vastu_data.json"

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonParser)
        }
    }

    private val storage = getLocalDataStorage()
    private val _dataFlow = MutableStateFlow<VastuDataPackage?>(null)
    val dataFlow = _dataFlow.asStateFlow()

    fun initialize() {
        // 1. Read existing cached file or default bundled asset
        val localContent = storage.readJson() ?: storage.readDefaultAsset()
        try {
            val current = jsonParser.decodeFromString<VastuDataPackage>(localContent)
            _dataFlow.value = current
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncWithGitHub() {
        try {
            val remoteString: String = client.get(GITHUB_RAW_URL).body()
            val remotePkg = jsonParser.decodeFromString<VastuDataPackage>(remoteString)
            val currentPkg = _dataFlow.value

            // If remote has a higher version number, replace local file
            if (currentPkg == null || remotePkg.version > currentPkg.version) {
                storage.saveJson(remoteString)
                _dataFlow.value = remotePkg
            }
        } catch (e: Exception) {
            // Offline, network error, or timeout: silently use cached file
            e.printStackTrace()
        }
    }
}
