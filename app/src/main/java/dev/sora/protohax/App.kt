package dev.sora.protohax

import android.app.Application
import java.io.File


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this
        configDir = File(filesDir, "config").also {
            it.mkdirs()
        }
    }

    companion object {
        lateinit var app: App
        lateinit var configDir: File
    }
}