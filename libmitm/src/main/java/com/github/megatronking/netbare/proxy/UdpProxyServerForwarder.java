/*  NetBare - An android network capture and injection library.
 *  Copyright (C) 2018-2019 Megatron King
 *  Copyright (C) 2018-2019 GuoShi
 *
 *  NetBare is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Found-
 *  ation, either version 3 of the License, or (at your option) any later version.
 *
 *  NetBare is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with NetBare.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.megatronking.netbare.proxy;

import android.net.VpnService;
import android.util.Pair;

import com.github.megatronking.netbare.NetBareLog;
import com.github.megatronking.netbare.NetBareUtils;
import com.github.megatronking.netbare.ip.IpHeader;
import com.github.megatronking.netbare.ip.Protocol;
import com.github.megatronking.netbare.ip.UdpHeader;
import com.github.megatronking.netbare.net.SessionProvider;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Unlike TCP proxy server, UDP doesn't need handshake, we can forward packets to it directly.
 *
 * @author Megatron King
 * @since 2018-10-09 01:30
 */
public final class UdpProxyServerForwarder implements ProxyServerForwarder {

    private static final int TARGET_FORWARD_IP = NetBareUtils.convertIp("10.1.10.1");
//    private static final int TARGET_FORWARD_IP = NetBareUtils.convertIp("192.168.2.103");
    public static short targetForwardPort = 19132;

    public static Pair<Integer, Short> lastForwardAddr;

    private final SessionProvider mSessionProvider;
    private final UdpProxyServer mProxyServer;

    public static void cleanupCaches() {
        lastForwardAddr = null;
    }

    public UdpProxyServerForwarder(VpnService vpnService, int mtu)
            throws IOException {
        this.mSessionProvider = new SessionProvider();
        this.mProxyServer = new UdpProxyServer(vpnService, mtu);
        this.mProxyServer.setSessionProvider(mSessionProvider);
    }

    public UdpProxyServer getProxyServer() {
        return mProxyServer;
    }

    @Override
    public void prepare() {
        this.mProxyServer.start();
    }

    @Override
    public void forward(byte[] packet, int len, OutputStream output) {
        IpHeader ipHeader = new IpHeader(packet, 0);
        UdpHeader udpHeader = new UdpHeader(ipHeader, packet, ipHeader.getHeaderLength());

        // Src IP & Port
        int localIp = ipHeader.getSourceIp();
        short localPort = udpHeader.getSourcePort();

        // Dest IP & Port
        int originalIp = ipHeader.getDestinationIp();
        short originalPort = udpHeader.getDestinationPort();
        ipHeader.setDestinationIp(TARGET_FORWARD_IP);
        udpHeader.setDestinationPort(targetForwardPort);
        lastForwardAddr = new Pair<>(originalIp, originalPort);

        NetBareLog.v("ip: %s:%d -> %s:%d", NetBareUtils.convertIp(localIp),
                NetBareUtils.convertPort(localPort), NetBareUtils.convertIp(originalIp),
                NetBareUtils.convertPort(originalPort));

        mSessionProvider.ensureQuery(Protocol.UDP, localPort, originalPort, originalIp);

        try {
            mProxyServer.send(udpHeader, output, TARGET_FORWARD_IP, targetForwardPort, originalIp, originalPort, false);
        } catch (IOException e) {
            NetBareLog.e(e.getMessage());
        }
    }

    @Override
    public void release() {
        this.mProxyServer.stop();
    }

}
