package com.vastu.compass.util

import android.content.Context
import android.content.Intent

actual class ShareHelper(private val context: Context) {
    actual constructor() : this(
        // Fallback constructor via reflection/context holder
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentAppMethod = activityThreadClass.getMethod("currentApplication")
            currentAppMethod.invoke(null) as Context
        } catch (e: Exception) {
            throw IllegalStateException("Context could not be resolved automatically")
        }
    )

    actual fun shareApp(text: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(shareIntent)
    }
}
