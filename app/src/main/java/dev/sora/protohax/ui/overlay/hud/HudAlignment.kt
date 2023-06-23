package dev.sora.protohax.ui.overlay.hud

enum class HudAlignment(val friendlyName: String) {
	LEFT_TOP("LeftTop") {
		override fun getPosition(width: Int, height: Int): Pair<Int, Int> {
			return 0 to 0
		}
	},
	LEFT_BOTTOM("LeftBottom") {
		override fun getPosition(width: Int, height: Int): Pair<Int, Int> {
			return 0 to height
		}
	},
	RIGHT_TOP("RightTop") {
		override fun getPosition(width: Int, height: Int): Pair<Int, Int> {
			return width to 0
		}
	},
	RIGHT_BOTTOM("RightBottom") {
		override fun getPosition(width: Int, height: Int): Pair<Int, Int> {
			return width to height
		}
	},
	CENTER("Center") {
		override fun getPosition(width: Int, height: Int): Pair<Int, Int> {
			return width / 2 to height / 2
		}
	};

	abstract fun getPosition(width: Int, height: Int): Pair<Int, Int>
}
