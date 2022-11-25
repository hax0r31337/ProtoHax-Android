package dev.sora.protohax

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout

class AlertWindow {

    private var layout: View? = null

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

    fun display(wm: WindowManager, ctx: Context) {
        if (layout != null) return
        val layout = LinearLayout(ctx)

        Button(ctx).apply {
            text = ctx.getString(R.string.close_overlay_menu)
            setOnClickListener { destroy(wm) }
            layout.addView(this)
        }

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
        wm.addView(layout, params)
        this.layout = layout
    }

}