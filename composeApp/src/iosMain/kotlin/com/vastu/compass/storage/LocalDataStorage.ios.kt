package com.vastu.compass.storage

import platform.Foundation.*

class IosLocalDataStorage : LocalDataStorage {
    private val fileManager = NSFileManager.defaultManager
    private val filePath: String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            val docDir = paths.first() as String
            return "$docDir/vastu_data.json"
        }

    override fun saveJson(content: String) {
        val str = NSString.create(string = content)
        str.writeToFile(filePath, true, NSUTF8StringEncoding, null)
    }

    override fun readJson(): String? {
        return if (fileManager.fileExistsAtPath(filePath)) {
            NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null) as String?
        } else null
    }

    override fun readDefaultAsset(): String {
        val bundlePath = NSBundle.mainBundle.pathForResource("vastu_data", "json") ?: return "{}"
        return NSString.stringWithContentsOfFile(bundlePath, NSUTF8StringEncoding, null) as String? ?: "{}"
    }
}

actual fun getLocalDataStorage(): LocalDataStorage = IosLocalDataStorage()
