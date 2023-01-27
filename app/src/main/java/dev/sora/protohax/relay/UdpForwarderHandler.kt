package dev.sora.protohax.relay

import android.util.Log
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.relay.service.ServiceListener
import libmitm.EstablishHandler
import libmitm.Redirector

object UdpForwarderHandler : Redirector, EstablishHandler, ServiceListener {

    val originalIpMap = mutableMapOf<Int, Pair<String, Int>>()

    init {
        AppService.addListener(this)
    }

    /**
     * if the return is empty, the destination will not be modified
     */
    override fun redirect(src: String, srcPort: Long, dst: String, dstPort: Long): String {
        if (dstPort.toInt() == 53 || src == "255.255.255.255") return ""
        return if (src.contains(":")) {
            "[$src]:${MinecraftRelay.listenPort}"
        } else {
            "$src:${MinecraftRelay.listenPort}"
        }
    }

    private fun cleanup() {
        originalIpMap.clear()
    }

    override fun onServiceStarted() {
        cleanup()
    }

    override fun onServiceStopped() {
        cleanup()
    }

    override fun handle(localIp: String, targetIp: String) {
        Log.i("ProtoHax", "$localIp -> $targetIp")
        val idx = targetIp.lastIndexOf(':')
        originalIpMap[localIp.substring(localIp.lastIndexOf(':')+1).toInt()] =
            targetIp.substring(0, idx) to targetIp.substring(idx+1).toInt()
    }
}