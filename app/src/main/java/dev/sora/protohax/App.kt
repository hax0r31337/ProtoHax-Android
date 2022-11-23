package dev.sora.protohax

import android.app.Application
import dev.sora.libmitm.MITM

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        MITM.app = this
    }
}