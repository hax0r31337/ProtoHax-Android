package dev.sora.protohax.ui.overlay.hud.elements

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.protohax.ui.overlay.hud.HudAlignment
import dev.sora.protohax.ui.overlay.hud.HudElement
import dev.sora.protohax.ui.overlay.hud.HudManager
import dev.sora.relay.cheat.module.impl.combat.ModuleTargets
import dev.sora.relay.game.entity.EntityOther
import dev.sora.relay.game.entity.EntityPlayer
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.math.roundToInt

class TargetIndicatorElement : HudElement(HudManager.TARGET_INDICATOR_ELEMENT_IDENTIFIER) {

	private var textSizeValue by intValue("TextSize", 20, 10..50).listen {
		paint.textSize = it * MyApplication.density
		it
	}

	private val paint = TextPaint().also {
		it.color = Color.WHITE
		it.isAntiAlias = true
		it.textSize = 20 * MyApplication.density
	}

	override var height = 10f
		private set
	override var width = 10f
		private set

	private var health = 0.5f

	private val COLOR_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]")

	init {
		alignmentValue = HudAlignment.CENTER
	}

	override fun onRender(canvas: Canvas, editMode: Boolean, needRefresh: AtomicBoolean) {
		val name: String
		val currentHealth: Float
		val maxHealth: Float
		if (editMode) {
			name = "ProtoHax"
			health = 0.7f
			currentHealth = 10f
			maxHealth = 20f
		} else {
			val targetsModule = MinecraftRelay.moduleManager.getModule(ModuleTargets::class.java)
			val target = targetsModule.previousAttack ?: return

			name = when (target) {
				is EntityPlayer -> stripColor(target.displayName)
				is EntityOther -> target.identifier.split(":").last()
				else -> return
			}

			val attribute = target.attributes["minecraft:health"]
			if (attribute != null) {
				maxHealth = attribute.maximum
				currentHealth = attribute.value
			} else {
				val data = target.metadata[EntityDataTypes.STRUCTURAL_INTEGRITY]
				maxHealth = 20f
				if (data != null) {
					currentHealth = (data as Int).toFloat()
				} else {
					currentHealth = 0f
				}
			}
			health += ((currentHealth / maxHealth) - health) * 0.05f
		}

		val lineHeight = paint.fontMetrics.let { it.descent - it.ascent }
		val lineSpacing = (textSizeValue / 5) * MyApplication.density

		height = lineHeight * 3 + lineSpacing * 6
		val healthStr = "HP: ${currentHealth.roundToInt()} / ${maxHealth.roundToInt()}"
		val nameWidth = paint.measureText(name).coerceAtLeast(paint.measureText(healthStr))
		width = nameWidth + lineSpacing * 4

		canvas.drawRoundRect(0f, 0f, width, height, lineSpacing, lineSpacing, Paint().apply {
			color = Color.argb(100, 255, 255, 255)
		})

		canvas.drawText(name, lineSpacing * 2, lineSpacing * 2 - paint.fontMetrics.ascent, paint)
		canvas.drawText(healthStr, lineSpacing * 2, lineSpacing * 3 + lineHeight - paint.fontMetrics.ascent, paint)

		canvas.drawRoundRect(lineSpacing * 2, lineSpacing * 4 + lineHeight * 2, lineSpacing * 2 + health * nameWidth, lineSpacing * 4 + lineHeight * 3, lineSpacing, lineSpacing, Paint().apply {
			color = dev.sora.protohax.util.Color.HSBtoRGB(health / 3, 1f, 1f)
		})

		needRefresh.set(true)
	}

	fun stripColor(input: String): String = COLOR_PATTERN.matcher(input).replaceAll("")

	private val handleTargetChange = handle<ModuleTargets.EventTargetChange> {
		session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
	}
}
