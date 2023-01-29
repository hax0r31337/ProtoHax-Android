package dev.sora.protohax.relay.service

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.relay.gui.PopupWindow
import dev.sora.protohax.relay.gui.RenderLayerView
import dev.sora.protohax.ui.activities.MainActivity
import dev.sora.protohax.util.ContextUtils.readString
import libmitm.Libmitm
import libmitm.Protector
import libmitm.TUN
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface


class AppService : VpnService(), Protector {

    private lateinit var windowManager: WindowManager
    private var layoutView: View? = null
    private var renderLayerView: View? = null
    private val popupWindow = PopupWindow()

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY
        instance = this

        val action = intent.action
        try {
            if (ACTION_START == action) {
                startVPN()
                startForeground(1, createNotification())
            } else if (ACTION_STOP == action) {
                stopVPN()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                stopSelf()
            }
        } catch (t: Throwable) {
            Log.e("ProtoHax", "command", t)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startVPN() {
        val (hasIPv4, hasIPv6) = checkNetState()

        val builder = Builder()
        builder.setBlocking(true)
        builder.setMtu(VPN_MTU)
        builder.setSession("ProtoHax")
        builder.addAllowedApplication(readString(MainActivity.KEY_TARGET_PACKAGE_CACHE)!!)
        builder.addAllowedApplication(this.packageName)
//        builder.addDnsServer("8.8.8.8")
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
            fdProtector = this@AppService
            if (hasIPv4) {
                addLocalIP(PRIVATE_VLAN4_CLIENT)
            }
            if (hasIPv6) {
                addLocalIP(PRIVATE_VLAN6_CLIENT)
            }
            udpRedirector = UdpForwarderHandler
            udpEstablishHandler = UdpForwarderHandler
        }
        this.tun = tun
        tun.start()
        Log.i("ProtoHax", "netstack started")
        isActive = true
        try {
            onServiceStart()
            serviceListeners.forEach { it.onServiceStarted() }
        } catch (t: Throwable) {
            Log.e("ProtoHax", "start callback", t)
        }
    }

    private fun stopVPN() {
        isActive = false
        try {
            onServiceStop()
            serviceListeners.forEach { it.onServiceStopped() }
        } catch (t: Throwable) {
            Log.e("ProtoHax", "stop callback", t)
        }
        tun?.close()
        vpnDescriptor?.close()
    }

    private fun checkNetState(): Pair<Boolean, Boolean> {
        var hasIPv4 = false
        var hasIPv6 = false

        val networkInterfaces = NetworkInterface.getNetworkInterfaces()
        while (networkInterfaces.hasMoreElements()) {
            val ni = networkInterfaces.nextElement()
            for (addr in ni.interfaceAddresses) {
                if (addr.address is Inet6Address) {
                    hasIPv6 = true
                } else if (addr.address is Inet4Address) {
                    hasIPv4 = true
                }
            }
        }
        return hasIPv4 to hasIPv6
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
                R.string.proxy_notification, getString(R.string.app_name), readString(
                MainActivity.KEY_TARGET_PACKAGE_CACHE) ?: "unknown"))
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        return builder.build()
    }

    private fun onServiceStart() {
        MinecraftRelay.listen()
        popupWindow()
    }

    private fun onServiceStop() {
        layoutView?.let { windowManager.removeView(it) }
        layoutView = null
        renderLayerView?.let { windowManager.removeView(it) }
        renderLayerView = null
        popupWindow.destroy(windowManager)
        MinecraftRelay.close()
    }

    private fun popupWindow() {
        var params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0   // Initial Position of window
        params.y = 100 // Initial Position of window

        val layout = LinearLayout(this)

        val imageView = ImageView(this)
        imageView.setImageResource(R.mipmap.ic_launcher_round)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        var dragPosX = 0f
        var dragPosY = 0f
        var pressDownTime = System.currentTimeMillis()
        imageView.setOnClickListener {
            popupWindow.toggle(windowManager, this)
        }
        imageView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragPosX = event.rawX
                    dragPosY = event.rawY
                    pressDownTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (System.currentTimeMillis() - pressDownTime < 500) {
                        v.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (System.currentTimeMillis() - pressDownTime < 500) {
                        false
                    } else {
                        params.x += -((event.rawX - dragPosX)).toInt()
                        params.y += (event.rawY - dragPosY).toInt()
                        dragPosX = event.rawX
                        dragPosY = event.rawY
                        windowManager.updateViewLayout(layout, params)
                        true
                    }
                }
                else -> false
            }
        }

        layout.addView(imageView)

        this.layoutView = layout
        windowManager.addView(layout, params)

        val params1 = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params1.gravity = Gravity.TOP or Gravity.END
        renderLayerView = RenderLayerView(this, MinecraftRelay.session)
        windowManager.addView(renderLayerView, params1)
    }

    companion object {
        const val ACTION_START = "dev.sora.libmitm.vpn.start"
        const val ACTION_STOP = "dev.sora.libmitm.vpn.stop"
        /**
         * this does nothing but initialize context
         */
        const val ACTION_INITIALIZE = "dev.sora.libmitm.vpn.initialize"
        const val CHANNEL_ID = "dev.sora.protohax.NOTIFICATION_CHANNEL_ID"

        const val VPN_MTU = 1500
        const val PRIVATE_VLAN4_CLIENT = "26.26.26.1"
        const val PRIVATE_VLAN6_CLIENT = "da26:2626::1"

        lateinit var instance: AppService
            private set
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