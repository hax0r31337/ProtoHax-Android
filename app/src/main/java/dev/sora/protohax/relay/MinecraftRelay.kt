package dev.sora.protohax.relay

import android.util.Log
import com.google.gson.JsonParser
import com.nukkitx.network.raknet.RakNetServerSession
import dev.sora.protohax.AppService
import dev.sora.protohax.ContextUtils.readString
import dev.sora.protohax.ContextUtils.writeString
import dev.sora.protohax.MainActivity
import dev.sora.protohax.relay.log.NettyLoggerFactory
import dev.sora.relay.RakNetRelay
import dev.sora.relay.RakNetRelayListener
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.config.AbstractConfigManager
import dev.sora.relay.cheat.config.ConfigManagerFileSystem
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.cheat.module.impl.ModuleResourcePackSpoof
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.RakNetRelaySessionListenerAutoCodec
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.HttpUtils
import dev.sora.relay.utils.logInfo
import io.netty.util.internal.logging.InternalLoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import kotlin.random.Random


object MinecraftRelay {

    private var relay: RakNetRelay? = null

    private val session = GameSession()
    val moduleManager: ModuleManager
    val commandManager: CommandManager
    val configManager: AbstractConfigManager

    var listenPort: Int = 10000+Random.nextInt(55534)

    init {
        moduleManager = ModuleManager(session)
        moduleManager.init()

        commandManager = CommandManager(session)
        commandManager.init(moduleManager)

        configManager = ConfigManagerFileSystem(AppService.instance.getExternalFilesDir("configs")!!, ".json", moduleManager)

        session.eventManager.registerListener(commandManager)
    }

    fun listen() {
        System.setProperty("io.netty.noUnsafe", "true")
        InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())

        listenPort = 10000+Random.nextInt(55534)
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

            override fun onPrepareClientConnection(clientSocket: DatagramChannel): RakNetRelaySessionListener {
                Log.i("ProtoHax", "PrepareClientConnection")
                AppService.instance.protect(clientSocket.socket())
                return super.onPrepareClientConnection(clientSocket)
            }

            override fun onSession(session: RakNetRelaySession) {
                Log.i("ProtoHax", "PreRelaySessionCreation")
                session.listener.childListener.add(RakNetRelaySessionListenerAutoCodec(session))
                this@MinecraftRelay.session.netSession = session
                session.listener.childListener.add(this@MinecraftRelay.session)
                if (msLoginSession == null) {
                    msLoginSession = AppService.instance.readString(MainActivity.KEY_MICROSOFT_REFRESH_TOKEN)?.let {
                        val tokens = getMSAccessToken(it)
                        AppService.instance.writeString(MainActivity.KEY_MICROSOFT_REFRESH_TOKEN, tokens.second)
                        logInfo("microsoft access token successfully fetched")
                        RakNetRelaySessionListenerMicrosoft(tokens.first, RakNetRelaySessionListenerMicrosoft.DEVICE_NINTENDO)
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
        AppService.instance.getExternalFilesDir("resource_packs")?.also {
            if (!it.exists()) it.mkdirs()
            ModuleResourcePackSpoof.resourcePackProvider = ModuleResourcePackSpoof.FileSystemResourcePackProvider(it)
        }
        Log.i("ProtoHax", "relay started")
    }

    /**
     * refreshes token
     * @return Pair(accessToken, newRefreshToken)
     */
    private fun getMSAccessToken(refreshToken: String): Pair<String, String> {
        val body = JsonParser.parseReader(
            HttpUtils.make("https://login.live.com/oauth20_token.srf", "POST",
                "client_id=00000000441cc96b&scope=service::user.auth.xboxlive.com::MBI_SSL&grant_type=refresh_token&redirect_uri=https://login.live.com/oauth20_desktop.srf&refresh_token=${refreshToken}",
                mapOf("Content-Type" to "application/x-www-form-urlencoded")).inputStream.reader(Charsets.UTF_8)).asJsonObject
        return body.get("access_token").asString to body.get("refresh_token").asString
    }

    fun close() {
        relay?.server?.close(true)
        relay = null
        Log.i("ProtoHax", "relay closed")
    }
}