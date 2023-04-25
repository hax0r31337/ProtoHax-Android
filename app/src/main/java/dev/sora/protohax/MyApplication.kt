package dev.sora.protohax

import android.app.Application
import dev.sora.protohax.relay.netty.log.NettyLoggerFactory
import io.netty.util.internal.logging.InternalLoggerFactory

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
		InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())
        instance = this
    }

    companion object {
        lateinit var instance: MyApplication
            private set
    }
}
