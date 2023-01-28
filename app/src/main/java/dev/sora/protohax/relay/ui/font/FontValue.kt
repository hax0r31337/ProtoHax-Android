package dev.sora.relay.cheat.value

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

/**
 * Text value represents a value with a string
 */
open class FontValue(name: String, size: Float) : Value<Float>(name, size) {
    override fun toJson() = JsonPrimitive(value)

    override fun fromJson(element: JsonElement) {
        if (element.isJsonPrimitive) {
            value = element.asFloat
        }
    }
}