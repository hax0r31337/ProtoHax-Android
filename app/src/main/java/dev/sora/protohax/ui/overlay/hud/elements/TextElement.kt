package dev.sora.protohax.ui.overlay.hud.elements

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import dev.sora.protohax.ui.overlay.hud.HudElement
import dev.sora.protohax.ui.overlay.hud.HudManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class TextElement : HudElement(HudManager.TEXT_ELEMENT_IDENTIFIER) {

	private val textElementDefault = "Hello world!"

	private var textValue by stringValue("Text", textElementDefault).listen {
		this.width = paint.measureText(it).roundToInt()
		it
	}
	private var colorRedValue by intValue("ColorRed", 255, 0..255)
	private var colorGreenValue by intValue("ColorGreen", 255, 0..255)
	private var colorBlueValue by intValue("ColorBlue", 255, 0..255)

	private val paint = Paint().also {
		it.color = Color.WHITE
	}

	override var height = paint.fontMetrics.let { it.descent - it.ascent }.roundToInt()
		private set
	override var width = paint.measureText(textElementDefault).roundToInt()
		private set

	override fun onRender(canvas: Canvas, needRefresh: AtomicBoolean) {
		paint.color = Color.rgb(colorRedValue, colorGreenValue, colorBlueValue)

		canvas.drawText(textValue, 0f, 0f, paint)
	}
}
