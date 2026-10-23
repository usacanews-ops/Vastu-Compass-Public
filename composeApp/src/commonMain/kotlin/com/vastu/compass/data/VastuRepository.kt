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

object VastuRepository {
    private const val LIVE_DATA_URL = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/assets/vastu2.json"
    private const val LIVE_PRODUCTS_URL = "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/assets/products.json"

    private val client = HttpClient()
    private val storage = LocalStorage()

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val _dataFlow = MutableStateFlow<VastuDataPackage?>(null)
    val dataFlow: StateFlow<VastuDataPackage?> = _dataFlow.asStateFlow()

    private val _productsFlow = MutableStateFlow<List<VastuProduct>>(emptyList())
    val productsFlow: StateFlow<List<VastuProduct>> = _productsFlow.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun initialize() {
        // 1. Load local cached Vastu database immediately for instant offline use
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

        // 2. Load local cached products immediately
        try {
            val cachedProductsJson = storage.loadData("products_db")
            if (!cachedProductsJson.isNullOrBlank()) {
                val cachedProducts = jsonFormat.decodeFromString<ProductPackage>(cachedProductsJson)
                _productsFlow.value = cachedProducts.products
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Check version differences in background and sync only if newer version exists
        CoroutineScope(Dispatchers.Default).launch {
            syncWithLiveServer()
        }
    }

    suspend fun syncWithLiveServer(): String {
        _isSyncing.value = true
        return try {
            // A. Vastu2.json Version Check & Sync
            val response = client.get(LIVE_DATA_URL).bodyAsText()
            val remoteData = jsonFormat.decodeFromString<VastuDataPackage>(response)
            val currentData = _dataFlow.value

            // ONLY sync and save if remote version is strictly greater than local version, or if local is empty
            val syncStatusMessage = if (currentData == null || remoteData.version > currentData.version) {
                _dataFlow.value = remoteData
                storage.saveData("vastu_db", response)
                CoroutineScope(Dispatchers.Default).launch {
                    preloadIcons(remoteData.items)
                }
                "Success! Updated to version ${remoteData.version}"
            } else {
                "Already up to date (Version ${currentData.version})"
            }

            // B. Products.json Version Check & Sync
            try {
                val prodResponse = client.get(LIVE_PRODUCTS_URL).bodyAsText()
                val remoteProductsPkg = jsonFormat.decodeFromString<ProductPackage>(prodResponse)
                
                val cachedProductsJson = storage.loadData("products_db")
                val currentProductsPkg = if (!cachedProductsJson.isNullOrBlank()) {
                    runCatching { jsonFormat.decodeFromString<ProductPackage>(cachedProductsJson) }.getOrNull()
                } else null

                if (currentProductsPkg == null || remoteProductsPkg.version > currentProductsPkg.version) {
                    _productsFlow.value = remoteProductsPkg.products
                    storage.saveData("products_db", prodResponse)
                } else if (_productsFlow.value.isEmpty() && currentProductsPkg != null) {
                    _productsFlow.value = currentProductsPkg.products
                }
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
