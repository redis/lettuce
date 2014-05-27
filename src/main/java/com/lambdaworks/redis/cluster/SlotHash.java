package com.lambdaworks.redis.cluster;

import java.util.Arrays;

import com.google.common.primitives.Chars;
import com.lambdaworks.codec.CRC16;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 27.05.14 08:38
 */
public class SlotHash {

    public final static byte SUBKEY_START = Chars.toByteArray('{')[0];
    public final static byte SUBKEY_END = Chars.toByteArray('}')[0];

    private SlotHash() {

    }

    public final static int getSlot(byte[] key) {
        byte finalKey[] = key;
        int s = Arrays.binarySearch(key, SUBKEY_START);
        if (s > -1) {
            int e = Arrays.binarySearch(key, s + 1, key.length, SUBKEY_END);
            if (e > -1 && e != s + 1) {

                finalKey = new byte[e - s + 1];
                System.arraycopy(key, s + 1, finalKey, 0, finalKey.length);
            }
        }
        return CRC16.crc16(finalKey) % 16384;
    }
}
