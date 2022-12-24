package dev.sora.protohax.relay.gui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.util.Gpw


class SelectionMenu(private val window: PopupWindow) {

    private var currentConfig: String = Gpw.generate(7)
    /**
     * a list liked layout who contains the features of selected menus
     */
    private lateinit var buttonList: LinearLayout

    private fun Context.themedButton(backgroundColor: Int = BACKGROUND_COLOR, textColor: Int = TEXT_COLOR,
                                     ripples: Boolean = true): Button {
        return Button(this).apply {
            background = ColorDrawable(backgroundColor)
            if (ripples) {
                background = RippleDrawable(getStateListDrawable(textColor, RIPPLE_COLOR), background,
                    ColorDrawable(RIPPLE_COLOR))
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
        ctx.themedButton(backgroundColor = BACKGROUND_COLOR_PRIMARY).also { btn ->
            btn.text = ctx.getString(R.string.clickgui_configs)
            btn.width = advisedWidth / 2
            btn.normalOnClickListener {
                it.addView(ctx.themedButton().apply {
                    text = "create"
                    width = advisedWidth
                    setOnClickListener {
                        currentConfig = Gpw.generate(kotlin.random.Random.nextInt(5) + 5)
                        MinecraftRelay.configManager.saveConfig(currentConfig)
                        btn.performClick() // refresh
                    }
                })
                it.addView(ctx.themedButton().apply {
                    text = ctx.getString(R.string.clickgui_configs_save)
                    width = advisedWidth
                    setOnClickListener {
                        MinecraftRelay.configManager.saveConfig(currentConfig)
                        btn.performClick() // refresh
                    }
                })
                MinecraftRelay.configManager.listConfig().forEach { config ->
                    it.addView(ctx.themedButton().apply {
                        text = config
                        width = advisedWidth
                        setOnClickListener {
                            currentConfig = config
                            MinecraftRelay.configManager.loadConfig(config)
                        }
                        setOnLongClickListener {
                            MinecraftRelay.configManager.deleteConfig(config)
                            btn.performClick()
                        }
                    })
                }
            }
            menuTabs.addView(btn)
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
        private val RIPPLE_COLOR = Color.parseColor("#888888")
        private val THEME_COLOR = Color.parseColor("#3d9adc")
    }
}