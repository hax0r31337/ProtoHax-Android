package dev.sora.protohax.relay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.relay.gui.PopupWindow
import dev.sora.protohax.ui.activities.MainActivity
import dev.sora.protohax.ui.components.screen.settings.Settings
import dev.sora.protohax.util.ContextUtils.getApplicationName
import dev.sora.relay.utils.logError
import dev.sora.relay.utils.logInfo
import libmitm.Libmitm
import libmitm.TUN
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface


class AppService : VpnService() {

    private lateinit var windowManager: WindowManager
    private val popupWindow = PopupWindow(this).also {
        addListener(it)
    }

    private var vpnDescriptor: ParcelFileDescriptor? = null
    private var tun: TUN? = null

    override fun onCreate() {
        val notificationManager = getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, getString(
                    R.string.app_name
                ), NotificationManager.IMPORTANCE_LOW))
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onDestroy() {
		logInfo("VPN service destroyed")
		stopVPN()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        val action = intent.action
        try {
            if (ACTION_START == action) {
                startVPN()
                startForeground(1, createNotification())
            } else {
                stopVPN()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } catch (t: Throwable) {
            logError("command", t)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startVPN() {
        val (hasIPv4, hasIPv6) = when(Settings.ipv6Status.getValue(this)) {
			Settings.IPv6Choices.AUTOMATIC -> checkNetState()
			Settings.IPv6Choices.ENABLED -> true to true
			Settings.IPv6Choices.DISABLED -> true to false
			Settings.IPv6Choices.V6ONLY -> false to true
		}

        val builder = Builder()
        builder.setBlocking(true)
        builder.setMtu(VPN_MTU)
        builder.setSession("ProtoHax")
        builder.addAllowedApplication(MainActivity.targetPackage)
        builder.addDnsServer("8.8.8.8")
        // ipv4
        if (hasIPv4) {
            builder.addAddress(PRIVATE_VLAN4_CLIENT, 30)
            builder.addRoute("0.0.0.0", 0)
        }
        // ipv6
        if (hasIPv6) {
            builder.addAddress(PRIVATE_VLAN6_CLIENT, 126)
            builder.addRoute("::", 0)
        }

        val vpnDescriptor = builder.establish() ?: return
        this.vpnDescriptor = vpnDescriptor

        val tun = TUN().apply {
            fileDescriber = vpnDescriptor.fd
            mtu = VPN_MTU
            iPv6Config = when {
                hasIPv4 && hasIPv6 -> Libmitm.IPv6Enable
                hasIPv4 -> Libmitm.IPv6Disable
                hasIPv6 -> Libmitm.IPv6Only
                else -> error("invalid state")
            }
        }
        this.tun = tun
        tun.start()
        logInfo("netstack started")
        isActive = true
        try {
			MinecraftRelay.announceRelayUp()
            serviceListeners.forEach { it.onServiceStarted() }
        } catch (t: Throwable) {
            logError("start callback", t)
        }
    }

    private fun stopVPN() {
        isActive = false
		vpnDescriptor?.close()
		tun?.let {
			try {
				serviceListeners.forEach { l -> l.onServiceStopped() }
			} catch (t: Throwable) {
				logError("stop callback", t)
			}
			Thread(it::close).start()
		}
    }

    private fun checkNetState(): Pair<Boolean, Boolean> {
		val connectivityManager = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
		val activeNetwork = connectivityManager.getLinkProperties(connectivityManager.activeNetwork ?: return true to false) ?: return true to false
		val interfaceName = activeNetwork.interfaceName

		val networkInterfaces = NetworkInterface.getNetworkInterfaces()
		while (networkInterfaces.hasMoreElements()) {
			val ni = networkInterfaces.nextElement()
			if (ni.name != interfaceName) continue

			var hasIPv4 = false
			var hasIPv6 = false
			for (addr in ni.interfaceAddresses) {
				if (addr.address is Inet6Address) {
					hasIPv6 = true
				} else if (addr.address is Inet4Address) {
					hasIPv4 = true
				}
			}
			return hasIPv4 to hasIPv6
		}

		return true to false
    }

    private fun createNotification(): Notification {
        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val intent = Intent(this, MainActivity::class.java)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.action = Intent.ACTION_MAIN
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flag)

//        val stopIntent = Intent(ACTION_STOP)
//        stopIntent.setPackage(packageName)
//        val pendingIntent1 = PendingIntent.getActivity(this, 1, stopIntent, flag)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(
                R.string.proxy_notification, getString(R.string.app_name), packageManager.getApplicationName(MainActivity.targetPackage)))
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        return builder.build()
    }

    companion object {
        const val ACTION_START = "dev.sora.libmitm.vpn.start"
        const val ACTION_STOP = "dev.sora.libmitm.vpn.stop"
        const val CHANNEL_ID = "dev.sora.protohax.NOTIFICATION_CHANNEL_ID"

        const val VPN_MTU = 1500
        const val PRIVATE_VLAN4_CLIENT = "26.26.26.1"
        const val PRIVATE_VLAN6_CLIENT = "da26:2626::1"

        var isActive = false
        private val serviceListeners = mutableSetOf<ServiceListener>()

        fun addListener(listener: ServiceListener) {
            serviceListeners.add(listener)
        }

        fun removeListener(listener: ServiceListener) {
            serviceListeners.remove(listener)
        }
    }
}
