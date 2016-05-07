package com.lambdaworks.redis;

import static com.lambdaworks.redis.internal.LettuceClassUtils.isPresent;

/**
 * Utility to determine which Java runtime is used.
 * 
 * @author Mark Paluch
 */
public class JavaRuntime {

    /**
     * Constant whether the current JDK is Java 8 or higher.
     */
    public final static boolean AT_LEAST_JDK_8 = isPresent("java.lang.FunctionalInterface");

}
