package dev.sora.protohax.relay.modules

import android.graphics.Canvas
import android.graphics.Point
import com.nukkitx.math.matrix.Matrix4f
import com.nukkitx.math.vector.Vector2d
import dev.sora.protohax.AppService
import dev.sora.protohax.relay.gui.RenderLayerView
import dev.sora.protohax.relay.ui.render.EntityNameTag
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.event.Listen
import kotlin.math.cos
import kotlin.math.sin


class ModuleNameTag : CheatModule("NameTag") {

    private val fovValue = IntValue("Fov", 110, 40, 110)
    private val botsValue = BoolValue("Bots", false)
    val leftStatusBarValue = BoolValue("LeftStatusBar", true)
    private val originalSizeValue = BoolValue("OriginalSize", true)
    private val avoidScreenValue = BoolValue("AvoidScreen", true)

    override fun onEnable() {
        session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
        displayList.clear()
    }
    var displayList = HashMap<EntityPlayer,EntityNameTag>()
    @Listen
    fun onRender(event: RenderLayerView.EventRender) {
        event.needRefresh = true
        if (avoidScreenValue.get() && event.session.thePlayer.openContainer != null) return
        val map = event.session.theWorld.entityMap.values.filterIsInstance<EntityPlayer>()
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
        map.forEach {
            drawEntityBox(it, viewProjMatrix, screenWidth, screenHeight, canvas)
        }
    }

    private fun drawEntityBox(entity: EntityPlayer, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas) {
        if(displayList[entity]==null){
            displayList[entity]= EntityNameTag()
        }
        displayList[entity]!!.draw(entity,viewProjMatrix,screenWidth,screenHeight,canvas,this)

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