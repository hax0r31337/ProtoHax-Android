package dev.sora.protohax.relay.gui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Html
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.google.android.material.slider.Slider
import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.util.Gpw
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.cheat.value.FloatValue
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.cheat.value.ListValue
import java.math.BigDecimal
import java.math.RoundingMode


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
            val btn = this
            normalOnClickListener(trigger = true) {
                MinecraftRelay.moduleManager.modules.sortedBy { it.name }.forEach { m ->
                    fun Button.updateColor() {
                        this.setTextColor(if (m.canToggle) if (m.state) TOGGLE_ON_COLOR_RGB else TOGGLE_OFF_COLOR_RGB else TEXT_COLOR)
                    }
                    it.addView(ctx.themedButton().apply {
                        text = m.name
                        width = advisedWidth
                        updateColor()
                        setOnClickListener {
                            m.toggle()
                            updateColor()
                        }
                        setOnLongClickListener {
                            moduleValues(ctx, advisedWidth, m, btn)
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
                    text = ctx.getString(R.string.clickgui_configs_create)
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
                        val hasConfig = MinecraftRelay.configManager.listConfig().contains(currentConfig)
                        MinecraftRelay.configManager.saveConfig(currentConfig)
                        // only refresh if not displayed this config on last refresh
                        if (!hasConfig) {
                            btn.performClick()
                        }
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

    private fun moduleValues(ctx: Context, advisedWidth: Int, module: CheatModule, backButton: Button): Boolean {
        val values = module.getValues()
        if (values.isEmpty()) return false
        buttonList.removeAllViews()
        values.forEach { value ->
            if (value is BoolValue) {
                buttonList.addView(ctx.themedButton().also { b ->
                    fun Button.setText() {
                        text = Html.fromHtml("${value.name}: <font color=\"${if(value.get()) "$TOGGLE_ON_COLOR\">ON" else "$TOGGLE_OFF_COLOR>OFF"}</font>", Html.FROM_HTML_MODE_LEGACY)
                    }
                    b.setText()
                    b.width = advisedWidth
                    b.setOnClickListener {
                        value.set(!value.get())
                        b.setText()
                    }
                })
            } else  {
                fun Button.setText() {
                    text = Html.fromHtml("${value.name}: <font color=\"#AAAAAA\">${value.get()
                        .let { if (it is Float) BigDecimal(it.toString()).setScale(2, RoundingMode.HALF_UP) else it }
                        .toString().replace("<", "&lt;").replace(">", "&gt;")}</font>", Html.FROM_HTML_MODE_LEGACY)
                }
                val button = ctx.themedButton().apply {
                    setText()
                    width = advisedWidth
                }
                buttonList.addView(button)
                if (value is ListValue) {
                    button.setOnClickListener {
                        val valueList = value.values
                        val idx = valueList.indexOf(value.get()) + 1
                        value.set(if (idx == valueList.size) {
                            valueList.first()
                        } else valueList[idx])
                        button.setText()
                    }
                } else if (value is IntValue) {
                    button.background = ColorDrawable(BACKGROUND_COLOR)
                    buttonList.addView(SeekBar(ctx).apply {
                        min = value.minimum
                        max = value.maximum
                        progress = value.get().coerceIn(value.minimum, value.maximum)
                        onProgressChanged { _, progress, _ ->
                            value.set(progress)
                            button.setText()
                        }
                        button.height -= height
                        background = ColorDrawable(BACKGROUND_COLOR)
                    })
                } else if (value is FloatValue) {
                    button.background = ColorDrawable(BACKGROUND_COLOR)
                    buttonList.addView(SeekBar(ctx).apply {
                        min = 0
                        max = advisedWidth
                        progress = (((value.get() - value.minimum) / (value.maximum - value.minimum) ) * advisedWidth).toInt().coerceIn(0, advisedWidth)
                        onProgressChanged { _, progress, _ ->
                            value.set(value.minimum + (value.maximum - value.minimum) * (progress.toFloat() / advisedWidth))
                            button.setText()
                        }
                        button.height -= height
                        background = ColorDrawable(BACKGROUND_COLOR)
                    })
                }
            }
        }
        buttonList.addView(ctx.themedButton().apply {
            text = "< " + ctx.getString(R.string.clickgui_modules_back)
            setTextColor(RIPPLE_COLOR)
            setOnClickListener {
                backButton.performClick()
            }
        })
        return true
    }

    private fun SeekBar.onProgressChanged(callback: (seekbar: SeekBar, progress: Int, fromUser: Boolean) -> Unit) {
        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                callback(seekBar, progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
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
        private const val TOGGLE_ON_COLOR = "#00a93f"
        private const val TOGGLE_OFF_COLOR = "#c81000"
        private val TOGGLE_ON_COLOR_RGB = Color.parseColor(TOGGLE_ON_COLOR)
        private val TOGGLE_OFF_COLOR_RGB = Color.parseColor(TOGGLE_OFF_COLOR)
    }
}