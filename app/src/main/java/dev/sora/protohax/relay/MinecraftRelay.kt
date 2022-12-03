package dev.sora.protohax.relay

import com.github.megatronking.netbare.proxy.UdpProxyServerForwarder
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.protocol.bedrock.BedrockPacket
import com.nukkitx.protocol.bedrock.data.Ability
import com.nukkitx.protocol.bedrock.data.AbilityLayer
import com.nukkitx.protocol.bedrock.data.AttributeData
import com.nukkitx.protocol.bedrock.data.PlayerPermission
import com.nukkitx.protocol.bedrock.data.command.CommandPermission
import com.nukkitx.protocol.bedrock.packet.*
import com.nukkitx.protocol.bedrock.v557.Bedrock_v557
import dev.sora.relay.RakNetRelay
import dev.sora.relay.RakNetRelayListener
import dev.sora.relay.RakNetRelaySession
import dev.sora.relay.RakNetRelaySessionListener
import io.netty.util.internal.logging.InternalLoggerFactory
import java.net.InetSocketAddress

object MinecraftRelay {

    private var relay: RakNetRelay? = null

    fun listen() {
        InternalLoggerFactory.setDefaultFactory(NettyLoggerFactory())

        val relay = RakNetRelay(InetSocketAddress("0.0.0.0", UdpProxyServerForwarder.TARGET_FORWARD_PORT.toInt()), packetCodec = Bedrock_v557.V557_CODEC)
        relay.listener = object : RakNetRelayListener {
            override fun onQuery(address: InetSocketAddress) =
                "MCPE;RakNet Relay;557;1.19.20;0;10;${relay.server.guid};Bedrock level;Survival;1;19132;19132;".toByteArray()

            override fun onSessionCreation(serverSession: RakNetServerSession) =
                InetSocketAddress("192.168.2.103", 19132)

            override fun onSession(session: RakNetRelaySession) {
                var entityId = 0L
                session.listener = object : RakNetRelaySessionListener(session = session) {
                    override fun onPacketInbound(packet: BedrockPacket): Boolean {
                        if (packet is StartGamePacket) {
                            entityId = packet.runtimeEntityId
                        } else if (packet is UpdateAbilitiesPacket) {
                            session.inboundPacket(UpdateAbilitiesPacket().apply {
                                uniqueEntityId = entityId
                                playerPermission = PlayerPermission.OPERATOR
                                commandPermission = CommandPermission.OPERATOR
                                abilityLayers.add(AbilityLayer().apply {
                                    layerType = AbilityLayer.Type.BASE
                                    abilitiesSet.addAll(Ability.values())
                                    abilityValues.addAll(arrayOf(Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES, Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS, Ability.OPERATOR_COMMANDS, Ability.MAY_FLY, Ability.FLY_SPEED, Ability.WALK_SPEED))
                                    walkSpeed = 0.1f
                                    flySpeed = 0.25f
                                })
                            })
                            return false
                        } else if (packet is UpdateAttributesPacket) {
                            packet.attributes.add(AttributeData("minecraft:movement", 0.0f, Float.MAX_VALUE, 0.5f, 0.1f))
                        }
                        return super.onPacketInbound(packet)
                    }

                    override fun onPacketOutbound(packet: BedrockPacket): Boolean {
                        if (packet is RequestAbilityPacket && packet.ability == Ability.FLYING) return false
                        return super.onPacketOutbound(packet)
                    }
                }
            }
        }
        relay.bind()
        this.relay = relay
    }

    fun close() {
        relay?.server?.close(true)
    }
}