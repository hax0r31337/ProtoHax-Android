package dev.sora.protohax.relay

import android.util.Log
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.network.raknet.RakNetSession
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializer
import com.nukkitx.protocol.bedrock.wrapper.BedrockWrapperSerializerV9_10
import dev.sora.protohax.MyApplication
import dev.sora.protohax.relay.log.NettyLoggerFactory
import dev.sora.protohax.relay.modules.ModuleESP
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.relay.service.UdpForwarderHandler
import dev.sora.relay.RakNetRelay
import dev.sora.relay.RakNetRelayListener
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.config.ConfigManagerFileSystem
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.RakNetRelaySessionListenerAutoCodec
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.logInfo
import io.netty.util.internal.logging.InternalLoggerFactory
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import kotlin.concurrent.thread
import kotlin.random.Random


object MinecraftRelay {

    private var relay: RakNetRelay? = null

    val session = GameSession()
    val moduleManager: ModuleManager
    val commandManager: CommandManager
    val configManager: ConfigManagerFileSystem

    var listenPort: Int = 10000+Random.nextInt(55534)

    init {
        moduleManager = ModuleManager(session)
        moduleManager.init()
        registerAdditionalModules(moduleManager)

        commandManager = CommandManager(session)
        commandManager.init(moduleManager)

        configManager = ConfigManagerFileSystem(MyApplication.instance.getExternalFilesDir("configs")!!, ".json", moduleManager)

        session.eventManager.registerListener(commandManager)

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
        val relay = RakNetRelay(InetSocketAddress("0.0.0.0", listenPort))
        var msLoginSession: RakNetRelaySessionListenerMicrosoft? = null
        relay.listener = object : RakNetRelayListener {
            override fun onQuery(address: InetSocketAddress) =
                "MCPE;RakNet Relay;560;1.19.50;0;10;${relay.server.guid};Bedrock level;Survival;1;$listenPort;$listenPort;".toByteArray()

            override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
                Log.i("ProtoHax", "SessionCreation ${serverSession.address.port}")
                try {
                    val pair = UdpForwarderHandler.originalIpMap.get(serverSession.address.port)!!
                    Log.i("ProtoHax", "EstablishConnection ${pair.first}:${pair.second}")
                    return InetSocketAddress(pair.first, pair.second)
                } catch (t: Throwable) {
                    Log.e("ProtoHax", "estab", t)
                    throw t
                }
            }

            override fun onSession(session: RakNetRelaySession) {
                Log.i("ProtoHax", "PreRelaySessionCreation")
                session.listener.childListener.add(RakNetRelaySessionListenerAutoCodec(session))
                this@MinecraftRelay.session.netSession = session
                session.listener.childListener.add(this@MinecraftRelay.session)
                if (msLoginSession == null) {
                    msLoginSession = AccountManager.currentAccount?.let {
                        val accessToken = it.refresh()
                        logInfo("logged in as ${it.remark}")
                        RakNetRelaySessionListenerMicrosoft(accessToken, it.platform)
                    }
                }
                msLoginSession?.let {
                    it.session = session
                    session.listener.childListener.add(it)
                }
            }
        }
        relay.bind()
        this.relay = relay
        MyApplication.instance.getExternalFilesDir("resource_packs")?.also {
            if (!it.exists()) it.mkdirs()
            ModuleResourcePackSpoof.resourcePackProvider = ModuleResourcePackSpoof.FileSystemResourcePackProvider(it)
        }
        Log.i("ProtoHax", "relay started")
    }

    fun close() {
        relay?.server?.close(true)
        relay = null
        Log.i("ProtoHax", "relay closed")
    }
}