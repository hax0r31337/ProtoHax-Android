package dev.sora.protohax.relay

import android.util.Log
import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.log.NettyLoggerFactory
import dev.sora.protohax.relay.modules.ModuleESP
import dev.sora.protohax.relay.service.UdpForwarderHandler
import dev.sora.relay.MinecraftRelayListener
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.config.ConfigManagerFileSystem
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.MinecraftRelaySession
import dev.sora.relay.session.listener.RelayListenerAutoCodec
import dev.sora.relay.session.listener.RelayListenerMicrosoftLogin
import dev.sora.relay.utils.logInfo
import io.netty.util.internal.logging.InternalLoggerFactory
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.concurrent.thread
import kotlin.random.Random

typealias ProtoHaxMinecraftRelay = dev.sora.relay.MinecraftRelay

object MinecraftRelay {

    private var relay: ProtoHaxMinecraftRelay? = null

    val session = GameSession()
    val moduleManager: ModuleManager
    val commandManager: CommandManager
    val configManager: ConfigManagerFileSystem

    var listenPort: Int = 10000+Random.nextInt(55534)

    init {
        moduleManager = ModuleManager(session)
        moduleManager.init()
        registerAdditionalModules(moduleManager)

		// command manager will register listener itself
        commandManager = CommandManager(session)
        commandManager.init(moduleManager)

        configManager = ConfigManagerFileSystem(MyApplication.instance.getExternalFilesDir("configs")!!, ".json", moduleManager)

        // initialize AccountManager
        AccountManager
    }

    private fun registerAdditionalModules(moduleManager: ModuleManager) {
        moduleManager.registerModule(ModuleESP())
    }

    private fun searchForUsablePort(): Int {
        var port = 10000+Random.nextInt(55534)
        thread {
            // android do not allow network operation on main thread
            val socket = DatagramSocket()
            port = socket.localPort
            socket.close()
        }.join()
        return port
    }

    fun listen() {
        System.setProperty("io.netty.noUnsafe", "true")
        InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())

        listenPort = searchForUsablePort()
		var msLoginSession: RelayListenerMicrosoftLogin? = null
        val relay = ProtoHaxMinecraftRelay(object : MinecraftRelayListener {
			override fun onSessionCreation(session: MinecraftRelaySession): InetSocketAddress {
				// add listeners
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
				val address = session.socketAddress as InetSocketAddress
				Log.i("ProtoHax", "SessionCreation ${address.port}")
				try {
					val pair = UdpForwarderHandler.originalIpMap[address.port]!!
					Log.i("ProtoHax", "EstablishConnection ${pair.first}:${pair.second}")
					return InetSocketAddress(pair.first, pair.second)
				} catch (t: Throwable) {
					Log.e("ProtoHax", "establish", t)
					throw t
				}
			}
		})
		relay.bind(InetSocketAddress("0.0.0.0", listenPort))
        this.relay = relay
        MyApplication.instance.getExternalFilesDir("resource_packs")?.also {
            if (!it.exists()) it.mkdirs()
            ModuleResourcePackSpoof.resourcePackProvider = ModuleResourcePackSpoof.FileSystemResourcePackProvider(it)
        }
        Log.i("ProtoHax", "relay started")
    }

    fun close() {
        relay?.stop()
        relay = null
        Log.i("ProtoHax", "relay closed")
    }
}
