package dev.sora.protohax.ui.overlay

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.sora.protohax.relay.MinecraftRelay
import dev.sora.relay.cheat.config.section.IConfigSection
import dev.sora.relay.utils.asJsonObjectOrNull

class ConfigSectionShortcut(private val overlayManager: OverlayManager) : IConfigSection {

	override fun load(element: JsonElement?) {
		overlayManager.shortcuts.forEach {
			overlayManager.removeShortcut(it.module)
		}

		element ?: return
		if (!element.isJsonArray) {
			return
		}
		val array = element.asJsonArray

		array.forEach { obj ->
			val json = obj.asJsonObjectOrNull ?: return@forEach
			if (!json.has("module")) {
				return@forEach
			}
			val module = MinecraftRelay.moduleManager.modules.find { it.name == json.get("module").asString } ?: return@forEach
			val shortcut = Shortcut(module, overlayManager)
			if (json.has("x")) {
				shortcut.params.x = json.get("x").asInt
			}
			if (json.has("y")) {
				shortcut.params.y = json.get("y").asInt
			}

			overlayManager.addShortcut(shortcut)
		}
	}

	override fun save(): JsonArray {
		val array = JsonArray()

		overlayManager.shortcuts.forEach { shortcut ->
			val json = JsonObject()

			json.addProperty("module", shortcut.module.name)
			json.addProperty("x", shortcut.params.x)
			json.addProperty("y", shortcut.params.y)

			array.add(json)
		}

		return array
	}

	override val sectionName: String
		get() = "shortcut"
}
