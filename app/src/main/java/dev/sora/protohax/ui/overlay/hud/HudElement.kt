package dev.sora.protohax.ui.overlay.hud

import android.graphics.Canvas
import android.graphics.Rect
import dev.sora.relay.cheat.value.Configurable
import dev.sora.relay.cheat.value.Value
import dev.sora.relay.game.event.EventHook
import dev.sora.relay.game.event.EventManager
import dev.sora.relay.game.event.GameEvent
import dev.sora.relay.game.event.Handler
import java.util.concurrent.atomic.AtomicBoolean

abstract class HudElement(val name: String) : Configurable {

	var posX = 0
	var posY = 0
	var alignment = HudAlignment.LEFT_TOP

	override val values = mutableListOf<Value<*>>()
	protected val handlers = mutableListOf<EventHook<in GameEvent>>()

	abstract fun onRender(canvas: Canvas, needRefresh: AtomicBoolean): Rect?

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
