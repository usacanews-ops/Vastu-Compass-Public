package com.vastu.compass.util

expect class ShareHelper() {
    fun shareApp(text: String, title: String = "Share Vastu Compass")
}
