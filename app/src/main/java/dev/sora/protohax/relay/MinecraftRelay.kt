package dev.sora.protohax.relay

import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.modules.ModuleESP
import dev.sora.protohax.relay.netty.channel.NativeRakConfig
import dev.sora.protohax.relay.netty.channel.NativeRakServerChannel
import dev.sora.protohax.relay.netty.log.NettyLoggerFactory
import dev.sora.relay.MinecraftRelayListener
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.command.impl.CommandDownloadWorld
import dev.sora.relay.cheat.config.ConfigManagerFileSystem
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.session.listener.RelayListenerAutoCodec
import dev.sora.relay.session.listener.RelayListenerMicrosoftLogin
import dev.sora.relay.session.listener.RelayListenerNetworkSettings
import dev.sora.relay.utils.logInfo
import io.netty.channel.ChannelFactory
import io.netty.channel.ServerChannel
import io.netty.util.internal.logging.InternalLoggerFactory
import java.net.InetSocketAddress
import kotlin.concurrent.thread

object MinecraftRelay {

    private var relay: Relay? = null

    val session = GameSession()
    val moduleManager: ModuleManager
    val commandManager: CommandManager
    val configManager: ConfigManagerFileSystem

	var loaderThread: Thread? = null

    init {
        moduleManager = ModuleManager(session)

        // command manager will register listener itself
        commandManager = CommandManager(session)

		// load asynchronously
		loaderThread = thread {
			moduleManager.init()
			registerAdditionalModules(moduleManager)
			MyApplication.instance.getExternalFilesDir("resource_packs")?.also {
				if (!it.exists()) it.mkdirs()
				ModuleResourcePackSpoof.resourcePackProvider = ModuleResourcePackSpoof.FileSystemResourcePackProvider(it)
			}

			commandManager.init(moduleManager)
			MyApplication.instance.getExternalFilesDir("downloaded_worlds")?.also {
				commandManager.registerCommand(CommandDownloadWorld(session.eventManager, it))
			}

			// clean-up
			loaderThread = null
		}

        configManager = ConfigManagerFileSystem(MyApplication.instance.getExternalFilesDir("configs")!!, ".json", moduleManager)
    }

    private fun registerAdditionalModules(moduleManager: ModuleManager) {
		moduleManager.registerModule(ModuleESP())
	}

    private fun constructRelay(): Relay {
//        System.setProperty("io.netty.noUnsafe", "true")
        InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())

        var msLoginSession: RelayListenerMicrosoftLogin? = null
        return Relay(object : MinecraftRelayListener {
            override fun onSessionCreation(session: MinecraftRelaySession): InetSocketAddress {
                // add listeners
                session.listeners.add(RelayListenerNetworkSettings(session))
                session.listeners.add(RelayListenerAutoCodec(session))
                this@MinecraftRelay.session.netSession = session
                session.listeners.add(this@MinecraftRelay.session)
                if (msLoginSession == null) {
                    msLoginSession = AccountManager.currentAccount?.let {
                        val accessToken = it.refresh()
                        logInfo("logged in as ${it.remark}")
                        RelayListenerMicrosoftLogin(accessToken, it.platform)
                    }
                }
                msLoginSession?.let {
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

	fun announceRelayUp() {
		if (relay == null) {
			relay = constructRelay()
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
