package com.vastu.compass.data

expect class LocalStorage() {
    fun saveData(key: String, data: String)
    fun loadData(key: String): String?
}
