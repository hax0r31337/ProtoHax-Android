package dev.sora.protohax.relay

import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.modules.ModuleESP
import dev.sora.protohax.relay.netty.channel.NativeRakConfig
import dev.sora.protohax.relay.netty.channel.NativeRakServerChannel
import dev.sora.protohax.ui.components.screen.settings.Settings
import dev.sora.protohax.ui.overlay.ConfigSectionShortcut
import dev.sora.protohax.ui.overlay.hud.HudManager
import dev.sora.relay.MinecraftRelayListener
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.command.impl.CommandDownloadWorld
import dev.sora.relay.cheat.config.ConfigManagerFileSystem
import dev.sora.relay.cheat.config.section.ConfigSectionModule
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.misc.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.session.listener.RelayListenerAutoCodec
import dev.sora.relay.session.listener.RelayListenerEncryptedSession
import dev.sora.relay.session.listener.RelayListenerNetworkSettings
import dev.sora.relay.session.listener.xbox.RelayListenerXboxLogin
import dev.sora.relay.session.listener.xbox.cache.XboxChainCacheFileSystem
import dev.sora.relay.utils.logInfo
import io.netty.channel.ChannelFactory
import io.netty.channel.ServerChannel
import org.cloudburstmc.netty.channel.raknet.RakReliability
import java.io.File
import java.net.InetSocketAddress
import kotlin.concurrent.thread

object MinecraftRelay {

    private var relay: Relay? = null

    val session = GameSession()
    val moduleManager: ModuleManager
    val configManager: ConfigManagerFileSystem
	val hudManager: HudManager

	val chainCacheFile = File(MyApplication.instance.cacheDir, "chain_cache.json")

	var loaderThread: Thread? = null

    init {
        moduleManager = ModuleManager(session)
		hudManager = HudManager(session)

		// load asynchronously
		loaderThread = thread {
			moduleManager.init()
			registerAdditionalModules(moduleManager)
			MyApplication.instance.getExternalFilesDir("resource_packs")?.also {
				if (!it.exists()) it.mkdirs()
				ModuleResourcePackSpoof.resourcePackProvider = ModuleResourcePackSpoof.FileSystemResourcePackProvider(it)
			}

			if (Settings.enableCommandManager.getValue(MyApplication.instance)) {
				// command manager will register listener itself
				val commandManager = CommandManager(session)
				commandManager.init(moduleManager)
				MyApplication.instance.getExternalFilesDir("downloaded_worlds")?.also {
					commandManager.registerCommand(CommandDownloadWorld(session.eventManager, it))
				}
			}

			// clean-up
			loaderThread = null
		}

        configManager = ConfigManagerFileSystem(MyApplication.instance.getExternalFilesDir("configs")!!, ".json").also {
			it.addSection(ConfigSectionModule(moduleManager))
			it.addSection(ConfigSectionShortcut(MyApplication.overlayManager))
			it.addSection(hudManager)
		}
    }

    private fun registerAdditionalModules(moduleManager: ModuleManager) {
		moduleManager.registerModule(ModuleESP())
	}

    private fun constructRelay(): Relay {
        return Relay(object : MinecraftRelayListener {
            override fun onSessionCreation(session: MinecraftRelaySession): InetSocketAddress {
                // add listeners
                session.listeners.add(RelayListenerNetworkSettings(session))
                session.listeners.add(RelayListenerAutoCodec(session))
                this@MinecraftRelay.session.netSession = session
                session.listeners.add(this@MinecraftRelay.session)

                val sessionEncryptor = if (Settings.offlineSessionEncryption.getValue(MyApplication.instance) && AccountManager.currentAccount == null) {
					RelayListenerEncryptedSession()
				} else {
					AccountManager.currentAccount?.let { account ->
						logInfo("logged in as ${account.remark}")
						RelayListenerXboxLogin({
							account.refresh()
						}, account.platform).also {
							it.chainCache = XboxChainCacheFileSystem(chainCacheFile, account.remark)
						}
					}
				}
                sessionEncryptor?.let {
                    it.session = session
                    session.listeners.add(it)
                }

                // resolve original ip and pass to relay client
                val address = session.peer.channel.config().getOption(NativeRakConfig.RAK_NATIVE_TARGET_ADDRESS)
                logInfo("SessionCreation $address")
				return address
            }
        })
    }

	fun updateReliability() {
		relay?.optionReliability = if (Settings.enableRakReliability.getValue(MyApplication.instance))
			RakReliability.RELIABLE_ORDERED else RakReliability.RELIABLE
	}

	fun announceRelayUp() {
		if (relay == null) {
			relay = constructRelay()
			updateReliability()
		}
		loaderThread?.join()
		if (!relay!!.isRunning) {
			relay!!.bind(InetSocketAddress("0.0.0.0", 1337))
			logInfo("relay started")
		}
	}

	class Relay(listener: MinecraftRelayListener) : dev.sora.relay.MinecraftRelay(listener) {

		override fun channelFactory(): ChannelFactory<out ServerChannel> {
			return ChannelFactory {
				NativeRakServerChannel()
			}
		}
	}
}
