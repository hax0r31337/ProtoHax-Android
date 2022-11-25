package dev.sora.protohax

import android.content.Context


object CacheManager {

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
        return this.getSharedPreferences(SHARED_PREFERENCES_KEY, Context. MODE_PRIVATE)
            .getString(key, null) ?: default
    }
}