package com.vastu.compass.storage

import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

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
