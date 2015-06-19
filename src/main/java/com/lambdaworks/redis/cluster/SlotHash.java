package com.lambdaworks.redis.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Chars;
import com.lambdaworks.codec.CRC16;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Utility to calculate the slot from a key.
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class SlotHash {

    /**
     * Constant for a subkey start.
     */
    public static final byte SUBKEY_START = Chars.toByteArray('{')[1];

    /**
     * Constant for a subkey end.
     */
    public static final byte SUBKEY_END = Chars.toByteArray('}')[1];

    public static final int SLOT_COUNT = 16384;

    private SlotHash() {

    }

    /**
     * Calculate the slot from the given key.
     *
     * @param key the key
     * @return slot
     */
    public static final int getSlot(String key) {
        return getSlot(key.getBytes());
    }

    /**
     * Calculate the slot from the given key.
     *
     * @param key the key
     * @return slot
     */
    public static final int getSlot(byte[] key) {
        byte[] finalKey = key;
        int start = indexOf(key, SUBKEY_START);
        if (start != -1) {
            int end = indexOf(key, start + 1, SUBKEY_END);
            if (end != -1 && end != start + 1) {

                finalKey = new byte[end - (start + 1)];
                System.arraycopy(key, start + 1, finalKey, 0, finalKey.length);
            }
        }
        return CRC16.crc16(finalKey) % SLOT_COUNT;
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

    /**
     * Partition keys by slot-hash. The resulting map honors order of the keys.
     * 
     * @param codec codec to encode the key
     * @param keys iterable of keys
     * @param <K> Key type.
     * @param <V> Value type.
     * @result map between slot-hash and an ordered list of keys.
     * 
     */
    static <K, V> Map<Integer, List<K>> partition(RedisCodec<K, V> codec, Iterable<K> keys) {

        Map<Integer, List<K>> partitioned = Maps.newHashMap();
        for (K key : keys) {
            int slot = getSlot(codec.encodeKey(key));
            if (!partitioned.containsKey(slot)) {
                partitioned.put(slot, Lists.newArrayList());
            }
            Collection<K> list = partitioned.get(slot);
            list.add(key);
        }
        return partitioned;
    }

    /**
     * Create mapping between the Key and hash slot.
     *
     * @param partitioned map partitioned by slothash and keys
     * @param <K>
     */
    static <K> Map<K, Integer> getSlots(Map<Integer, ? extends Iterable<K>> partitioned) {

        Map<K, Integer> result = Maps.newHashMap();
        for (Map.Entry<Integer, ? extends Iterable<K>> entry : partitioned.entrySet()) {
            for (K key : entry.getValue()) {
                result.put(key, entry.getKey());
            }
        }

        return result;
    }
}