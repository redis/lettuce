package com.lambdaworks.redis.cluster;

import com.google.common.primitives.Chars;
import com.lambdaworks.codec.CRC16;

/**
 * Utility to calculate the slot from a key.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 27.05.14 08:38
 */
public class SlotHash {

    public final static byte SUBKEY_START = Chars.toByteArray('{')[1];
    public final static byte SUBKEY_END = Chars.toByteArray('}')[1];

    private SlotHash() {

    }

    /**
     * Calculate the slot from the given key.
     * 
     * @param key
     * @return
     */
    public final static int getSlot(String key) {
        return getSlot(key.getBytes());
    }

    /**
     * Calculate the slot from the given key.
     * 
     * @param key
     * @return slot
     */
    public final static int getSlot(byte[] key) {
        byte finalKey[] = key;
        int start = indexOf(key, SUBKEY_START);
        if (start != -1) {
            int end = indexOf(key, start + 1, SUBKEY_END);
            if (end != -1 && end != start + 1) {

                finalKey = new byte[end - (start + 1)];
                System.arraycopy(key, start + 1, finalKey, 0, finalKey.length);
            }
        }
        return CRC16.crc16(finalKey) % 16384;
    }

    private static int indexOf(byte[] haystack, byte needle) {
        return indexOf(haystack, 0, needle);
    }

    private static int indexOf(byte[] haystack, int start, byte needle) {

        for (int i = start; i < haystack.length; i++) {

            if (haystack[i] == needle) {
                return i;
            }
        }

        return -1;
    }
}
