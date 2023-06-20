package dev.sora.protohax.relay.modules

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.combat.ModuleTargets
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import org.cloudburstmc.math.matrix.Matrix4f
import org.cloudburstmc.math.vector.Vector2f
import kotlin.math.cos
import kotlin.math.sin


class ModuleESP : CheatModule("ESP", CheatCategory.VISUAL) {

    private var fovValue by intValue("Fov", 110, 40..110)
    private var allObjectsValue by boolValue("AllObjects", false)
    private var botsValue by boolValue("Bots", false)
    private var avoidScreenValue by boolValue("AvoidScreen", true)
    private var strokeWidthValue by intValue("StrokeWidth", 2, 1..10)
    private var colorRedValue by intValue("ColorRed", 61, 0..255)
    private var colorGreenValue by intValue("ColorGreen", 154, 0..255)
    private var colorBlueValue by intValue("ColorBlue", 220, 0..255)

    override fun onEnable() {
        session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
    }

 	private val handleRender = handle<RenderLayerView.EventRender> {
		needRefresh = true
		if (avoidScreenValue && session.player.openContainer != null) return@handle
		val moduleTargets = moduleManager.getModule(ModuleTargets::class.java)
		val map = session.level.entityMap.values
			.let { if (allObjectsValue) it else it.filter { e -> e is EntityPlayer && (botsValue || with(moduleTargets) { !e.isBot() })} }
		if (map.isEmpty()) return@handle

		val player = session.player
		val screenWidth = canvas.width
		val screenHeight = canvas.height
		val renderPartialTicks = player.renderPartialTicks

		val viewProjMatrix =  Matrix4f.createPerspective(fovValue.toFloat(), screenWidth.toFloat() / screenHeight, 0.1f, 128f)
			.mul(Matrix4f.createTranslation(player.vec3Position)
				.mul(rotY(-(player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * renderPartialTicks)-180))
				.mul(rotX(-(player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * renderPartialTicks)))
				.invert())

		val paint = Paint()
		paint.strokeWidth = strokeWidthValue.toFloat()
		paint.color = Color.rgb(colorRedValue, colorGreenValue, colorBlueValue)

		map.forEach {
			drawEntityBox(it, viewProjMatrix, screenWidth, screenHeight, canvas, paint)
		}
	}

    private fun drawEntityBox(entity: Entity, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas, paint: Paint) {
        var minX = entity.posX - 0.3f
        val minZ = entity.posZ - 0.3f
        var maxX = entity.posX + 0.3f
        val maxZ = entity.posZ + 0.3f
        var minY = entity.posY
        var maxY = entity.posY + 1f
		if (entity is EntityPlayer) {
			minY -= 1.62f
			maxY -= 0.82f
		}
        val boxVertices =
            arrayOf(
                floatArrayOf(minX, minY, minZ),
				floatArrayOf(minX, maxY, minZ),
				floatArrayOf(maxX, maxY, minZ),
				floatArrayOf(maxX, minY, minZ),
				floatArrayOf(minX, minY, maxZ),
				floatArrayOf(minX, maxY, maxZ),
				floatArrayOf(maxX, maxY, maxZ),
				floatArrayOf(maxX, minY, maxZ)
            )

        minX = screenWidth.toFloat()
        minY = screenHeight.toFloat()
        maxX = 0f
        maxY = 0f
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
			canvas.drawLine(minX - fit, minY, maxX + fit, minY, paint)
			canvas.drawLine(minX - fit, maxY, maxX + fit, maxY, paint)
			canvas.drawLine(minX, minY, minX, maxY, paint)
			canvas.drawLine(maxX, minY, maxX, maxY, paint)
        }
    }

    private fun worldToScreen(posX: Float, posY: Float, posZ: Float, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int): Vector2f? {
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
        return Vector2f.from(screenX, screenY)
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
