package com.vastu.compass.storage

interface LocalDataStorage {
    fun saveJson(content: String)
    fun readJson(): String?
    fun readDefaultAsset(): String
}

expect fun getLocalDataStorage(): LocalDataStorage
