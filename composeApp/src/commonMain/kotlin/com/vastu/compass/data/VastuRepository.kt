package com.vastu.compass.data

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
    private const val GITHUB_RAW_URL = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/assets/consult_config.json/vastu_data.json"

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
        // Generates a random number to bypass GitHub's 5-minute cache
        val cacheBuster = kotlin.random.Random.nextInt(10000, 99999)
        
        // Append it to the URL (e.g. .../vastu_data.json?v=45812)
        val url = "$GITHUB_RAW_URL?v=$cacheBuster"
        
        val response = client.get(url).bodyAsText()
        
        // Ensure lenient parsing just in case there are formatting quirks
        val format = kotlinx.serialization.json.Json { 
            ignoreUnknownKeys = true 
            isLenient = true 
        }
        
        val remoteData = format.decodeFromString<com.vastu.compass.model.VastuDataPackage>(response)
        val currentData = _dataFlow.value
        
        if (currentData == null || remoteData.version > currentData.version) {
            _dataFlow.value = remoteData
            
            // If you know your original save function name (like saveData(response)), put it here.
            // Otherwise, just leave this space blank! The UI will still update live in memory.
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
}
