package dev.sora.protohax.relay

import android.util.Log
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

object MinecraftRelay {

    private val relay: Relay

    val session = GameSession()
    val moduleManager: ModuleManager
    val commandManager: CommandManager
    val configManager: ConfigManagerFileSystem

    init {
        moduleManager = ModuleManager(session)
        moduleManager.init()
        registerAdditionalModules(moduleManager)
        MyApplication.instance.getExternalFilesDir("resource_packs")?.also {
            if (!it.exists()) it.mkdirs()
            ModuleResourcePackSpoof.resourcePackProvider = ModuleResourcePackSpoof.FileSystemResourcePackProvider(it)
        }

        // command manager will register listener itself
        commandManager = CommandManager(session)
        commandManager.init(moduleManager)
        MyApplication.instance.getExternalFilesDir("downloaded_worlds")?.also {
            commandManager.registerCommand(CommandDownloadWorld(session.eventManager, it))
        }

        configManager = ConfigManagerFileSystem(MyApplication.instance.getExternalFilesDir("configs")!!, ".json", moduleManager)

        // initialize AccountManager
        AccountManager

		relay = listen()
    }

    private fun registerAdditionalModules(moduleManager: ModuleManager) {
		moduleManager.registerModule(ModuleESP())
	}

    private fun listen(): Relay {
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
                Log.i("ProtoHax", "SessionCreation $address")
				return address
            }
        }).also {
			it.bind(InetSocketAddress("0.0.0.0", 1337))
			Log.i("ProtoHax", "relay started")
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
