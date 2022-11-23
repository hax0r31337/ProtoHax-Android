package dev.sora.libmitm

import android.app.Application
import android.content.Intent
import android.net.VpnService

object MITM {

    lateinit var app: Application
    var config: MITMConfig? = null
        private set

    var running: Boolean = false

    /**
     * Prepare to establish a VPN connection. This method returns {@code null} if the VPN
     * application is already prepared or if the user has previously consented to the VPN
     * application. Otherwise, it returns an {@link Intent} to a system activity. The application
     * should launch the activity using {@link Activity#startActivityForResult} to get itself
     * prepared.
     *
     * @return The intent to call using {@link Activity#startActivityForResult}.
     */
    fun prepare(): Intent? {
        return VpnService.prepare(app)
    }

    fun start(config: MITMConfig) {
        this.config = config

        val intent = Intent(dev.sora.libmitm.VpnService.SERVICE_START)
        intent.setPackage(app.packageName)
        app.startForegroundService(intent)
    }

    fun stop() {
        this.config = null

        val intent = Intent(dev.sora.libmitm.VpnService.SERVICE_STOP)
        intent.setPackage(app.packageName)
        app.startService(intent)
    }
}