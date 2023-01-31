package dev.sora.protohax.relay.service

import android.util.Log
import dev.sora.protohax.relay.MinecraftRelay
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
        if (dstPort.toInt() == 53 || dst == "255.255.255.255") return ""
        return if (src.contains(":")) {
            "127.0.0.1:${MinecraftRelay.listenPort}"
        } else {
            "127.0.0.1:${MinecraftRelay.listenPort}"
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
        if (targetIp.endsWith(":53")) return
        Log.i("ProtoHax", "$localIp -> $targetIp")
        val idx = targetIp.lastIndexOf(':')
        originalIpMap[localIp.substring(localIp.lastIndexOf(':')+1).toInt()] =
            targetIp.substring(0, idx) to targetIp.substring(idx+1).toInt()
    }
}