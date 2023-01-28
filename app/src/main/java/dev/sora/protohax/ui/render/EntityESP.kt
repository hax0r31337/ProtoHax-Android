package dev.sora.protohax.ui.render

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.nukkitx.math.matrix.Matrix4f
import dev.sora.protohax.relay.modules.ModuleESP
import dev.sora.protohax.util.AnimationUtils
import dev.sora.relay.game.entity.Entity
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.game.utils.TimerUtil

class EntityESP {

    private val minXTimer = TimerUtil()
    private var minXFloat = 0.0f
    private val minYTimer = TimerUtil()
    private var minYFloat = 0.0f
    private val maxXTimer = TimerUtil()
    private var maxXFloat = 0.0f
    private val maxYTimer = TimerUtil()
    private var maxYFloat = 0.0f
    fun draw(entity: Entity, viewProjMatrix: Matrix4f, screenWidth: Int, screenHeight: Int, canvas: Canvas, paint: Paint,mode: String,movePredictionValue: Boolean,m:ModuleESP){
        if(entity is EntityPlayer) if (entity.username.isEmpty()) return
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
            val screenPos = m.worldToScreen(boxVertex[0], boxVertex[1], boxVertex[2], viewProjMatrix, screenWidth, screenHeight)?: continue
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
        // out of screen
        if (!(minX >= screenWidth || minY >= screenHeight || maxX <= 0 || maxY <= 0)) {
            val fit = paint.strokeWidth * 0.5f
            val buttonBackground = Paint()
            when(mode){
                "2D"->{
                    canvas.drawLine(this.minXFloat - fit, this.minYFloat, this.maxXFloat + fit, this.minYFloat, paint)
                    canvas.drawLine(this.minXFloat - fit, this.maxYFloat, this.maxXFloat + fit, this.maxYFloat, paint)
                    canvas.drawLine(this.minXFloat, this.minYFloat, this.minXFloat, this.maxYFloat, paint)
                    canvas.drawLine(this.maxXFloat, this.minYFloat, this.maxXFloat, this.maxYFloat, paint)
                }
                "RoundBox"->{
                    val width = this.maxXFloat - this.minXFloat
                    buttonBackground.color = Color.argb(180,paint.color.red , paint.color.green, paint.color.blue)
                    buttonBackground.style = Paint.Style.FILL
                    buttonBackground.maskFilter = null
                    canvas.drawRoundRect(
                        this.minXFloat,
                        this.minYFloat,
                        this.maxXFloat,
                        this.maxYFloat, width/4f, width/4f,
                        buttonBackground
                    )
                }
                "Glow"->{
                    buttonBackground.color = Color.argb(180,paint.color.red , paint.color.green, paint.color.blue)
                    buttonBackground.style = Paint.Style.FILL
                    buttonBackground.maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
                    canvas.drawRect(
                        this.minXFloat,
                        this.minYFloat,
                        this.maxXFloat,
                        this.maxYFloat,
                        buttonBackground
                    )
                }
                "Box"->{
                    buttonBackground.color = Color.argb(180,paint.color.red , paint.color.green, paint.color.blue)
                    buttonBackground.style = Paint.Style.FILL
                    buttonBackground.maskFilter = null
                    canvas.drawRect(
                        this.minXFloat,
                        this.minYFloat,
                        this.maxXFloat,
                        this.maxYFloat,
                        buttonBackground
                    )
                }
            }
            if(movePredictionValue){
                val width = this.maxXFloat - this.minXFloat
                val posX = this.minXFloat + (width / 2)
                val width2 = maxX - minX
                val posX2 = minX + (width2 / 2)
                canvas.drawLine(posX,maxYFloat, posX2.toFloat(), maxY.toFloat(),paint)
            }
        }
    }
}