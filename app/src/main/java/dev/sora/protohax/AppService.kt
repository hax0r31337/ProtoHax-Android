package dev.sora.protohax

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import com.github.megatronking.netbare.NetBare
import com.github.megatronking.netbare.NetBareService
import dev.sora.protohax.ContextUtils.toast
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.relay.PopupWindow
import dev.sora.protohax.ui.RainbowTextView


class AppService : NetBareService() {

    private lateinit var windowManager: WindowManager
    private var layoutView: View? = null
    private var layoutView1: View? = null
    private val popupWindow = PopupWindow()

    override fun onCreate() {
        val notificationManager = getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW))
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun notificationId() = 100

    override fun createNotification(): Notification {
        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val intent = Intent(this, MainActivity::class.java)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.action = Intent.ACTION_MAIN
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flag)

//        val stopIntent = Intent(ACTION_STOP)
//        stopIntent.setPackage(packageName)
//        val pendingIntent1 = PendingIntent.getActivity(this, 1, stopIntent, flag)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)

        val seq = AntiModification.call
        if (AntiModification.validateAppSignature(this).let { !it.first || !it.second.startsWith("fuck") } || seq == AntiModification.call || seq + 1 == AntiModification.call) {
            toast("Internal error occurred, please contact the developer.")
            throw NullPointerException("null")
        } else {
            builder
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.proxy_notification, getString(R.string.app_name), NetBare.get().config.allowedApplications.firstOrNull() ?: "unknown"))
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
//                .addAction(R.drawable.notification_icon, getString(R.string.stop_proxy_notify), pendingIntent1)
        }

        return builder.build()
    }

    override fun onServiceStart() {
        MinecraftRelay.listen()
        popupWindow()
    }

    override fun onServiceStop() {
        layoutView?.let { windowManager.removeView(it) }
        layoutView1?.let { windowManager.removeView(it) }
        popupWindow.destroy(windowManager)
        MinecraftRelay.close()
    }

    private fun popupWindow() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0   // Initial Position of window
        params.y = 100 // Initial Position of window

        val layout = LinearLayout(this)

//        val imageView = Button(this)
//        imageView.text = "BTN"
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

        displayWatermark()
    }

    private fun displayWatermark() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 0   // Initial Position of window
        params.y = 0 // Initial Position of window

        val layout1 = LinearLayout(this)
        layout1.addView(RainbowTextView(this).apply {
            text = "${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} by Liulihaocai, DO NOT LEAK"
        })
        this.layoutView1 = layout1
        windowManager.addView(layout1, params)
    }

    companion object {
        const val CHANNEL_ID = "dev.sora.protohax.NOTIFICATION_CHANNEL_ID"
    }
}