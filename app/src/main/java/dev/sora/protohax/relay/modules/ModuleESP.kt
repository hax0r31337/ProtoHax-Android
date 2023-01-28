package dev.sora.protohax.relay.modules

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.nukkitx.math.matrix.Matrix4f
import com.nukkitx.math.vector.Vector2d
import dev.sora.protohax.AppService
import dev.sora.protohax.MainActivity
import dev.sora.protohax.relay.gui.RenderLayerView
import dev.sora.protohax.relay.ui.render.EntityESP
import dev.sora.protohax.util.ColorUtils
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.combat.ModuleAntiBot.isBot
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.cheat.value.ListValue
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.event.Listen
import kotlin.math.cos
import kotlin.math.sin


class ModuleESP : CheatModule("ESP") {

    private val modeValue = ListValue("Mode", arrayOf("Box","RoundBox", "2D", "Glow"), "Box")

    private val fovValue = IntValue("Fov", 110, 40, 110)
    private val allObjectsValue = BoolValue("AllObjects", false)
    private val botsValue = BoolValue("Bots", false)
    private val movePredictionValue = BoolValue("MovePrediction", false)
    val leftStatusBarValue = BoolValue("LeftStatusBar", true)
    private val originalSizeValue = BoolValue("OriginalSize", true)
    private val avoidScreenValue = BoolValue("AvoidScreen", true)
    private val strokeWidthValue = IntValue("StrokeWidth", 2, 1, 10)
    private val rainbowValue = BoolValue("Rainbow", true)
    private val colorRedValue = IntValue("ColorRed", 61, 0, 255)
    private val colorGreenValue = IntValue("ColorGreen", 154, 0, 255)
    private val colorBlueValue = IntValue("ColorBlue", 220, 0, 255)

    override fun onEnable() {
        session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
        displayList.clear()
    }
    var displayList = HashMap<Entity,EntityESP>()
    @Listen
    fun onRender(event: RenderLayerView.EventRender) {
        event.needRefresh = true
        if (avoidScreenValue.get() && event.session.thePlayer.openContainer != null) return
        val map = event.session.theWorld.entityMap.values
            .let { if (allObjectsValue.get()) it else it.filter { e -> e is EntityPlayer && (botsValue.get() || !e.isBot(event.session))} }
        if (map.isEmpty()) return

        val player = event.session.thePlayer
        val canvas = event.canvas
        val realSize = Point()
        (event.context as AppService).windowManager.defaultDisplay.getRealSize(realSize)
        val screenWidth = if(originalSizeValue.get()) realSize.x else canvas.width
        val screenHeight = if(originalSizeValue.get()) realSize.y else canvas.height

        val viewProjMatrix =  Matrix4f.createPerspective(fovValue.get().toFloat()+10, screenWidth.toFloat() / screenHeight, 0.1f, 128f)
            .mul(Matrix4f.createTranslation(player.vec3Position)
                .mul(rotY(-player.rotationYaw-180))
                .mul(rotX(-player.rotationPitch))
                .invert())
//
        val paint = Paint()
        paint.strokeWidth = strokeWidthValue.get().toFloat()
        val colors = ColorUtils.getChromaRainbow(100.0, 10.0)
        paint.color = if(rainbowValue.get()) Color.rgb(colors.r, colors.g, colors.b) else Color.rgb( colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())

        map.forEach {
            drawEntityBox(it, viewProjMatrix, screenWidth, screenHeight, canvas, paint)
        }
    }

    private fun drawEntityBox(entity: Entity, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas, paint: Paint) {
        if(displayList[entity]==null){
            displayList[entity]=EntityESP()
        }
        displayList[entity]!!.draw(entity,viewProjMatrix,screenWidth,screenHeight,canvas,paint,modeValue.get(),movePredictionValue.get(),this)
    }

    fun worldToScreen(posX: Double, posY: Double, posZ: Double, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2d? {
        val w = viewProjMatrix.get(3, 0) * posX +
                viewProjMatrix.get(3, 1) * posY +
                viewProjMatrix.get(3, 2) * posZ +
                viewProjMatrix.get(3, 3)
        if (w < 0.01f) return null
        val inverseW = 1 / w

        val screenX = screenWidth / 2f + (0.5f * ((viewProjMatrix.get(0, 0) * posX + viewProjMatrix.get(0, 1) * posY +
                viewProjMatrix.get(0, 2) * posZ + viewProjMatrix.get(0, 3)) * inverseW) * screenWidth + 0.5f)
        val screenY = screenHeight / 2f - (0.5f * ((viewProjMatrix.get(1, 0) * posX + viewProjMatrix.get(1, 1) * posY +
                viewProjMatrix.get(1, 2) * posZ + viewProjMatrix.get(1, 3)) * inverseW) * screenHeight + 0.5f)
        return Vector2d.from(screenX, screenY)
    }

    private fun rotX(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(1f, 0f, 0f, 0f,
            0f, c, -s, 0f,
            0f, s, c, 0f,
            0f, 0f, 0f, 1f)
    }

    private fun rotY(angle: Float): Matrix4f {
        val rad = Math.toRadians(angle.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()

        return Matrix4f.from(c, 0f, s, 0f,
            0f, 1f, 0f, 0f,
            -s, 0f, c, 0f,
            0f, 0f, 0f, 1f)
    }
}