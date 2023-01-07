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

import com.github.megatronking.netbare.gateway.Request;
import com.github.megatronking.netbare.gateway.Response;
import com.github.megatronking.netbare.gateway.VirtualGateway;
import com.github.megatronking.netbare.ip.Protocol;
import com.github.megatronking.netbare.net.Session;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The main virtual gateway used in proxy servers, it wraps the actual virtual gateway. We use this
 * class to do some internal verifications.
 *
 * @author Megatron King
 * @since 2018-11-17 23:10
 */
public final class NetBareVirtualGateway extends VirtualGateway {

    /**
     * Policy is indeterminate, we should resolve the policy before process data.
     */
    private static final int POLICY_INDETERMINATE = 0;

    /**
     * This policy allows data flow to configured virtual gateway.
     */
    private static final int POLICY_ALLOWED = 1;

    /**
     * This policy doesn't allow data flow to configured virtual gateway.
     */
    private static final int POLICY_DISALLOWED = 2;

    private final VirtualGateway mGateway;
    private final Session mSession;
    private final NetBareXLog mLog;

    private int mPolicy;

    private boolean mRequestFinished;
    private boolean mResponseFinished;

    public NetBareVirtualGateway(Session session, Request request, Response response) {
        super(session, request, response);
        mGateway = NetBare.get().getGatewayFactory().create(session, request, response);
        mSession = session;
        mLog = new NetBareXLog(session);

//        if (session.uid == Process.myUid()) {
//            // Exclude the app itself.
//            mLog.w("Exclude an app-self connection!");
//            mPolicy = POLICY_DISALLOWED;
//        } else {
            mPolicy = POLICY_INDETERMINATE;
//        }
    }

    @Override
    public void onRequest(ByteBuffer buffer) throws IOException {
        if (mRequestFinished) {
            mLog.w("Drop a buffer due to request has finished.");
            return;
        }
        resolvePolicyIfNecessary(buffer);
        if (mPolicy == POLICY_ALLOWED) {
            mGateway.onRequest(buffer);
        } else if (mPolicy == POLICY_DISALLOWED) {
            super.onRequest(buffer);
        }
    }

    @Override
    public void onResponse(ByteBuffer buffer) throws IOException {
        if (mResponseFinished) {
            mLog.w("Drop a buffer due to response has finished.");
            return;
        }
        resolvePolicyIfNecessary(buffer);
        if (mPolicy == POLICY_ALLOWED) {
            mGateway.onResponse(buffer);
        } else if (mPolicy == POLICY_DISALLOWED) {
            super.onResponse(buffer);
        }
    }

    @Override
    public void onRequestFinished() {
        if (mRequestFinished) {
            return;
        }
//        mLog.i("Gateway request finished!");
        mRequestFinished = true;
        if (mPolicy == POLICY_ALLOWED) {
            mGateway.onRequestFinished();
        } else if (mPolicy == POLICY_DISALLOWED) {
            super.onRequestFinished();
        }
    }

    @Override
    public void onResponseFinished() {
        if (mResponseFinished) {
            return;
        }
//        mLog.i("Gateway response finished!");
        mResponseFinished = true;
        if (mPolicy == POLICY_ALLOWED) {
            mGateway.onResponseFinished();
        } else if (mPolicy == POLICY_DISALLOWED) {
            super.onResponseFinished();
        }
    }

    private void resolvePolicyIfNecessary(ByteBuffer buffer) {
        if (mPolicy != POLICY_INDETERMINATE) {
            // Resolved.
            return;
        }
        if (!buffer.hasRemaining()) {
            // Invalid buffer remaining, do nothing.
            return;
        }
        if (mSession.protocol != Protocol.TCP) {
            mPolicy = POLICY_ALLOWED;
            return;
        }

        // Now we verify the TCP protocol host
        mPolicy = POLICY_ALLOWED;
    }
}
