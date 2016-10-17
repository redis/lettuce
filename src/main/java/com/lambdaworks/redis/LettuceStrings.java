package com.lambdaworks.redis;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

import com.lambdaworks.codec.Base16;

/**
 * Helper for {@link String} checks. This class is part of the internal API and may change without further notice.
 * 
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

    /**
     * Convert double to string. If double is infinite, returns positive/negative infinity {@code +inf} and {@code -inf}.
     * 
     * @param n the double.
     * @return string representation of {@code n}
     */
    public static String string(double n) {
        if (Double.isInfinite(n)) {
            return (n > 0) ? "+inf" : "-inf";
        }
        return Double.toString(n);
    }

    /**
     * Create SHA1 digest from Lua script.
     * 
     * @param script the script
     * @return the Base16 encoded SHA1 value
     */
    public static String digest(byte[] script) {
        return digest(ByteBuffer.wrap(script));
    }

    /**
     * Create SHA1 digest from Lua script.
     * 
     * @param script the script
     * @return the Base16 encoded SHA1 value
     */
    public static String digest(ByteBuffer script) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.update(script);
            return new String(Base16.encode(md.digest(), false));
        } catch (NoSuchAlgorithmException e) {
            throw new RedisException("JVM does not support SHA1");
        }
    }

    /**
     * Convert a {@code String} array into a delimited {@code String} (e.g. CSV).
     * <p>
     * Useful for {@code toString()} implementations.
     * 
     * @param arr the array to display
     * @param delim the delimiter to use (typically a ",")
     * @return the delimited {@code String}
     */
    public static String arrayToDelimitedString(Object[] arr, String delim) {
        if ((arr == null || arr.length == 0)) {
            return "";
        }
        if (arr.length == 1) {
            return "" + arr[0];
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /**
     * Convert a {@link Collection} to a delimited {@code String} (e.g. CSV).
     * <p>
     * Useful for {@code toString()} implementations.
     * 
     * @param coll the {@code Collection} to convert
     * @param delim the delimiter to use (typically a ",")
     * @param prefix the {@code String} to start each element with
     * @param suffix the {@code String} to end each element with
     * @return the delimited {@code String}
     */
    public static String collectionToDelimitedString(Collection<?> coll, String delim, String prefix, String suffix) {

        if (coll == null || coll.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<?> it = coll.iterator();
        while (it.hasNext()) {
            sb.append(prefix).append(it.next()).append(suffix);
            if (it.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }
}
