package dev.sora.protohax.relay.gui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Build
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import dev.sora.protohax.MyApplication
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.relay.service.ServiceListener
import dev.sora.relay.cheat.module.CheatModule

class PopupWindow(private val ctx: Context) : ServiceListener {

    private var layoutView: View? = null
    private var renderLayerView: View? = null

    private var menuLayout: View? = null
    private val menu = SelectionMenu(this)
    private val shortcuts = mutableMapOf<CheatModule, View>()

    val screenSize = Point()

    fun toggle(wm: WindowManager, ctx: Context) {
        if (menuLayout == null) {
            display(wm, ctx)
        } else {
            destroy(wm)
        }
    }

    fun destroy(wm: WindowManager) {
        if (menuLayout == null) return
        wm.removeView(menuLayout)
        menuLayout = null
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
        if (menuLayout != null) return

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
        this.menuLayout = layout
    }

    private fun View.draggable(params: WindowManager.LayoutParams, windowManager: WindowManager) {
        var dragPosX = 0f
        var dragPosY = 0f
        var pressDownTime = System.currentTimeMillis()
        setOnTouchListener { v, event ->
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
                        params.x += (event.rawX - dragPosX).toInt()
                        params.y += (event.rawY - dragPosY).toInt()
                        dragPosX = event.rawX
                        dragPosY = event.rawY
                        windowManager.updateViewLayout(this, params)
                        true
                    }
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onServiceStarted() {
        val windowManager = MyApplication.instance.getSystemService(VpnService.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

		val imageView = ImageView(ctx)

		ResourcesCompat.getDrawable(
			ctx.resources, R.mipmap.ic_launcher, ctx.theme
		)?.let { drawable ->
			val bitmap = Bitmap.createBitmap(
				(drawable.intrinsicWidth * 0.7).toInt(), (drawable.intrinsicHeight * 0.7).toInt(),
				Bitmap.Config.ARGB_8888
			)
			val canvas = Canvas(bitmap)
			drawable.setBounds(0, 0, canvas.width, canvas.height)
			drawable.draw(canvas)
			imageView.setImageBitmap(bitmap)
		} ?: imageView.setImageResource(R.drawable.notification_icon)
		imageView.setOnClickListener {
            toggle(windowManager, ctx)
        }

        imageView.draggable(params, windowManager)

        this.layoutView = imageView
        windowManager.addView(imageView, params)

        startRenderLayer(windowManager)

        val shortcutList = shortcuts.keys.map { it }
        shortcuts.clear()
        shortcutList.forEach {
            shortcutFor(it)
        }
    }

    private fun startRenderLayer(windowManager: WindowManager) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        // this will fix https://developer.android.com/about/versions/12/behavior-changes-all#untrusted-touch-events
        params.dimAmount = 0.8f
        params.alpha = 0.8f
        params.gravity = Gravity.TOP or Gravity.END
        val layout = RelativeLayout(ctx)
        layout.addView(RenderLayerView(ctx, MinecraftRelay.session),
            ViewGroup.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT))

        renderLayerView = layout
        windowManager.addView(layout, params)
    }

    override fun onServiceStopped() {
        val windowManager = MyApplication.instance.getSystemService(VpnService.WINDOW_SERVICE) as WindowManager
        layoutView?.let { windowManager.removeView(it) }
        layoutView = null
        renderLayerView?.let { windowManager.removeView(it) }
        renderLayerView = null
        destroy(windowManager)
        shortcuts.values.forEach {
            windowManager.removeView(it)
        }
    }

    fun shortcutFor(module: CheatModule): Boolean {
        val windowManager = MyApplication.instance.getSystemService(VpnService.WINDOW_SERVICE) as WindowManager
        if (shortcuts.containsKey(module)) {
            try {
                windowManager.removeView(shortcuts[module])
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            shortcuts.remove(module)
            return false
        }

        fun TextView.updateTextColor() {
            setTextColor(if (module.canToggle) if (module.state) SelectionMenu.TOGGLE_ON_COLOR_RGB else SelectionMenu.TOGGLE_OFF_COLOR_RGB else SelectionMenu.TEXT_COLOR)
        }

        val layout = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER or Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        val text = TextView(ctx).apply {
            gravity = Gravity.CENTER or Gravity.CENTER
            text = module.name.filter { it.isUpperCase() }
            textSize = 14f
            updateTextColor()

            setPadding(30, 30, 30, 30)

            background = GradientDrawable().apply {
                setColor(SelectionMenu.BACKGROUND_COLOR)
                cornerRadius = 15f
                alpha = 150
            }
        }
        layout.addView(text)
        layout.setOnClickListener {
            module.toggle()
            text.updateTextColor()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        layout.draggable(params, windowManager)

        windowManager.addView(layout, params)

        shortcuts[module] = layout

        return true
    }

    fun hasShortcut(module: CheatModule): Boolean {
        return shortcuts.containsKey(module)
    }
}
