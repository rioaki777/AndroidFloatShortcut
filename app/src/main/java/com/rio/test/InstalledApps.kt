package com.rio.test

import java.io.Serializable

class InstalledApps : Serializable{
    var appName: String? = null
    var appIcon: ByteArray? = null
    var packageName: String? = null
    var className: String? = null
}
