package dev.sora.protohax.relay.gui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.net.http.SslError
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import dev.sora.protohax.AppService
import dev.sora.protohax.relay.modules.ModuleESP
import dev.sora.protohax.relay.ui.`interface`.WebAppInterface
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.GameEvent
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.Listener


@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class RenderLayerView(context: Context, private val session: GameSession) : View(context),
    Listener {
    init {
        session.eventManager.registerListener(this)
        WebView.setWebContentsDebuggingEnabled(true);
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if ((session.moduleManager.getModuleByName("ESP") as ModuleESP).leftStatusBarValue.get()) {
            val realSize = Point()
            (context as AppService).windowManager.defaultDisplay.getRealSize(realSize)
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels - realSize.y
            left = -screenHeight
        }
        val event = EventRender(session, canvas, context)
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

    class EventRender(
        session: GameSession,
        val canvas: Canvas,
        val context: Context,
        var needRefresh: Boolean = false
    ) : GameEvent(session)

    /**
     * call this event when module needs a refresh for the layer,
     * it won't refresh if you don't call this event or set [needRefresh] to true in [EventRefreshRender]
     */
    class EventRefreshRender(session: GameSession) : GameEvent(session)
}