package dev.sora.protohax.activity

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import dev.sora.protohax.ContextUtils.toast
import dev.sora.protohax.R
import java.io.IOException
import kotlin.concurrent.thread

class LogcatActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logcat)
        logcat(false)
    }

    private fun logcat(shouldFlushLog: Boolean) {
        try {
            val waiting = findViewById<ProgressBar>(R.id.pb_waiting)
            waiting.visibility = View.VISIBLE

            thread {
                if (shouldFlushLog) {
                    val lst = LinkedHashSet<String>()
                    lst.add("logcat")
                    lst.add("-c")
                    val process = Runtime.getRuntime().exec(lst.toTypedArray())
                    process.waitFor()
                }
                val lst = LinkedHashSet<String>()
                lst.add("logcat")
                lst.add("-d")
                lst.add("-v")
                lst.add("time")
                lst.add("-s")
                lst.add("ProtoHax")
                val process = Runtime.getRuntime().exec(lst.toTypedArray())
                val allText = process.inputStream.bufferedReader().use { it.readText() }
                runOnUiThread {
                    val tvLogcat = findViewById<TextView>(R.id.tv_logcat)
                    val svLogcat = findViewById<ScrollView>(R.id.sv_logcat)
                    tvLogcat.text = allText
                    tvLogcat.movementMethod = ScrollingMovementMethod()
                    waiting.visibility = View.GONE
                    Handler(Looper.getMainLooper()).post { svLogcat.fullScroll(View.FOCUS_DOWN) }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    fun copyLogs(item: MenuItem) {
        val tvLogcat = findViewById<TextView>(R.id.tv_logcat)
        try {
            val cmb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(null, tvLogcat.text.toString())
            cmb.setPrimaryClip(clipData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        toast(R.string.logcat_copied)
    }
    
    fun clearLogs(item: MenuItem) {
        logcat(true)
    }
}