package com.lambdaworks.redis;

/**
 * Helper for {@link String} checks.
 * @author Mark Paluch
 * @since 3.0
 */
public class LettuceStrings {

    /**
     * Utility constructor.
     */
    private LettuceStrings() {

    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     * 
     * @param cs the char sequence
     * @return true if empty
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if a CharSequence is not empty ("") and not null.
     * 
     * @param cs the char sequence
     * @return true if not empty
     * 
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }
}
