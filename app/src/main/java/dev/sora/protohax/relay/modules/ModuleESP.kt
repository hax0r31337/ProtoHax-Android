package dev.sora.protohax.relay.modules

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.nukkitx.math.matrix.Matrix4f
import com.nukkitx.math.vector.Vector2d
import dev.sora.protohax.relay.gui.RenderLayerView
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.ModuleAntiBot.isBot
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.Listen
import kotlin.math.cos
import kotlin.math.sin


class ModuleESP : CheatModule("ESP") {

    private val fovValue = intValue("Fov", 110, 40, 110)
    private val allObjectsValue = boolValue("AllObjects", false)
    private val botsValue = boolValue("Bots", false)
    private val avoidScreenValue = boolValue("AvoidScreen", true)
    private val strokeWidthValue = intValue("StrokeWidth", 2, 1, 10)
    private val colorRedValue = intValue("ColorRed", 61, 0, 255)
    private val colorGreenValue = intValue("ColorGreen", 154, 0, 255)
    private val colorBlueValue = intValue("ColorBlue", 220, 0, 255)

    override fun onEnable() {
        session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
    }

    @Listen
    fun onRender(event: RenderLayerView.EventRender) {
        event.needRefresh = true
        if (avoidScreenValue.get() && event.session.thePlayer.openContainer != null) return
        val map = event.session.theWorld.entityMap.values
            .let { if (allObjectsValue.get()) it else it.filter { e -> e is EntityPlayer && (botsValue.get() || !e.isBot(event.session))} }
        if (map.isEmpty()) return

        val player = event.session.thePlayer
        val canvas = event.canvas
        val screenWidth = canvas.width
        val screenHeight = canvas.height
        val renderPartialTicks = player.renderPartialTicks

        val viewProjMatrix =  Matrix4f.createPerspective(fovValue.get().toFloat()+10, screenWidth.toFloat() / screenHeight, 0.1f, 128f)
            .mul(Matrix4f.createTranslation(player.vec3Position)
                .mul(rotY(-(player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * renderPartialTicks)-180))
                .mul(rotX(-(player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * renderPartialTicks)))
                .invert())

        val paint = Paint()
        paint.strokeWidth = strokeWidthValue.get().toFloat()
        paint.color = Color.rgb(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())

        map.forEach {
            drawEntityBox(it, viewProjMatrix, screenWidth, screenHeight, canvas, paint)
        }
    }

    private fun drawEntityBox(entity: Entity, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas, paint: Paint) {
        var minX = entity.posX - 0.3
        val minZ = entity.posZ - 0.3
        var maxX = entity.posX + 0.3
        val maxZ = entity.posZ + 0.3
        var minY = entity.posY
        var maxY = entity.posY + 1
        val boxVertices = if (entity is EntityPlayer) {
            minY -= 1.62
            maxY -= 0.82
            arrayOf(
                doubleArrayOf(minX, minY, minZ),
                doubleArrayOf(minX, maxY, minZ),
                doubleArrayOf(maxX, maxY, minZ),
                doubleArrayOf(maxX, minY, minZ),
                doubleArrayOf(minX, minY, maxZ),
                doubleArrayOf(minX, maxY, maxZ),
                doubleArrayOf(maxX, maxY, maxZ),
                doubleArrayOf(maxX, minY, maxZ)
            )
        } else {
            arrayOf(
                doubleArrayOf(minX, minY, minZ),
                doubleArrayOf(minX, maxY, minZ),
                doubleArrayOf(maxX, maxY, minZ),
                doubleArrayOf(maxX, minY, minZ),
                doubleArrayOf(minX, minY, maxZ),
                doubleArrayOf(minX, maxY, maxZ),
                doubleArrayOf(maxX, maxY, maxZ),
                doubleArrayOf(maxX, minY, maxZ)
            )
        }

        minX = screenWidth.toDouble()
        minY = screenHeight.toDouble()
        maxX = .0
        maxY = .0
        for (boxVertex in boxVertices) {
            val screenPos = worldToScreen(boxVertex[0], boxVertex[1], boxVertex[2], viewProjMatrix, screenWidth, screenHeight)?: continue
            minX = screenPos.x.coerceAtMost(minX)
            minY = screenPos.y.coerceAtMost(minY)
            maxX = screenPos.x.coerceAtLeast(maxX)
            maxY = screenPos.y.coerceAtLeast(maxY)
        }
        // out of screen
        if (!(minX >= screenWidth || minY >= screenHeight || maxX <= 0 || maxY <= 0)) {
            val fit = paint.strokeWidth * 0.5f
            canvas.drawLine(minX.toFloat() - fit, minY.toFloat(), maxX.toFloat() + fit, minY.toFloat(), paint)
            canvas.drawLine(minX.toFloat() - fit, maxY.toFloat(), maxX.toFloat() + fit, maxY.toFloat(), paint)
            canvas.drawLine(minX.toFloat(), minY.toFloat(), minX.toFloat(), maxY.toFloat(), paint)
            canvas.drawLine(maxX.toFloat(), minY.toFloat(), maxX.toFloat(), maxY.toFloat(), paint)
        }
    }

    private fun worldToScreen(posX: Double, posY: Double, posZ: Double, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2d? {
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