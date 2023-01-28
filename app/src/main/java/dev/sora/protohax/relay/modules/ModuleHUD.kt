package dev.sora.protohax.relay.modules

import android.widget.Toast
import com.nukkitx.protocol.bedrock.packet.AvailableCommandsPacket
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket
import com.nukkitx.protocol.bedrock.packet.PlayerAuthInputPacket
import com.nukkitx.protocol.bedrock.packet.SetTitlePacket
import dev.sora.protohax.relay.gui.RenderLayerView
import dev.sora.protohax.relay.ui.element.Element
import dev.sora.protohax.relay.ui.element.elements.Text
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.cheat.module.impl.player.ModuleScaffold
import dev.sora.relay.game.event.EventPacketInbound
import dev.sora.relay.game.event.EventPacketOutbound
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.utils.TimerUtil
import kotlin.math.sqrt


class ModuleHUD : CheatModule("HUD") {
    private val timerUtil= TimerUtil()

    private val elements = mutableListOf<Element>()
    override fun onEnable() {
        super.onEnable()
        elements.clear()
        elements.add(Text.defaultClient())
    }
    @Listen
    fun onPacketInbound(event: EventPacketInbound) {
        val packet = event.packet
        if (packet is AvailableCommandsPacket) return
        if (packet is MobEquipmentPacket) {
            session.thePlayer.currentItem = packet.item
        }
        if(packet is SetTitlePacket){
            if(!packet.text.contains("ProtoHax")){
                timerUtil.reset()
            }
        }
    }
    @Listen
    fun onRender(event: RenderLayerView.EventRender) {
        event.needRefresh = true
        elements.sortedBy { -it.info.priority }
            .forEach {
                try {
                    it.border = it.drawElement(mc,event.canvas,event.context)
                } catch (ex: Exception) {
                    Toast.makeText(event.context, ex.toString(), Toast.LENGTH_LONG).show()
                }
            }
    }
    @Listen
    fun onPacketOutbound(event: EventPacketOutbound) {
        val packet = event.packet
        if (packet is PlayerAuthInputPacket) {
            if(packet.tick%2!=0L) return
            if(!timerUtil.delay(1500f)) return
            val titlePacket = SetTitlePacket()
            titlePacket.type = SetTitlePacket.Type.ACTIONBAR
            titlePacket.fadeInTime = 0
            titlePacket.fadeOutTime = 0
            titlePacket.stayTime = 2
            val xDist: Double = mc.thePlayer.posX - mc.thePlayer.prevPosX
            val zDist: Double = mc.thePlayer.posZ - mc.thePlayer.prevPosZ
            val moveSpeed = sqrt(xDist * xDist + zDist * zDist) * 2
            val scaffold = session.moduleManager.getModuleByName("Scaffold") as ModuleScaffold?
            if(scaffold!=null && mc.thePlayer.currentItem!=null) {
                titlePacket.text = "§bProtoHax §f| §eSpeed: §f${
                    String.format(
                        "%.2f",
                        moveSpeed * 10
                    )
                } bps${if (scaffold.state && !(mc.thePlayer.currentItem?.isNull)!!) " §f| §eScaffold: §f${mc.thePlayer.currentItem!!.count} blocks" else ""}"
            }else{
                titlePacket.text = "${
                    String.format(
                        "%.2f",
                        moveSpeed * 10
                    )
                } §eBlocks/sec"
            }
            titlePacket.xuid = ""
            titlePacket.platformOnlineId = ""
            session.netSession.inboundPacket(titlePacket)
            return
        }
        if (packet is InventoryTransactionPacket) {
            session.thePlayer.currentItem = packet.itemInHand
        }
    }
}