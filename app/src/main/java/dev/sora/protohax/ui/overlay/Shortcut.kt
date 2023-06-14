package dev.sora.protohax.ui.overlay

import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.relay.gui.SelectionMenu
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.EventModuleToggle
import dev.sora.relay.game.event.EventHook

class Shortcut(val module: CheatModule, val overlayManager: OverlayManager) {

	val params = WindowManager.LayoutParams(
		WindowManager.LayoutParams.WRAP_CONTENT,
		WindowManager.LayoutParams.WRAP_CONTENT,
		WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
		PixelFormat.TRANSLUCENT
	)
	private var overlayView: TextView? = null
	private val listener = EventHook(EventModuleToggle::class.java, handler = {
		if (it.module == module) {
			overlayView?.updateTextColor(it.targetState)
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
				setColor(SelectionMenu.BACKGROUND_COLOR)
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
		setTextColor(if (module.canToggle) if (targetState) SelectionMenu.TOGGLE_ON_COLOR_RGB else SelectionMenu.TOGGLE_OFF_COLOR_RGB else SelectionMenu.TEXT_COLOR)
	}
}
