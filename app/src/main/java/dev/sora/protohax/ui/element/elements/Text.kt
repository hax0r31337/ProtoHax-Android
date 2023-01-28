package dev.sora.protohax.ui.element.elements

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.inputmethodservice.Keyboard
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import dev.sora.protohax.relay.Client
import dev.sora.protohax.relay.MinecraftRelay.session
import dev.sora.protohax.ui.element.Border
import dev.sora.protohax.ui.element.Element
import dev.sora.protohax.ui.element.ElementInfo
import dev.sora.protohax.ui.element.Side
import dev.sora.protohax.util.ColorUtils
import dev.sora.relay.cheat.value.BoolValue
import dev.sora.protohax.ui.font.FontValue
import dev.sora.relay.cheat.value.IntValue
import dev.sora.relay.cheat.value.StringValue
import dev.sora.relay.game.GameSession
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import kotlin.math.sqrt

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "Text")
class Text(
    x: Double = 100.0, y: Double = 100.0, scale: Float = 1F,
    side: Side = Side.default()
) : Element(x, y, scale, side) {

    companion object {

        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
        val HOUR_FORMAT = SimpleDateFormat("HH:mm")

        val DECIMAL_FORMAT = DecimalFormat("0.00")

        /**
         * Create default element
         */
        fun defaultClient(): Text {
            val text = Text(x = 50.0, y = 80.0, scale = 1F)
            text.displayString.set("%clientName%")
            text.shadow.set(true)
            text.fontValue.set(80f)
            text.setColor(Color.rgb(0, 111, 255))
            text.updateElement()
            return text
        }

    }

    private val displayString = StringValue("DisplayText", "")
    private val redValue = IntValue("Red", 255, 0, 255)
    private val greenValue = IntValue("Green", 255, 0, 255)
    private val blueValue = IntValue("Blue", 255, 0, 255)
    private val rainbow = BoolValue("Rainbow", false)
    private val rainbow2 = BoolValue("AllRainbow", true)
    private val shadow = BoolValue("Shadow", true)
    private var fontValue = FontValue("Font", 40f)

    private var editMode = false
    private var editTicks = 0
    private var prevClick = 0L

    private var displayText = display

    private val display: String
        get() {
            val textContent = displayString.get().ifEmpty { "文本渲染测试" }
            return multiReplace(textContent)
        }

    private fun getReplacement(str: String): String? {
        val thePlayer = session.thePlayer

        when (str.lowercase()) {
            "x" -> return DECIMAL_FORMAT.format(thePlayer.posX)
            "y" -> return DECIMAL_FORMAT.format(thePlayer.posY)
            "z" -> return DECIMAL_FORMAT.format(thePlayer.posZ)
            "xdp" -> return thePlayer.posX.toString()
            "ydp" -> return thePlayer.posY.toString()
            "zdp" -> return thePlayer.posZ.toString()
            "velocity" -> return DECIMAL_FORMAT.format(sqrt(thePlayer.motionX * thePlayer.motionX + thePlayer.motionZ * thePlayer.motionZ))
        }

        return when (str.lowercase()) {
            "username" -> session.displayName
            "clientname" -> Client.CLIENT_NAME
            "clientversion" -> "b${Client.CLIENT_VERSION}"
            "clientcreator" -> Client.CLIENT_CREATOR
            "date" -> DATE_FORMAT.format(System.currentTimeMillis())
            "time" -> HOUR_FORMAT.format(System.currentTimeMillis())
            else -> null // Null = don't replace
        }
    }

    private fun multiReplace(str: String): String {
        var lastPercent = -1
        val result = StringBuilder()
        for (i in str.indices) {
            if (str[i] == '%') {
                if (lastPercent != -1) {
                    if (lastPercent + 1 != i) {
                        val replacement = getReplacement(str.substring(lastPercent + 1, i))

                        if (replacement != null) {
                            result.append(replacement)
                            lastPercent = -1
                            continue
                        }
                    }
                    result.append(str, lastPercent, i)
                }
                lastPercent = i
            } else if (lastPercent == -1) {
                result.append(str[i])
            }
        }

        if (lastPercent != -1) {
            result.append(str, lastPercent, str.length)
        }

        return result.toString()
    }
    val font = Paint()
    /**
     * Draw element
     */
    override fun drawElement(session: GameSession, canvas: Canvas, context: Context): Border {
        if(rainbow.get()) {
            var i = 0
            for (c in displayText.toCharArray()) {
                i++
                font.textSize = fontValue.get() * scale
                if (shadow.get()) {
                    font.color = Color.argb(160, 0, 0, 0)
                    canvas.drawText(
                        c.toString(),
                        (renderX + (fontValue.get() / 20f) + font.measureText(
                            displayText,
                            0,
                            i
                        )).toFloat(), renderY.toFloat() + (fontValue.get() / 20f), font
                    )
                }
                val colors = ColorUtils.getChromaRainbow(100.0 + (i * fontValue.get() / 4f), 10.0)
                font.color = Color.rgb(colors.r, colors.g, colors.b)
                canvas.drawText(
                    c.toString(),
                    (renderX + font.measureText(displayText, 0, i)).toFloat(),
                    renderY.toFloat(),
                    font
                )
            }
        }else{
            font.textSize = fontValue.get() * scale
            if (shadow.get()) {
                font.color = Color.argb(160, 0, 0, 0)
                canvas.drawText(
                    displayText,
                    (renderX + (fontValue.get() / 20f)).toFloat(), renderY.toFloat() + (fontValue.get() / 20f), font
                )
            }
            val colors = ColorUtils.getChromaRainbow(100.0, 10.0)
            font.color = if(rainbow2.get()) Color.rgb(colors.r, colors.g, colors.b) else Color.rgb(redValue.get(), greenValue.get(), blueValue.get())
            canvas.drawText(
                displayText,
                renderX.toFloat(), renderY.toFloat(), font
            )
        }
        return Border(
            -2F,
            -2F,
            font.measureText(displayText), 0f
        )
    }

    override fun updateElement() {
        editTicks += 5
        if (editTicks > 80) editTicks = 0

        displayText = if (editMode) displayString.get() else display
    }

    override fun handleMouseClick(x: Double, y: Double, mouseButton: Int) {
        if (isInBorder(x, y) && mouseButton == 0) {
            if (System.currentTimeMillis() - prevClick <= 250L)
                editMode = true

            prevClick = System.currentTimeMillis()
        } else {
            editMode = false
        }
    }

    override fun handleKey(c: Char, keyCode: Int) {
        if (editMode) {
            if (keyCode == Keyboard.KEYCODE_CANCEL) {
                if (displayString.get().isNotEmpty())
                    displayString.set(
                        displayString.get().substring(0, displayString.get().length - 1)
                    )

                updateElement()
                return
            }

            if (isAllowedCharacter(c) || c == '§')
                displayString.set(displayString.get() + c)

            updateElement()
        }
    }

    fun isAllowedCharacter(character: Char): Boolean {
        return character.toInt() != 167 && character.toInt() >= 32 && character.toInt() != 127
    }

    fun setColor(c: Int): Text {
        redValue.set(c.red)
        greenValue.set(c.green)
        blueValue.set(c.blue)
        return this
    }

}
