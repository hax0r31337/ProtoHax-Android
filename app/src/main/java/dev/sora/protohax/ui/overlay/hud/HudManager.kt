package dev.sora.protohax.ui.overlay.hud

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.sora.protohax.ui.overlay.RenderLayerView
import dev.sora.protohax.ui.overlay.hud.elements.TextElement
import dev.sora.relay.cheat.config.section.IConfigSection
import dev.sora.relay.game.GameSession
import dev.sora.relay.utils.asJsonObjectOrNull

class HudManager(private val session: GameSession) : IConfigSection {

	val elementRegistry: Map<String, () -> HudElement>
	val elements = mutableListOf<HudElement>()

	init {
	    elementRegistry = mapOf(
			TEXT_ELEMENT_IDENTIFIER to { TextElement() }
		)
	}

	fun addElement(element: HudElement) {
		elements.add(element)
		element.register(session.eventManager)
		session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
	}

	fun addElement(element: String) {
		elementRegistry[element]?.also { e -> addElement(e()) } ?: error("no element called: $element")
	}

	fun removeElement(element: HudElement) {
		elements.remove(element)
		element.unregister(session.eventManager)
		session.eventManager.emit(RenderLayerView.EventRefreshRender(session))
	}

	override val sectionName: String
		get() = "hud"

	override fun load(jsonElement: JsonElement?) {
		val array = jsonElement?.asJsonArray ?: return

		elements.map { it }.forEach {
			removeElement(it)
		}

		array.forEach { obj ->
			val json = obj.asJsonObjectOrNull ?: return@forEach
			val element = elementRegistry[json.get("name").asString]?.invoke() ?: return@forEach
			if (json.has("posX")) {
				element.posX = json.get("posX").asInt
			}
			if (json.has("posY")) {
				element.posY = json.get("posY").asInt
			}

			if (element.values.isNotEmpty()) {
				val valuesJson = json.get("values")?.asJsonObjectOrNull ?: JsonObject()
				element.values.forEach { v ->
					if (valuesJson.has(v.name)) {
						try {
							v.fromJson(valuesJson.get(v.name))
						} catch (t: Throwable) {
							v.reset()
						}
					} else {
						v.reset()
					}
				}
			}

			addElement(element)
		}
	}

	override fun save(): JsonArray {
		val array = JsonArray()

		elements.forEach { element ->
			val json = JsonObject()
			json.addProperty("name", element.name)
			json.addProperty("posX", element.posX)
			json.addProperty("posY", element.posY)
			if (element.values.isNotEmpty()) {
				val valuesJson = JsonObject()
				element.values.forEach { v ->
					valuesJson.add(v.name, v.toJson())
				}
				json.add("values", valuesJson)
			}
			array.add(json)
		}

		return array
	}

	companion object {
		const val TEXT_ELEMENT_IDENTIFIER = "Text"
	}
}
