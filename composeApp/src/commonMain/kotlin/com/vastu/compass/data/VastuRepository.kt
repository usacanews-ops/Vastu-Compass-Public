package com.vastu.compass.data

import com.vastu.compass.model.VastuDataPackage
import com.vastu.compass.storage.getLocalDataStorage
io.ktor.client.*
io.ktor.client.call.*
io.ktor.client.plugins.contentnegotiation.*
io.ktor.client.request.*
io.ktor.serialization.kotlinx.json.*
kotlinx.coroutines.flow.MutableStateFlow
kotlinx.coroutines.flow.asStateFlow
kotlinx.serialization.json.Json

object VastuRepository {
    // Update this with your GitHub username and repository name
    private const val GITHUB_RAW_URL = "https://raw.githubusercontent.com/satinder-singh/vastu-compass/main/data/vastu_data.json"

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
