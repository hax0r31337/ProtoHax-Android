package dev.sora.protohax

import android.app.Application
import com.github.megatronking.netbare.NetBare
import java.io.File


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this
        configDir = File(filesDir, "config").also {
            it.mkdirs()
        }

        NetBare.get().attachApplication(this, true)
    }

    companion object {
        lateinit var app: App
        lateinit var configDir: File
    }
}