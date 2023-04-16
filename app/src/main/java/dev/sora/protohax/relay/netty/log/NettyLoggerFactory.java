package dev.sora.protohax.relay.netty.log;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class NettyLoggerFactory extends InternalLoggerFactory {
    @Override
    protected InternalLogger newInstance(String s) {
        return new NettyLogger(s);
    }
}
