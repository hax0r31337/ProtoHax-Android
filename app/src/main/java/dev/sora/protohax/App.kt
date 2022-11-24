package dev.sora.protohax

import android.app.Application
import com.github.megatronking.netbare.NetBare


class App : Application() {

    override fun onCreate() {
        super.onCreate()

        NetBare.get().attachApplication(this, true)
    }
}