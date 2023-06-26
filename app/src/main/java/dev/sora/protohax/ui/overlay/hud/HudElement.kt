package dev.sora.protohax.ui.overlay.hud

import android.graphics.Canvas
import androidx.core.graphics.withTranslation
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.relay.cheat.value.Configurable
import dev.sora.relay.cheat.value.Value
import dev.sora.relay.game.event.EventHook
import dev.sora.relay.game.event.EventManager
import dev.sora.relay.game.event.GameEvent
import dev.sora.relay.game.event.Handler
import org.cloudburstmc.math.vector.Vector2i
import java.util.concurrent.atomic.AtomicBoolean

abstract class HudElement(val name: String) : Configurable {

	var posX = 0
	var posY = 0
	var alignment = HudAlignment.LEFT_TOP

	abstract val height: Int
	abstract val width: Int

	override val values = mutableListOf<Value<*>>()
	protected val handlers = mutableListOf<EventHook<in GameEvent>>()

	// cache the value to avoid frequent object creation
	private val needRefresh = AtomicBoolean()

	abstract fun onRender(canvas: Canvas, needRefresh: AtomicBoolean)

	open fun getPosition(canvasWidth: Int, canvasHeight: Int): Vector2i {
		val position = alignment.getPosition(canvasWidth, canvasHeight)
		return Vector2i.from(
			(position.x + posX + width).coerceAtMost(width) - width,
			(position.y + posY + height).coerceAtMost(height) - height
		)
	}

	private val handleRender = handle<RenderLayerView.EventRender> {
		this@HudElement.needRefresh.set(false)
		getPosition(canvas.width, canvas.height).also {
			canvas.withTranslation(it.x.toFloat(), it.y.toFloat()) {
				onRender(this, this@HudElement.needRefresh)
			}
		}
		if (this@HudElement.needRefresh.get()) {
			needRefresh = true
		}
	}

	@Suppress("unchecked_cast")
	protected inline fun <reified T : GameEvent> handle(noinline handler: Handler<T>) {
		handlers.add(EventHook(T::class.java, handler) as EventHook<in GameEvent>)
	}

	fun register(eventManager: EventManager) {
		handlers.forEach(eventManager::register)
	}

	fun unregister(eventManager: EventManager) {
		handlers.forEach(eventManager::removeHandler)
	}
}
