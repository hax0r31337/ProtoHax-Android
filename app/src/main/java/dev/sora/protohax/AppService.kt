package dev.sora.protohax

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.github.megatronking.netbare.NetBare
import com.github.megatronking.netbare.NetBareService
import dev.sora.protohax.ContextUtils.toast
import dev.sora.protohax.forwarder.R
import kotlin.random.Random


class AppService : NetBareService() {

    private lateinit var windowManager: WindowManager

    override fun onCreate() {
        val notificationManager = getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW))
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun notificationId() = Random.Default.nextInt()

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

    companion object {
        const val CHANNEL_ID = "dev.sora.protohax.NOTIFICATION_CHANNEL_ID"
    }
}