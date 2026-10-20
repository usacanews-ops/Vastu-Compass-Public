package com.vastu.compass.data

import com.vastu.compass.model.VastuDataPackage
import com.vastu.compass.model.VastuItem
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.random.Random

object VastuRepository {
    private const val LIVE_DATA_URL = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/assets/vastu_data.json"

    private val client = HttpClient()
    private val storage = LocalStorage()

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _dataFlow = MutableStateFlow<VastuDataPackage?>(null)
    val dataFlow: StateFlow<VastuDataPackage?> = _dataFlow.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun initialize() {
        try {
            val cachedJson = storage.loadData("vastu_db")
            if (!cachedJson.isNullOrBlank()) {
                val cachedData = jsonFormat.decodeFromString<VastuDataPackage>(cachedJson)
                _dataFlow.value = cachedData
                CoroutineScope(Dispatchers.Default).launch {
                    preloadIcons(cachedData.items)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        CoroutineScope(Dispatchers.Default).launch {
            syncWithLiveServer()
        }
    }

    suspend fun syncWithLiveServer(): String {
        _isSyncing.value = true
        return try {
            val cacheBuster = Random.nextInt(10000, 99999)
            val url = "$LIVE_DATA_URL?v=$cacheBuster"
            val response = client.get(url).bodyAsText()

            val remoteData = jsonFormat.decodeFromString<VastuDataPackage>(response)
            val currentData = _dataFlow.value

            if (currentData == null || remoteData.version >= currentData.version) {
                _dataFlow.value = remoteData
                storage.saveData("vastu_db", response)
                CoroutineScope(Dispatchers.Default).launch {
                    preloadIcons(remoteData.items)
                }
                _isSyncing.value = false
                "Success! Updated to version ${remoteData.version}"
            } else {
                _isSyncing.value = false
                "Already up to date (Version ${currentData.version})"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isSyncing.value = false
            "Sync Failed: ${e.message ?: "Network error"}"
        }
    }

    private suspend fun preloadIcons(items: List<VastuItem>) {
        withContext(Dispatchers.Default) {
            items.forEach { item ->
                if (item.iconUrl.isNotBlank()) {
                    try {
                        client.get(item.iconUrl)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
