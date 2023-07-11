package dev.sora.protohax.ui.overlay

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.graphics.*
import android.hardware.input.InputManager
import android.os.Build
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import dev.sora.protohax.R
import dev.sora.protohax.ui.components.screen.settings.Settings
import dev.sora.protohax.util.ContextUtils.getColor
import dev.sora.relay.game.GameSession
import dev.sora.relay.game.event.*


@SuppressLint("ClickableViewAccessibility")
class RenderLayerView(ctx: Context, private val windowManager: WindowManager, private val session: GameSession) : View(ctx) {

	private val listener = EventHook(EventRefreshRender::class.java, handler = {
		postInvalidate()
	})

	var editMode: Boolean = false
	val params: WindowManager.LayoutParams

	init {
	    session.eventManager.register(listener)

		params = WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.MATCH_PARENT,
			WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
			PixelFormat.TRANSLUCENT
		)
		// android introduced so called "untrusted clicks" to protect user from fraud applications
		// this security feature affects ProtoHax
		// https://developer.android.com/about/versions/12/behavior-changes-all#untrusted-touch-events
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Settings.trustClicks.getValue(ctx)) {
			params.alpha = (ctx.getSystemService(Service.INPUT_SERVICE) as? InputManager)?.maximumObscuringOpacityForTouch ?: 0.8f
		}
		params.gravity = Gravity.TOP or Gravity.END

		val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
			override fun onDown(e: MotionEvent): Boolean {
				return true
			}

			override fun onSingleTapUp(e: MotionEvent): Boolean {
				session.eventManager.emit(EventRenderLayerClick(session, e, this@RenderLayerView))
				if (e.y > height - 150 && e.x < 150f) {
					quitEdit()
				}
				return true
			}
		})

		setOnTouchListener { _, event ->
			gestureDetector.onTouchEvent(event)
			val sessionEvent = EventRenderLayerMotion(session, event, this@RenderLayerView)
			session.eventManager.emit(sessionEvent)
			if (sessionEvent.hasHandled) {
				invalidate()
			}

			true
		}

		windowManager.addView(this, params)
	}

	fun edit() {
		if (editMode) return

		params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
		windowManager.updateViewLayout(this, params)
		editMode = true

		invalidate()
	}

	fun quitEdit() {
		if (!editMode) return

		params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
		windowManager.updateViewLayout(this, params)
		editMode = false

		invalidate()
	}

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val event = EventRender(session, canvas, editMode)
        session.eventManager.emit(event)
        if (event.needRefresh) {
            invalidate()
        }

		if (editMode) {
			drawQuitEditButton(canvas)
		}
    }

	private fun drawQuitEditButton(canvas: Canvas) {
		val corners = floatArrayOf(
			0f, 0f,   // Top left radius in px
			25f, 25f,   // Top right radius in px
			0f, 0f,     // Bottom right radius in px
			0f, 0f      // Bottom left radius in px
		)

		val path = Path()
		val rect = RectF(0f, height - 150f, 150f, height.toFloat())
		path.addRoundRect(rect, corners, Path.Direction.CW)
		canvas.drawPath(path, Paint().apply {
			color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				context.getColor(context.getColor(android.R.color.system_accent1_600, android.R.color.system_accent1_200))
			} else context.getColor(Color.rgb(103, 80, 164), Color.rgb(208, 188, 255))
		})

		val icon = resources.getDrawable(R.drawable.mdi_arrow_back, context.theme)
		icon.setBounds(rect.left.toInt() + 30, rect.top.toInt() + 30, rect.right.toInt() - 30, rect.bottom.toInt() - 30)
		icon.setTint(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			context.getColor(context.getColor(android.R.color.system_accent1_0, android.R.color.system_accent1_800))
		} else context.getColor(Color.rgb(255, 255, 255), Color.rgb(56, 30, 114)))
		icon.draw(canvas)
	}

	fun destroy() {
		// detach event listener
		session.eventManager.removeHandler(listener)
		windowManager.removeView(this)
	}

    class EventRender(session: GameSession, val canvas: Canvas, val editMode: Boolean, var needRefresh: Boolean = false) : GameEvent(session, "render")

    /**
     * notify the render layer to refresh at next frame
     * it won't refresh unless you don't call this event or set [needRefresh] to true in [EventRefreshRender]
     */
    class EventRefreshRender(session: GameSession) : GameEvent(session, "refresh_render")

	class EventRenderLayerClick(session: GameSession, val event: MotionEvent, val view: RenderLayerView) : GameEvent(session, "render_click")

	class EventRenderLayerMotion(session: GameSession, val event: MotionEvent, val view: RenderLayerView, var hasHandled: Boolean = false) : GameEvent(session, "render_motion")
}
