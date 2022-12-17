package dev.sora.protohax.relay

import android.util.Log
import com.github.megatronking.netbare.NetBareService
import com.github.megatronking.netbare.NetBareUtils
import com.github.megatronking.netbare.proxy.UdpProxyServerForwarder
import com.google.gson.JsonParser
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.v560.Bedrock_v560
import dev.sora.protohax.App
import dev.sora.protohax.ContextUtils.readStringOrDefault
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
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.HttpUtils
import io.netty.util.internal.logging.InternalLoggerFactory
import java.io.File
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel


object MinecraftRelay {

    private var relay: RakNetRelay? = null

    private val session = GameSession()
    val moduleManager: ModuleManager
    val commandManager: CommandManager
    val configManager: AbstractConfigManager

    init {
        moduleManager = ModuleManager(session)
        moduleManager.init()

        commandManager = CommandManager(session)
        commandManager.init(moduleManager)

        configManager = ConfigManagerFileSystem(App.app.getExternalFilesDir("configs")!!, ".json", moduleManager)

        session.eventManager.registerListener(commandManager)
    }

    fun listen() {
        System.setProperty("io.netty.noUnsafe", "true")
        InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())

        val port = NetBareUtils.convertPort(UdpProxyServerForwarder.targetForwardPort)
        val codec = Bedrock_v560.V560_CODEC
        val relay = RakNetRelay(InetSocketAddress("0.0.0.0", port), packetCodec = codec)
        relay.listener = object : RakNetRelayListener {
            override fun onQuery(address: InetSocketAddress) =
                "MCPE;RakNet Relay;${codec.protocolVersion};${codec.minecraftVersion};0;10;${relay.server.guid};Bedrock level;Survival;1;$port;$port;".toByteArray()

            override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
                Log.i("ProtoHax", "SessionCreation ${serverSession.address.port}")
                try {
                    val server = NetBareService.getInstance()!!.thread!!.udpProxyServer
                    val pair = server.getOriginIP(serverSession.address.port)
                    Log.i("ProtoHax", "EstablishConnection ${pair.first}:${pair.second}")
                    return InetSocketAddress(pair.first, pair.second)
                } catch (t: Throwable) {
                    Log.e("ProtoHax", "estab", t)
                    throw t
                }
            }

            override fun onPrepareClientConnection(clientSocket: DatagramChannel): RakNetRelaySessionListener {
                Log.i("ProtoHax", "PrepareClientConnection")
                NetBareService.getInstance()?.protect(clientSocket.socket())
                return RakNetRelaySessionListener()
            }

            override fun onSession(session: RakNetRelaySession) {
                Log.i("ProtoHax", "PreRelaySessionCreation")
                this@MinecraftRelay.session.netSession = session
                session.listener.childListener.add(this@MinecraftRelay.session)
                val token = App.app.readStringOrDefault(MainActivity.KEY_MICROSOFT_REFRESH_TOKEN, "")
                if (token.isNotEmpty()) {
                    val tokens = getMSAccessToken(token)
                    App.app.writeString(MainActivity.KEY_MICROSOFT_REFRESH_TOKEN, tokens.second)
                    session.listener.childListener.add(RakNetRelaySessionListenerMicrosoft(tokens.first, session))
                }
            }
        }
        relay.bind()
        this.relay = relay
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
        UdpProxyServerForwarder.cleanupCaches()
        relay?.server?.close(true)
        relay = null
        Log.i("ProtoHax", "relay closed")
    }
}