package com.ishland.c2me.opts.accel.opencl.common.util;

import java.util.function.Supplier;

/**
 * Minimal replacement for the flowsched library's assertion helper.
 */
public class FlowschedAssertions {
    public static void assertTrue(boolean b) {
        if (!b) throw new AssertionError("Assertion failed");
    }

    public static void assertTrue(boolean b, String msg) {
        if (!b) throw new AssertionError(msg);
    }

    public static void assertTrue(boolean b, Supplier<String> msg) {
        if (!b) throw new AssertionError(msg.get());
    }

    public static <T> T assertNotNull(T obj) {
        if (obj == null) throw new AssertionError("Assertion failed: expected non-null");
        return obj;
    }

    public static void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError(msg);
    }

    public static void assertTrue(boolean b, String fmt, Object... args) {
        if (!b) throw new AssertionError(String.format(fmt, args));
    }
}
