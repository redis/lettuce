package com.lambdaworks.redis;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>>
 * @since 18.05.14 13:26
 */
public class LettuceStrings {
    /**
     * <p>
     * Checks if a CharSequence is empty ("") or null.
     * </p>
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * <p>
     * Checks if a CharSequence is not empty ("") and not null.
     * </p>
     * 
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }
}
