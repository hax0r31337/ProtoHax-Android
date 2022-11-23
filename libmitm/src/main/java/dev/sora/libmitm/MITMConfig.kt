package dev.sora.libmitm

import com.github.megatronking.netbare.gateway.DefaultVirtualGatewayFactory
import com.github.megatronking.netbare.gateway.VirtualGatewayFactory
import com.github.megatronking.netbare.ip.IpAddress

class MITMConfig(val mtu: Int = 4096, val dumpUid: Boolean = false,
                 val address: IpAddress = IpAddress("10.1.10.1", 32),
                 val session: String? = "LibMITM", val routes: Array<IpAddress> = arrayOf(IpAddress("0.0.0.0", 0)),
                 val dnsServers: Array<String> = emptyArray(), val allowedApps: Array<String> = emptyArray(),
                 val blockedApps: Array<String> = emptyArray(), val virtualGatewayFactory: VirtualGatewayFactory? = DefaultVirtualGatewayFactory.create())