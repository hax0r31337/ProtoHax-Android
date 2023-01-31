package dev.sora.protohax.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File


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

    fun Context.shareTextAsFile(text: String, title: String? = null) {
        val file = File(File(cacheDir, "share").also {
            if (!it.exists()) it.mkdirs()
        }, "ProtoHax-${System.currentTimeMillis()}.log")
        file.writeText(text)
        file.deleteOnExit()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this@shareTextAsFile, packageName, file))
            type = "text/x-log"
        }
        startActivity(Intent.createChooser(shareIntent, title))
    }

    val PackageInfo.hasInternetPermission: Boolean
        get() {
            val permissions = requestedPermissions
            return permissions?.any { it == Manifest.permission.INTERNET } ?: false
        }

    fun PackageManager.getApplicationInfo(packageName: String): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            getApplicationInfo(packageName, 0)
        }
    }

    fun PackageManager.getPackageInfo(packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            getPackageInfo(packageName, 0)
        }
    }

    fun PackageManager.isAppExists(packageName: String): Boolean {
        return try {
            getApplicationInfo(packageName)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun PackageManager.getApplicationName(packageName: String): String {
        val info = getApplicationInfo(packageName)
        return getApplicationLabel(info).toString()
    }
}