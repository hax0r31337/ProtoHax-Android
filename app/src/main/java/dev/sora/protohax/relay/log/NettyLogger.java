package dev.sora.protohax.relay.log;

import android.util.Log;

import dev.sora.protohax.BuildConfig;
import io.netty.util.internal.logging.AbstractInternalLogger;

public class NettyLogger extends AbstractInternalLogger {

    private static final String TAG = "ProtoHax";

    protected NettyLogger(String name) {
        super(name);
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void trace(String s) {

    }

    @Override
    public void trace(String s, Object o) {

    }

    @Override
    public void trace(String s, Object o, Object o1) {

    }

    @Override
    public void trace(String s, Object... objects) {

    }

    @Override
    public void trace(String s, Throwable throwable) {

    }

    @Override
    public boolean isDebugEnabled() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void debug(String s) {
        Log.d(TAG, s);
    }

    @Override
    public void debug(String s, Object o) {
        Log.d(TAG, s + ", " + o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        Log.d(TAG, s + ", " + o + ", " + o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        Log.d(TAG, s);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        Log.d(TAG, s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String s) {
        Log.i(TAG, s);
    }

    @Override
    public void info(String s, Object o) {
        Log.i(TAG, s + ", " + o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        Log.i(TAG, s + ", " + o + ", " + o1);
    }

    @Override
    public void info(String s, Object... objects) {
        Log.i(TAG, s);
    }

    @Override
    public void info(String s, Throwable throwable) {
        Log.i(TAG, s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String s) {
        Log.w(TAG, s);
    }

    @Override
    public void warn(String s, Object o) {
        Log.w(TAG, s + ", " + o);
    }

    @Override
    public void warn(String s, Object... objects) {
        Log.w(TAG, s);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        Log.w(TAG, s + ", " + o + ", " + o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        Log.w(TAG, s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String s) {
        Log.e(TAG, s);
    }

    @Override
    public void error(String s, Object o) {
        Log.e(TAG, s + ", " + o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        Log.e(TAG, s + ", " + o + ", " + o1);
    }

    @Override
    public void error(String s, Object... objects) {
        Log.e(TAG, s);
    }

    @Override
    public void error(String s, Throwable throwable) {
        Log.e(TAG, s, throwable);
    }
}
