/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

import io.lettuce.core.codec.Base16;

/**
 * Helper for {@link String} checks. This class is part of the internal API and may change without further notice.
 *
 * @author Mark Paluch
 * @since 3.0
 * @deprecated since 5.3.2, this class will move into {@code io.lettuce.core.internal} with Lettuce 6.
 */
@Deprecated
public class LettuceStrings {

    /**
     * Utility constructor.
     */
    private LettuceStrings() {

    }

    /**
     * Checks if a CharSequence is empty ("") or null.
     *
     * @param cs the char sequence.
     * @return {@code true} if empty.
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks if a CharSequence is not empty ("") and not null.
     *
     * @param cs the char sequence.
     * @return {@code true} if not empty.
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * Convert {@code double} to {@link String}. If {@code n} is infinite, returns positive/negative infinity {@code +inf} and
     * {@code -inf}.
     *
     * @param n the double.
     * @return string representation of {@code n}.
     */
    public static String string(double n) {
        if (Double.isInfinite(n)) {
            return (n > 0) ? "+inf" : "-inf";
        }
        return Double.toString(n);
    }

    /**
     * Convert {@link String} to {@code double}. If {@code s} is {@literal +inf}/{@literal -inf}, returns positive/negative
     * infinity. If {@code s} is {@literal +nan}/{@literal -nan}, returns NaN.
     *
     * @param s string representation of the number.
     * @return the {@code double} value.
     * @since 4.3.3
     */
    public static double toDouble(String s) {

        if ("+inf".equals(s) || "inf".equals(s)) {
            return Double.POSITIVE_INFINITY;
        }

        if ("-inf".equals(s)) {
            return Double.NEGATIVE_INFINITY;
        }

        if ("-nan".equals(s) || "nan".equals(s) || "+nan".equals(s)) {
            return Double.NaN;
        }

        return Double.parseDouble(s);
    }

    /**
     * Create SHA1 digest from Lua script.
     *
     * @param script the script.
     * @return the Base16 encoded SHA1 value.
     */
    public static String digest(byte[] script) {
        return digest(ByteBuffer.wrap(script));
    }

    /**
     * Create SHA1 digest from Lua script.
     *
     * @param script the script.
     * @return the Base16 encoded SHA1 value.
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
     * @param arr the array to display.
     * @param delim the delimiter to use (typically a ",").
     * @return the delimited {@code String}.
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
     * @param coll the {@code Collection} to convert.
     * @param delim the delimiter to use (typically a ",").
     * @param prefix the {@code String} to start each element with.
     * @param suffix the {@code String} to end each element with.
     * @return the delimited {@code String}.
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
