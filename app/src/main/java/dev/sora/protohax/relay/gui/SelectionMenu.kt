package dev.sora.protohax.relay.gui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RoundRectShape
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.relay.cheat.module.CheatModule
import java.util.*


class SelectionMenu(private val window: PopupWindow) {

    /**
     * a list liked layout who contains the features of selected menus
     */
    private lateinit var buttonList: LinearLayout

    private fun Context.themedButton(backgroundColor: Int = BACKGROUND_COLOR, textColor: Int = TEXT_COLOR,
                                     ripples: Boolean = true): Button {
        return Button(this).apply {
            background = ColorDrawable(backgroundColor)
            if (ripples) {
                background = RippleDrawable(getStateListDrawable(textColor, RIPPLE_COLOR), background, getRippleMask(textColor))
            }
            setTextColor(textColor)
            isAllCaps = false
        }
    }

    private fun getStateListDrawable(normalColor: Int, pressedColor: Int): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(android.R.attr.state_activated),
                intArrayOf()
            ), intArrayOf(
                pressedColor,
                pressedColor,
                pressedColor,
                normalColor
            )
        )
    }

    private fun getRippleMask(color: Int): Drawable {
        val outerRadii = FloatArray(8)
        // 3 is radius of final ripple,
        // instead of 3 you can give required final radius
        Arrays.fill(outerRadii, 3f)
        val r = RoundRectShape(outerRadii, null, null)
        val shapeDrawable = ShapeDrawable(r)
        shapeDrawable.paint.color = color
        return shapeDrawable
    }

    fun apply(ctx: Context, layout: LinearLayout, wm: WindowManager) {
        val advisedWidth = (window.screenSize.x * 0.3).toInt()
        // title
        ctx.themedButton(backgroundColor = BACKGROUND_COLOR_PRIMARY, textColor = THEME_COLOR, ripples = false).apply {
            text = ctx.getString(R.string.app_name)
            width = advisedWidth
            setOnClickListener { window.destroy(wm) }
            layout.addView(this)
        }

        // tabs
        val menuTabs = LinearLayout(ctx).apply {
            layout.addView(this)
        }

        // buttons
        val buttonList = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            // make it scrollable
            val sv = ScrollView(ctx)
            sv.addView(this)
            layout.addView(sv)
        }
        this.buttonList = buttonList

        addTabsMenu(ctx, menuTabs, advisedWidth)
    }

    private fun addTabsMenu(ctx: Context, menuTabs: LinearLayout, advisedWidth: Int) {
        ctx.themedButton(backgroundColor = BACKGROUND_COLOR_PRIMARY).apply {
            text = ctx.getString(R.string.clickgui_modules)
            width = advisedWidth / 2
            normalOnClickListener(trigger = true) {
                MinecraftRelay.moduleManager.modules.sortedBy { it.name }.forEach { m ->
                    fun Button.updateColor() {
                        this.setTextColor(if (m.canToggle) if (m.state) Color.GREEN else Color.RED else TEXT_COLOR)
                    }
                    it.addView(ctx.themedButton().apply {
                        text = m.name
                        width = advisedWidth
                        updateColor()
                        setOnClickListener {
                            m.toggle()
                            updateColor()
                        }
                    })
                }
            }
            menuTabs.addView(this)
        }
        ctx.themedButton(backgroundColor = BACKGROUND_COLOR_PRIMARY).apply {
            text = ctx.getString(R.string.clickgui_configs)
            width = advisedWidth / 2
            normalOnClickListener {
                it.addView(ctx.themedButton().apply {
                    text = ctx.getString(R.string.clickgui_configs_save)
                    width = advisedWidth
                })
            }
            menuTabs.addView(this)
        }
    }

    private fun Button.normalOnClickListener(trigger: Boolean = false, callback: (LinearLayout) -> Unit) {
        setOnClickListener {
            buttonList.removeAllViews()
            callback(buttonList)
        }
        if (trigger) {
            callback(buttonList)
        }
    }

    companion object {
        private val TEXT_COLOR = Color.parseColor("#c1c1c1")
        private val BACKGROUND_COLOR_PRIMARY = Color.parseColor("#3c3c3c")
        private val BACKGROUND_COLOR = Color.parseColor("#1b1b1b")
        private val RIPPLE_COLOR = TEXT_COLOR
        private val THEME_COLOR = Color.parseColor("#3d9adc")
    }
}