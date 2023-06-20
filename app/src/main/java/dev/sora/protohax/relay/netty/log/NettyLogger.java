package dev.sora.protohax.relay.netty.log;

import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;

import dev.sora.protohax.BuildConfig;
import io.netty.util.internal.logging.AbstractInternalLogger;

public class NettyLogger extends AbstractInternalLogger {

    public static final String TAG = "ProtoHax";

    private static final LinkedList<String> logs = new LinkedList<>();

    public static String getLogs() {
        final StringBuilder sb = new StringBuilder();

        for (String log : logs) {
            sb.append(log);
            sb.append('\n');
        }

        return sb.toString();
    }

    public static void clearLogs() {
        logs.clear();
    }

    private static void log(final String log) {
        if (log.contains("\n")) {
            logs.addAll(Arrays.asList(log.split("\n")));
        } else {
            logs.add(log);
        }

        while (logs.size() > 500) {
            logs.pop();
        }
    }

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
        log(s);
        Log.d(TAG, s);
    }

    @Override
    public void debug(String s, Object o) {
        s = s + ", " + o;
        log(s);
        Log.d(TAG, s);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        s = s + ", " + o + ", " + o1;
        log(s);
        Log.d(TAG, s);
    }

    @Override
    public void debug(String s, Object... objects) {
        final StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append(", ");
        for (Object o : objects) {
            sb.append(o);
            sb.append(", ");
        }
        sb.setLength(sb.length()-2);
        s = sb.toString();
        log(s);
        Log.d(TAG, s);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        log(s + ": " + throwable);
        Log.d(TAG, s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String s) {
        log(s);
        Log.i(TAG, s);
    }

    @Override
    public void info(String s, Object o) {
        s = s + ", " + o;
        log(s);
        Log.i(TAG, s);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        s = s + ", " + o + ", " + o1;
        log(s);
        Log.i(TAG, s);
    }

    @Override
    public void info(String s, Object... objects) {
        final StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append(", ");
        for (Object o : objects) {
            sb.append(o);
            sb.append(", ");
        }
        sb.setLength(sb.length()-2);
        s = sb.toString();
        log(s);
        Log.i(TAG, s);
    }

    @Override
    public void info(String s, Throwable throwable) {
        log(s + ": " + throwable);
        Log.i(TAG, s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String s) {
        log(s);
        Log.w(TAG, s);
    }

    @Override
    public void warn(String s, Object o) {
        s = s + ", " + o;
        log(s);
        Log.w(TAG, s);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        s = s + ", " + o + ", " + o1;
        log(s);
        Log.w(TAG, s);
    }

    @Override
    public void warn(String s, Object... objects) {
        final StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append(", ");
        for (Object o : objects) {
            sb.append(o);
            sb.append(", ");
        }
        sb.setLength(sb.length()-2);
        s = sb.toString();
        log(s);
        Log.w(TAG, s);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        log(s + ": " + throwable);
        Log.w(TAG, s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String s) {
        log(s);
        Log.e(TAG, s);
    }

    @Override
    public void error(String s, Object o) {
        s = s + ", " + o;
        log(s);
        Log.e(TAG, s);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        s = s + ", " + o + ", " + o1;
        log(s);
        Log.e(TAG, s);
    }

    @Override
    public void error(String s, Object... objects) {
        final StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append(", ");
        for (Object o : objects) {
            sb.append(o);
            sb.append(", ");
        }
        sb.setLength(sb.length()-2);
        s = sb.toString();
        log(s);
        Log.e(TAG, s);
    }

    @Override
    public void error(String s, Throwable throwable) {
        log(s + ": " + throwable);
        Log.e(TAG, s, throwable);
    }
}
