package com.changecut.common.config

import android.content.Context

class AppConfig(private val context: Context) {

    val versionName: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

    val versionCode: Long
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        } catch (e: Exception) {
            1L
        }

    val packageName: String = context.packageName
}
