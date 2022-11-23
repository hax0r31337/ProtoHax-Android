package dev.sora.libmitm

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.util.Log
import com.github.megatronking.netbare.ip.IpHeader
import com.github.megatronking.netbare.ip.Protocol
import com.github.megatronking.netbare.proxy.ProxyServerForwarder
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream


class PacketHandler(private val service: VpnService, private val config: MITMConfig) : Thread() {

    private var vpnDescriptor: ParcelFileDescriptor? = null
    private var vpnInput: InputStream? = null
    private var vpnOutput: OutputStream? = null

    private val buffer = ByteArray(config.mtu)

    override fun interrupt() {
        vpnDescriptor?.close()
        vpnInput?.close()
        vpnOutput?.close()

        vpnDescriptor = null
        vpnInput = null
        vpnOutput = null
    }

    override fun run() {
        MITM.running = true

        try {
            val builder = service.Builder()
            builder.setBlocking(true)
            builder.setMtu(config.mtu)
            builder.addAddress(config.address.address, config.address.prefixLength)
            if (config.session != null) {
                builder.setSession(config.session)
            }
            config.routes.forEach {
                builder.addRoute(it.address, it.prefixLength)
            }
            config.dnsServers.forEach {
                builder.addDnsServer(it)
            }
            try {
                config.allowedApps.forEach {
                    builder.addAllowedApplication(it)
                }
                config.blockedApps.forEach {
                    builder.addDisallowedApplication(it)
                }
                if (config.allowedApps.isNotEmpty()) {
                    builder.addAllowedApplication(service.packageName)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("LibMITM", "app strategy", e)
            }

            vpnDescriptor = builder.establish()
            vpnDescriptor?.also {
                val desc = it.fileDescriptor
                vpnInput = FileInputStream(desc)
                vpnOutput = FileOutputStream(desc)

                while (!isInterrupted) {
                    transfer(vpnInput ?: break, vpnOutput ?: break)
                }
            }
        } catch (t: Throwable) {
            Log.e("LibMITM", "vpn thread", t)
        }

        MITM.running = false
    }

    private fun transfer(input: InputStream, output: OutputStream) {
        // The thread would be blocked if there is no outgoing packets from input stream.
        transfer(buffer, input.read(buffer), output)
    }

    private fun transfer(packet: ByteArray, len: Int, output: OutputStream) {
        if (len < IpHeader.MIN_HEADER_LENGTH) {
            Log.w("LibMITM", "Ip header length < " + IpHeader.MIN_HEADER_LENGTH)
            return
        }
        val ipHeader = IpHeader(packet, 0)
        val protocol = Protocol.parse(ipHeader.protocol.toInt())
        if (protocol == Protocol.UDP) {
            Log.i("LibMITM", "udp packet received")
        }
    }
}