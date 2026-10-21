package com.vastu.compass.data

import com.vastu.compass.model.ProductPackage
import com.vastu.compass.model.VastuDataPackage
import com.vastu.compass.model.VastuItem
import com.vastu.compass.model.VastuProduct
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
    private const val LIVE_PRODUCTS_URL = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/assets/products.json"

    private val client = HttpClient()
    private val storage = LocalStorage()

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _dataFlow = MutableStateFlow<VastuDataPackage?>(null)
    val dataFlow: StateFlow<VastuDataPackage?> = _dataFlow.asStateFlow()

    private val _productsFlow = MutableStateFlow<List<VastuProduct>>(emptyList())
    val productsFlow: StateFlow<List<VastuProduct>> = _productsFlow.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun initialize() {
        // 1. Load cached Vastu DB
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

        // 2. Load cached Products
        try {
            val cachedProductsJson = storage.loadData("products_db")
            if (!cachedProductsJson.isNullOrBlank()) {
                val cachedProducts = jsonFormat.decodeFromString<ProductPackage>(cachedProductsJson)
                _productsFlow.value = cachedProducts.products
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Sync live data from server
        CoroutineScope(Dispatchers.Default).launch {
            syncWithLiveServer()
        }
    }

    suspend fun syncWithLiveServer(): String {
        _isSyncing.value = true
        return try {
            val cacheBuster = Random.nextInt(10000, 99999)

            // A. Sync Main Vastu Data
            val url = "$LIVE_DATA_URL?v=$cacheBuster"
            val response = client.get(url).bodyAsText()
            val remoteData = jsonFormat.decodeFromString<VastuDataPackage>(response)
            val currentData = _dataFlow.value

            var syncStatusMessage = ""

            if (currentData == null || remoteData.version >= currentData.version) {
                _dataFlow.value = remoteData
                storage.saveData("vastu_db", response)
                CoroutineScope(Dispatchers.Default).launch {
                    preloadIcons(remoteData.items)
                }
                syncStatusMessage = "Success! Updated to version ${remoteData.version}"
            } else {
                syncStatusMessage = "Already up to date (Version ${currentData.version})"
            }

            // B. Sync Products Data
            try {
                val prodUrl = "$LIVE_PRODUCTS_URL?v=$cacheBuster"
                val prodResponse = client.get(prodUrl).bodyAsText()
                val remoteProducts = jsonFormat.decodeFromString<ProductPackage>(prodResponse)
                _productsFlow.value = remoteProducts.products
                storage.saveData("products_db", prodResponse)
            } catch (pe: Exception) {
                pe.printStackTrace()
            }

            _isSyncing.value = false
            syncStatusMessage
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
