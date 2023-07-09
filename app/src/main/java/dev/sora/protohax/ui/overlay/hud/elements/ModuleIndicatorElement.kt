package dev.sora.protohax.ui.overlay.hud.elements

import android.graphics.Canvas
import android.graphics.Color
import android.text.TextPaint
import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.protohax.ui.overlay.hud.HudAlignment
import dev.sora.protohax.ui.overlay.hud.HudElement
import dev.sora.protohax.ui.overlay.hud.HudManager
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.EventModuleToggle
import dev.sora.relay.cheat.value.NamedChoice
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class ModuleIndicatorElement : HudElement(HudManager.MODULE_INDICATOR_ELEMENT_IDENTIFIER) {

	private var sortingModeValue by listValue("SortingMode", SortingMode.values(), SortingMode.LENGTH_DESCENDING)
	private var textRTLValue by boolValue("TextRTL", true)
	private var colorModeValue by listValue("ColorMode", ColorMode.values(), ColorMode.HUE)
	private var colorReversedSortValue by boolValue("ColorReversedSort", false)
	private var colorRedValue by intValue("ColorRed", 255, 0..255).visible { colorModeValue != ColorMode.HUE }
	private var colorGreenValue by intValue("ColorGreen", 255, 0..255).visible { colorModeValue != ColorMode.HUE }
	private var colorBlueValue by intValue("ColorBlue", 255, 0..255).visible { colorModeValue != ColorMode.HUE }
	private var textSizeValue by intValue("TextSize", 15, 10..50).listen {
		paint.textSize = it * MyApplication.density
		it
	}
	private var spacingValue by intValue("Spacing", 3, 0..20)

	private val paint = TextPaint().also {
		it.color = Color.WHITE
		it.isAntiAlias = true
		it.textSize = 20 * MyApplication.density
	}

	override var height = 10
		private set
	override var width = 10
		private set

	init {
	    alignmentValue = HudAlignment.RIGHT_TOP
	}

	override fun onRender(canvas: Canvas, needRefresh: AtomicBoolean) {
		val modules = sortingModeValue.getModules(paint)
		val lineHeight = paint.fontMetrics.let { it.descent - it.ascent }.roundToInt()
		if (modules.isEmpty()) {
			if (height != lineHeight) {
				needRefresh.set(true)
			}
			height = lineHeight
			val alertNoModules = "No modules has toggled on currently"
			width = paint.measureText(alertNoModules).roundToInt()

			paint.color = colorModeValue.getColor(0, 1, colorRedValue, colorGreenValue, colorBlueValue)
			canvas.drawText(alertNoModules, 0f, -paint.fontMetrics.ascent, paint)

			return
		}

		var y = 0
		val lineSpacing = (spacingValue * MyApplication.density).toInt()
		val maxWidth = modules.maxOf { paint.measureText(it.name).toInt() }
		modules.forEachIndexed { i, module ->
			paint.color = colorModeValue.getColor(if (colorReversedSortValue) modules.size - i else i, modules.size, colorRedValue, colorGreenValue, colorBlueValue)
			canvas.drawText(module.name, if (textRTLValue) maxWidth - paint.measureText(module.name) else 0f, -paint.fontMetrics.ascent + y, paint)
			y += lineHeight + lineSpacing
		}
		y -= lineHeight
		if (height != y) {
			needRefresh.set(true)
			height = y
		}
		if (width != maxWidth) {
			needRefresh.set(true)
			width = maxWidth
		}
	}

	private val onModuleToggle = handle<EventModuleToggle> {
		session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
	}

	enum class SortingMode(override val choiceName: String) : NamedChoice {
		NAME_ASCENDING("NameAscending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { it.name }
			}
		},
		NAME_DESCENDING("NameDescending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { it.name }
					.reversed()
			}
		},
		LENGTH_ASCENDING("LengthAscending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { paint.measureText(it.name) }
			}
		},
		LENGTH_DESCENDING("LengthDescending") {
			override fun getModules(paint: TextPaint): List<CheatModule> {
				return MinecraftRelay.moduleManager.modules
					.filter { it.state }
					.sortedBy { paint.measureText(it.name) }
					.reversed()
			}
		};

		abstract fun getModules(paint: TextPaint): List<CheatModule>
	}

	enum class ColorMode(override val choiceName: String) : NamedChoice {
		CUSTOM("Custom") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				return Color.rgb(r, g, b)
			}
		},
		HUE("Hue") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				return dev.sora.protohax.util.Color.HSBtoRGB(index.toFloat() / size, 1f, 1f)
			}
		},
		SATURATION_SHIFT_ASCENDING("SaturationShift") {
			override fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int {
				val hsv = floatArrayOf(0f, 0f, 0f)
				Color.colorToHSV(Color.rgb(r, g, b), hsv)
				hsv[2] = index.toFloat() / size
				return Color.HSVToColor(hsv)
			}
		};

		abstract fun getColor(index: Int, size: Int, r: Int, g: Int, b: Int): Int
	}
}
