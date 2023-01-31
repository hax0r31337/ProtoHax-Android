package dev.sora.protohax.relay.gui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.VpnService
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.relay.service.ServiceListener

class PopupWindow(private val ctx: Context) : ServiceListener {

    private var layoutView: View? = null
    private var renderLayerView: View? = null

    private var layout: View? = null
    private val menu = SelectionMenu(this)

    val screenSize = Point()

    fun toggle(wm: WindowManager, ctx: Context) {
        if (layout == null) {
            display(wm, ctx)
        } else {
            destroy(wm)
        }
    }

    fun destroy(wm: WindowManager) {
        if (layout == null) return
        wm.removeView(layout)
        layout = null
    }

    private fun getScreenSize(wm: WindowManager): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.maximumWindowMetrics.bounds.let {
                Point(it.width(), it.height())
            }
        } else {
            val point = Point()
            wm.defaultDisplay.getRealSize(point)
            point
        }
    }

    fun display(wm: WindowManager, ctx: Context) {
        if (layout != null) return

        getScreenSize(wm).also { screenSize.set(it.x, it.y) }

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        menu.apply(ctx, layout, wm)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0   // Initial Position of window
        params.y = 0 // Initial Position of window
        wm.addView(layout, params)
        this.layout = layout
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onServiceStarted() {
        val windowManager = AppService.instance.getSystemService(VpnService.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0   // Initial Position of window
        params.y = 100 // Initial Position of window

        val layout = LinearLayout(ctx)

        val imageView = ImageView(ctx)
        imageView.setImageResource(R.mipmap.ic_launcher_round)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        var dragPosX = 0f
        var dragPosY = 0f
        var pressDownTime = System.currentTimeMillis()
        imageView.setOnClickListener {
            toggle(windowManager, ctx)
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params1.gravity = Gravity.TOP or Gravity.END
        renderLayerView = RenderLayerView(ctx, MinecraftRelay.session)
        windowManager.addView(renderLayerView, params1)
    }

    override fun onServiceStopped() {
        val windowManager = AppService.instance.getSystemService(VpnService.WINDOW_SERVICE) as WindowManager
        layoutView?.let { windowManager.removeView(it) }
        layoutView = null
        renderLayerView?.let { windowManager.removeView(it) }
        renderLayerView = null
        destroy(windowManager)
    }
}