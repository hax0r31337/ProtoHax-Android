package dev.sora.libmitm

import android.app.Notification
import android.content.Intent
import android.util.Log

abstract class VpnService : android.net.VpnService() {

    private var thread: PacketHandler? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        try {
            if (action == SERVICE_START) {
                onService()
                notification().also {
                    startForeground(it.first, it.second)
                }
                Log.i("LibMITM", "service started")
            } else {
                onTerminate()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                Log.i("LibMITM", "service stopped")
            }
        } catch (t: Throwable) {
            Log.e("LibMITM", "launch service", t)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    protected open fun onService() {
        thread = PacketHandler(this, MITM.config!!).apply {
            start()
        }
    }

    protected open fun onTerminate() {
        if (thread == null) return

        thread!!.interrupt()
        thread = null
    }

    protected abstract fun notification(): Pair<Int, Notification>

    companion object {
        const val SERVICE_START = "dev.sora.libmitm.vpn.start"
        const val SERVICE_STOP = "dev.sora.libmitm.vpn.stop"
    }
}