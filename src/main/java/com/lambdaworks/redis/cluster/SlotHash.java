package com.lambdaworks.redis.cluster;

import java.nio.ByteBuffer;
import java.util.*;

import com.lambdaworks.codec.CRC16;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Utility to calculate the slot from a key.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class SlotHash {

    /**
     * Constant for a subkey start.
     */
    public static final byte SUBKEY_START = (byte) '{';

    /**
     * Constant for a subkey end.
     */
    public static final byte SUBKEY_END = (byte) '}';

    /**
     * Number of redis cluster slot hashes.
     */
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
        return getSlot(ByteBuffer.wrap(key));
    }

    /**
     * Calculate the slot from the given key.
     *
     * @param key the key
     * @return slot
     */
    public static final int getSlot(ByteBuffer key) {

        byte[] input = new byte[key.remaining()];
        key.duplicate().get(input);

        byte[] finalKey = input;

        int start = indexOf(input, SUBKEY_START);
        if (start != -1) {
            int end = indexOf(input, start + 1, SUBKEY_END);
            if (end != -1 && end != start + 1) {

                finalKey = new byte[end - (start + 1)];
                System.arraycopy(input, start + 1, finalKey, 0, finalKey.length);
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

        Map<Integer, List<K>> partitioned = new HashMap<>();
        for (K key : keys) {
            int slot = getSlot(codec.encodeKey(key));
            if (!partitioned.containsKey(slot)) {
                partitioned.put(slot, new ArrayList<>());
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

        Map<K, Integer> result = new HashMap<>();
        for (Map.Entry<Integer, ? extends Iterable<K>> entry : partitioned.entrySet()) {
            for (K key : entry.getValue()) {
                result.put(key, entry.getKey());
            }
        }

        return result;
    }
}