package dev.sora.protohax.ui.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.EventModuleToggle
import dev.sora.relay.game.event.EventHook

class Shortcut(val module: CheatModule, private val overlayManager: OverlayManager) {

	val params = WindowManager.LayoutParams(
		WindowManager.LayoutParams.WRAP_CONTENT,
		WindowManager.LayoutParams.WRAP_CONTENT,
		WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
		PixelFormat.TRANSLUCENT
	)
	private var overlayView: TextView? = null
	private val listener = EventHook(EventModuleToggle::class.java, handler = {
		if (module == this@Shortcut.module) {
			overlayView?.updateTextColor(targetState)
		}
	})

	init {
		params.gravity = Gravity.TOP or Gravity.START
		params.x = 100
		params.y = 100
	}

	fun display(wm: WindowManager) {
		if (overlayView != null) remove(wm)

		val ctx = overlayManager.ctx

		val text = TextView(ctx).apply {
			gravity = Gravity.CENTER or Gravity.CENTER
			text = module.name.filter { it.isUpperCase() }
			textSize = 14f
			updateTextColor()

			setPadding(30, 30, 30, 30)

			background = GradientDrawable().apply {
				setColor(getBackgroundColor(ctx))
				cornerRadius = 15f
				alpha = 150
			}
		}
		text.setOnClickListener {
			module.toggle()
			text.updateTextColor()
		}

		with(overlayManager) {
			text.draggable(params, wm)
		}

		wm.addView(text, params)
		MinecraftRelay.session.eventManager.register(listener)

		overlayView = text
	}

	fun remove(wm: WindowManager) {
		wm.removeView(overlayView ?: return)
		MinecraftRelay.session.eventManager.removeHandler(listener)
		overlayView = null
	}

	private fun TextView.updateTextColor(targetState: Boolean = module.state) {
		val ctx = overlayManager.ctx
		setTextColor(if (module.canToggle) if (targetState) getToggleOnColor(ctx) else getToggleOffColor(ctx) else getTextColor(ctx))
	}

	/**
	 * color used if dynamic color is not available
	 */
	companion object {
		private val TEXT_COLOR = Color.parseColor("#c1c1c1")
		private val BACKGROUND_COLOR = Color.parseColor("#1b1b1b")
		private val TOGGLE_ON_COLOR = Color.parseColor("#00a93f")
		private val TOGGLE_OFF_COLOR = Color.parseColor("#c81000")

		private fun Context.isNightMode(): Boolean {
			return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
		}

		private fun getTextColor(ctx: Context): Int {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if (ctx.isNightMode()) ctx.getColor(android.R.color.system_neutral1_100) else ctx.getColor(android.R.color.system_neutral1_900)
			} else TEXT_COLOR
		}

		private fun getBackgroundColor(ctx: Context): Int {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if (ctx.isNightMode()) ctx.getColor(android.R.color.system_neutral1_900) else ctx.getColor(android.R.color.system_neutral1_10)
			} else BACKGROUND_COLOR
		}

		private fun getToggleOnColor(ctx: Context): Int {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if (ctx.isNightMode()) ctx.getColor(android.R.color.system_accent1_200) else ctx.getColor(android.R.color.system_accent1_600)
			} else TOGGLE_ON_COLOR
		}

		private fun getToggleOffColor(ctx: Context): Int {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if (ctx.isNightMode()) ctx.getColor(android.R.color.system_accent1_600) else ctx.getColor(android.R.color.system_accent1_200)
			} else TOGGLE_OFF_COLOR
		}
	}
}
