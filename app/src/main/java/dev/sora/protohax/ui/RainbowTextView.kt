package dev.sora.protohax.ui

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.widget.TextView
import dev.sora.protohax.R


class RainbowTextView(ctx: Context) : TextView(ctx) {
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val rainbow = rainbowColors
        val shader: Shader = LinearGradient(
            0f, 0f, 0f, w.toFloat(), rainbow,
            null, Shader.TileMode.MIRROR
        )
        val matrix = Matrix()
        matrix.setRotate(90f)
        shader.setLocalMatrix(matrix)
        paint.shader = shader
    }

    private val rainbowColors: IntArray
        get() = intArrayOf(
            resources.getColor(R.color.green),
            resources.getColor(R.color.yellow),
            resources.getColor(R.color.orange),
            resources.getColor(R.color.red)
        )
}