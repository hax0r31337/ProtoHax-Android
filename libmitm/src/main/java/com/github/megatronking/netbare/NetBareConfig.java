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
package com.github.megatronking.netbare;

import android.app.PendingIntent;

import androidx.annotation.NonNull;

import com.github.megatronking.netbare.gateway.VirtualGatewayFactory;
import com.github.megatronking.netbare.ip.IpAddress;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * The configuration class for NetBare. Use {@link Builder} to construct an instance.
 *
 * @author Megatron King
 * @since 2018-10-07 11:20
 */
public final class NetBareConfig {

    String session;
    PendingIntent configureIntent;
    int mtu;
    IpAddress address;
    Set<IpAddress> routes;
    Set<String> dnsServers;
    Set<String> allowedApplications;
    Set<String> disallowedApplications;
    VirtualGatewayFactory gatewayFactory;

    private NetBareConfig() {
    }

    public Set<String> getAllowedApplications() {
        return allowedApplications;
    }

    /**
     * Create a new builder based on the current.
     *
     * @return A new builder instance.
     */
    public Builder newBuilder() {
        Builder builder = new Builder();
        builder.mConfig = this;
        return builder;
    }

    /**
     * Create a default config.
     *
     * @return A NetBare config instance.
     */
    public static NetBareConfig defaultConfig() {
        return new Builder()
                .setMtu(4096)
                .setAddress(new IpAddress("10.1.10.1", 32))
                .setSession("NetBare")
                .addRoute(new IpAddress("0.0.0.0", 0))
                .build();
    }

    /**
     * Helper class to createServerEngine a VPN Service.
     */
    public static class Builder {

        private NetBareConfig mConfig;

        public Builder() {
            this.mConfig = new NetBareConfig();
            this.mConfig.routes = new HashSet<>();
            this.mConfig.dnsServers = new HashSet<>();
            this.mConfig.allowedApplications = new HashSet<>();
            this.mConfig.disallowedApplications = new HashSet<>();
        }

        /**
         * Set the name of this session. It will be displayed in system-managed dialogs and
         * notifications. This is recommended not required.
         *
         * @param session Session name.
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder setSession(@NonNull String session) {
            mConfig.session = session;
            return this;
        }

        /**
         * Set the {@link PendingIntent} to an activity for users to configure the VPN connection.
         * If it is not set, the button to configure will not be shown in system-managed dialogs.
         *
         * @param intent An Activity intent.
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder setConfigureIntent(@NonNull PendingIntent intent) {
            mConfig.configureIntent = intent;
            return this;
        }

        /**
         * Set the maximum transmission unit (MTU) of the VPN interface. If it is not set, the
         * default value in the operating system will be used.
         *
         * @param mtu Maximum transmission unit (MTU).
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder setMtu(int mtu) {
            mConfig.mtu = mtu;
            return this;
        }

        /**
         * Convenience method to add a network address to the VPN interface using a numeric address
         * string. See {@link InetAddress} for the definitions of numeric address formats.
         *
         * Adding an address implicitly allows traffic from that address family (i.e., IPv4 or IPv6)
         * to be routed over the VPN.
         *
         * @param address IPv4 or IPv6 address.
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder setAddress(@NonNull IpAddress address) {
            mConfig.address = address;
            return this;
        }

        /**
         * Add a network route to the VPN interface. Both IPv4 and IPv6 routes are supported.
         *
         * Adding a route implicitly allows traffic from that address family (i.e., IPv4 or IPv6)
         * to be routed over the VPN.
         *
         * @param address IPv4 or IPv6 address.
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder addRoute(@NonNull IpAddress address) {
            mConfig.routes.add(address);
            return this;
        }

        /**
         * Add a DNS server to the VPN connection. Both IPv4 and IPv6 addresses are supported.
         * If none is set, the DNS servers of the default network will be used.
         *
         * Adding a server implicitly allows traffic from that address family (i.e., IPv4 or IPv6)
         * to be routed over the VPN.
         *
         * @param address IPv4 or IPv6 address.
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder addDnsServer(@NonNull String address) {
            mConfig.dnsServers.add(address);
            return this;
        }

        /**
         * Adds an application that's allowed to access the VPN connection.
         *
         * If this method is called at least once, only applications added through this method (and
         * no others) are allowed access. Else (if this method is never called), all applications
         * are allowed by default.  If some applications are added, other, un-added applications
         * will use networking as if the VPN wasn't running.
         *
         * @param packageName The full name (e.g.: "com.google.apps.contacts") of an application.
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder addAllowedApplication(@NonNull String packageName) {
            mConfig.allowedApplications.add(packageName);
            return this;
        }

        /**
         * Adds an application that's denied access to the VPN connection.
         *
         * By default, all applications are allowed access, except for those denied through this
         * method.  Denied applications will use networking as if the VPN wasn't running.
         *
         * @param packageName The full name (e.g.: "com.google.apps.contacts") of an application.
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder addDisallowedApplication(@NonNull String packageName) {
            mConfig.disallowedApplications.add(packageName);
            return this;
        }

        /**
         * Set the factory of gateway, the gateway will handle some intercepted actions before the
         * server and client received the final data.
         *
         * @param gatewayFactory A factory of gateway.
         * @return this {@link Builder} object to facilitate chaining method calls.
         */
        public Builder setVirtualGatewayFactory(VirtualGatewayFactory gatewayFactory) {
            mConfig.gatewayFactory = gatewayFactory;
            return this;
        }

        /**
         * Create the instance of {@link NetBareConfig}.
         *
         * @return The instance of {@link NetBareConfig}.
         */
        public NetBareConfig build() {
            return mConfig;
        }

    }
}
