package dev.sora.protohax

import android.annotation.SuppressLint
import android.app.Application
import dev.sora.protohax.relay.netty.log.NettyLoggerFactory
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.ui.overlay.OverlayManager
import io.netty.util.internal.logging.InternalLoggerFactory

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

		InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())

		density = resources.displayMetrics.density

        instance = this
    }

    companion object {
        lateinit var instance: MyApplication
            private set

		var density: Float = 1f
			private set

		@SuppressLint("StaticFieldLeak")
		val overlayManager = OverlayManager().also {
			AppService.addListener(it)
		}
    }
}
