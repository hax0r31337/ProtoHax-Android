package dev.sora.protohax.ui.overlay.hud

import dev.sora.relay.cheat.value.NamedChoice
import org.cloudburstmc.math.vector.Vector2i

enum class HudAlignment(override val choiceName: String) : NamedChoice {
	LEFT_TOP("LeftTop") {
		override fun getPosition(width: Int, height: Int): Vector2i {
			return Vector2i.from(0, 0)
		}
	},
	LEFT_BOTTOM("LeftBottom") {
		override fun getPosition(width: Int, height: Int): Vector2i {
			return Vector2i.from(0, height)
		}
	},
	RIGHT_TOP("RightTop") {
		override fun getPosition(width: Int, height: Int): Vector2i {
			return Vector2i.from(width, 0)
		}
	},
	RIGHT_BOTTOM("RightBottom") {
		override fun getPosition(width: Int, height: Int): Vector2i {
			return Vector2i.from(width, height)
		}
	},
	CENTER("Center") {
		override fun getPosition(width: Int, height: Int): Vector2i {
			return Vector2i.from(width / 2, height / 2)
		}
	};

	abstract fun getPosition(width: Int, height: Int): Vector2i
}
