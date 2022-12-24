package dev.sora.protohax

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.widget.Toast


object ContextUtils {

    private const val SHARED_PREFERENCES_KEY = "ProtoHax_Caches"

    fun Context.writeString(key: String, property: String) {
        val editor = this.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
                .edit()
        editor.putString(key, property)
        editor.apply()
    }

    fun Context.readString(key: String): String? {
        return this.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(key, null)
    }

    fun Context.readStringOrDefault(key: String, default: String): String {
        return this.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            .getString(key, null) ?: default
    }

    fun Context.toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun Context.toast(resId: Int) {
        toast(getString(resId))
    }

    val PackageInfo.hasInternetPermission: Boolean
        get() {
            val permissions = requestedPermissions
            return permissions?.any { it == Manifest.permission.INTERNET } ?: false
        }

    fun PackageManager.isAppExists(packageName: String): Boolean {
        return try {
            getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}