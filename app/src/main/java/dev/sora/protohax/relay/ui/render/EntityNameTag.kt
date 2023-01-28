package dev.sora.protohax.relay.ui.render

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.nukkitx.math.matrix.Matrix4f
import dev.sora.protohax.relay.modules.ModuleESP
import dev.sora.protohax.relay.modules.ModuleNameTag
import dev.sora.protohax.util.AnimationUtils
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.entity.EntityPlayerSP
import dev.sora.relay.game.utils.TimerUtil
import java.util.regex.Pattern

class EntityNameTag {

    private val minXTimer = TimerUtil()
    private var minXFloat = 0.0f
    private val minYTimer = TimerUtil()
    private var minYFloat = 0.0f
    private val maxXTimer = TimerUtil()
    private var maxXFloat = 0.0f
    private val maxYTimer = TimerUtil()
    private var maxYFloat = 0.0f
    fun draw(
        entity: EntityPlayer,
        viewProjMatrix: Matrix4f,
        screenWidth: Int,
        screenHeight: Int,
        canvas: Canvas,
        m: ModuleNameTag
    ) {
        if(entity.username.isEmpty()) return
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
            val screenPos = m.worldToScreen(
                boxVertex[0],
                boxVertex[1],
                boxVertex[2],
                viewProjMatrix,
                screenWidth,
                screenHeight
            ) ?: continue
            minX = screenPos.x.coerceAtMost(minX)
            minY = screenPos.y.coerceAtMost(minY)
            maxX = screenPos.x.coerceAtLeast(maxX)
            maxY = screenPos.y.coerceAtLeast(maxY)
        }
        if (this.minXTimer.elapsed(15L)) {
            this.minXFloat = AnimationUtils.animate(
                minX,
                this.minXFloat.toDouble(), 0.2
            ).toFloat()
            this.minXTimer.reset();
        }
        if (this.minYTimer.elapsed(15L)) {
            this.minYFloat = AnimationUtils.animate(
                minY,
                this.minYFloat.toDouble(), 0.2
            ).toFloat()
            this.minXTimer.reset();
        }
        if (this.maxXTimer.elapsed(15L)) {
            this.maxXFloat = AnimationUtils.animate(
                maxX,
                this.maxXFloat.toDouble(), 0.2
            ).toFloat()
            this.maxXTimer.reset();
        }
        if (this.maxYTimer.elapsed(15L)) {
            this.maxYFloat = AnimationUtils.animate(
                maxY,
                this.maxYFloat.toDouble(), 0.2
            ).toFloat()
            this.maxYTimer.reset();
        }

        if (!(minX >= screenWidth || minY >= screenHeight || maxX <= 0 || maxY <= 0)) {
            val width = this.maxXFloat - this.minXFloat
            val posX = this.minXFloat + (width / 2)
            font.textSize = 18f
            font.textAlign=Paint.Align.CENTER
            font.color = Color.argb(255, 255  , 255, 255)
            buttonBackground.color = Color.argb(180, 0, 0, 0)
            buttonBackground.style = Paint.Style.FILL
            buttonBackground.maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawRoundRect(
                posX - (font.measureText(stripControlCodes(entity.username)) / 2) - 10,
                this.minYFloat - 45,
                posX + (font.measureText(stripControlCodes(entity.username)) / 2) + 10,
                this.minYFloat - 10, 8f, 8f,
                buttonBackground
            )
            buttonBackground.maskFilter = null
            canvas.drawRoundRect(
                posX - (font.measureText(stripControlCodes(entity.username)) / 2) - 10,
                this.minYFloat - 45,
                posX + (font.measureText(stripControlCodes(entity.username)) / 2) + 10,
                this.minYFloat - 10, 8f, 8f,
                buttonBackground
            )
            canvas.drawText(
                stripControlCodes(entity.username)!!,
                posX - 0, this.minYFloat-20f, font
            )
        }

    }
    private val patternControlCode: Pattern = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]")

    private fun stripControlCodes(p_76338_0_: String?): String? {
        return p_76338_0_?.let { patternControlCode.matcher(it).replaceAll("") }
    }
    private val font = Paint()
    private val buttonBackground = Paint()
}