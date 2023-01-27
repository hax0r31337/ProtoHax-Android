package dev.sora.protohax.relay.gui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.View
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.GameEvent
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.Listener

class RenderLayerView(context: Context, private val session: GameSession) : View(context), Listener {

    init {
        session.eventManager.registerListener(this)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val event = EventRender(session, canvas)
        session.eventManager.emit(event)
        if (event.needRefresh) {
            invalidate()
        }
    }

    @Listen
    fun onRefreshRender(event: EventRefreshRender) {
        invalidate()
    }

    override fun listen() = true

    class EventRender(session: GameSession, val canvas: Canvas, var needRefresh: Boolean = false) : GameEvent(session)

    /**
     * call this event when module needs a refresh for the layer,
     * it won't refresh if you don't call this event or set [needRefresh] to true in [EventRefreshRender]
     */
    class EventRefreshRender(session: GameSession) : GameEvent(session)
}