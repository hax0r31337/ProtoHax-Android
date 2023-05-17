package dev.sora.protohax.ui.components.screen.settings

import dev.sora.protohax.R
import dev.sora.protohax.relay.MinecraftRelay

object Settings {

	val offlineSessionEncryption = BoolSetting(R.string.setting_encryption, R.string.setting_encryption_desc, "OFFLINE_SESSION_ENCRYPTION", false)
	val enableCommandManager = BoolSetting(R.string.setting_commands, R.string.setting_commands_desc, "ENABLE_COMMAND_MANAGER", true, restartRequired = true)
	val enableRakReliability = BoolSetting(R.string.setting_rak_reliability, R.string.setting_rak_reliability_desc, "ENABLE_RAK_RELIABILITY", true) {
		MinecraftRelay.updateReliability()
	}
	val ipv6Status = TabSetting(R.string.setting_ip, "INTERNET_PROTOCOL", IPv6Choices.AUTOMATIC, IPv6Choices.values(), restartRequired = true)

//	const val KEY_INTERNET_PROTOCOL = "ENABLE_INTERNET_PROTOCOL"
//	const val KEY_INTERNET_PROTOCOL_DEFAULT = "auto"

	val settings = arrayOf(offlineSessionEncryption, enableCommandManager, enableRakReliability, ipv6Status)

	enum class IPv6Choices(override val displayName: Int, override val internalName: String) : TabChoice {
		AUTOMATIC(R.string.setting_ip_auto, "auto"),
		ENABLED(R.string.setting_ip_enabled, "enabled"),
		DISABLED(R.string.setting_ip_disabled, "disabled"),
		V6ONLY(R.string.setting_ip_only, "v6only"),
	}
}
