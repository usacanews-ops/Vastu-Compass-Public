package com.vastu.compass.data

import com.vastu.compass.model.VastuDataPackage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlin.random.Random

object VastuRepository {
    // Replace with your permanent raw Gist URL (ensure commit hash is removed from between /raw/ and filename)
    private const val GITHUB_RAW_URL = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/assets/vastu_data.json"

    private val client = HttpClient()
    private val storage = LocalStorage()

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _dataFlow = MutableStateFlow<VastuDataPackage?>(null)
    val dataFlow: StateFlow<VastuDataPackage?> = _dataFlow.asStateFlow()

    suspend fun initialize() {
        try {
            val cachedJson = storage.loadData("vastu_db")
            if (!cachedJson.isNullOrBlank()) {
                val cachedData = jsonFormat.decodeFromString<VastuDataPackage>(cachedJson)
                _dataFlow.value = cachedData
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncWithGitHub(): String {
        return try {
            val cacheBuster = Random.nextInt(10000, 99999)
            val url = "$GITHUB_RAW_URL?v=$cacheBuster"
            val response = client.get(url).bodyAsText()

            val remoteData = jsonFormat.decodeFromString<VastuDataPackage>(response)
            val currentData = _dataFlow.value

            if (currentData == null || remoteData.version > currentData.version) {
                _dataFlow.value = remoteData
                storage.saveData("vastu_db", response)
                "Success! Updated to version ${remoteData.version}"
            } else {
                "Already up to date (Version ${currentData.version})"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Sync Failed: ${e.message ?: "Unknown error"}"
        }
    }
}
