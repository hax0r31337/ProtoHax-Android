package dev.sora.protohax.relay

import android.util.Log
import com.github.megatronking.netbare.NetBareUtils
import com.github.megatronking.netbare.proxy.UdpProxyServerForwarder
import com.google.gson.JsonParser
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.v560.Bedrock_v560
import dev.sora.protohax.App
import dev.sora.protohax.ContextUtils.readStringOrDefault
import dev.sora.protohax.ContextUtils.writeString
import dev.sora.protohax.MainActivity
import dev.sora.relay.RakNetRelay
import dev.sora.relay.RakNetRelayListener
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import dev.sora.relay.cheat.command.CommandManager
import dev.sora.relay.cheat.module.ModuleManager
import dev.sora.relay.game.GameSession
import dev.sora.relay.session.RakNetRelaySessionListenerMicrosoft
import dev.sora.relay.utils.HttpUtils
import io.netty.util.internal.logging.InternalLoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.random.Random

object MinecraftRelay {

    private var firstStart = true
    private var relay: RakNetRelay? = null

    private val session = GameSession()

    init {
        val moduleManager = ModuleManager(session)
        moduleManager.init()

        val commandManager = CommandManager(session)
        commandManager.init(moduleManager)

        session.eventManager.registerListener(commandManager)
    }

    fun listen() {
        InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())

        UdpProxyServerForwarder.targetForwardPort++
        val port = NetBareUtils.convertPort(UdpProxyServerForwarder.targetForwardPort)
        val codec = Bedrock_v560.V560_CODEC
        val relay = RakNetRelay(InetSocketAddress("0.0.0.0", port), packetCodec = codec)
        relay.listener = object : RakNetRelayListener {
            override fun onQuery(address: InetSocketAddress) =
                "MCPE;RakNet Relay;${codec.protocolVersion};${codec.minecraftVersion};0;10;${relay.server.guid};Bedrock level;Survival;1;$port;$port;".toByteArray()

            override fun onSessionCreation(serverSession: RakNetServerSession): InetSocketAddress {
                val originalAddr = UdpProxyServerForwarder.lastForwardAddr
                return InetSocketAddress(NetBareUtils.convertIp(originalAddr.first), originalAddr.second.toInt()).also {
                    Log.i("ProtoHax", "SessionCreation: $it")
                }
            }

            override fun onPrepareClientConnection(address: InetSocketAddress): RakNetRelaySessionListener {
                Log.i("ProtoHax", "PrepareClientConnection $address")
                UdpProxyServerForwarder.addWhitelist(NetBareUtils.convertIp("10.1.10.1"), address.port.toShort())
                return RakNetRelaySessionListener()
            }

            override fun onSession(session: RakNetRelaySession) {
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
        if (this.firstStart) {
            this.firstStart = false
            doFirstStartPrepare()
            return
        }
        this.relay = relay
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

    private fun doFirstStartPrepare() {
        thread {
            val pingBuf = ByteBuffer.allocate(33).apply {
                put(0x01)
                putLong(System.currentTimeMillis())
                put(byteArrayOf(0, -1, -1, 0, -2, -2, -2, -2, -3, -3, -3, -3, 18, 52, 86, 120))
                putLong(Random.Default.nextLong())
            }.array()
            val packet = DatagramPacket(pingBuf, pingBuf.size, InetSocketAddress("10.1.10.1", NetBareUtils.convertPort(UdpProxyServerForwarder.targetForwardPort)))
            val socket = DatagramSocket()
            socket.send(packet)
            Thread.sleep(50L)
            socket.close()
        }
        Thread.sleep(60L)

        close()
        listen()
    }

    fun close() {
        UdpProxyServerForwarder.cleanupCaches()
        relay?.server?.close(true)
        relay = null
    }
}