/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import java.nio.ByteBuffer;
import java.util.*;

import io.lettuce.core.codec.CRC16;
import io.lettuce.core.codec.RedisCodec;

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
    public static int getSlot(byte[] key) {
        return getSlot(ByteBuffer.wrap(key));
    }

    /**
     * Calculate the slot from the given key.
     *
     * @param key the key
     * @return slot
     */
    public static int getSlot(ByteBuffer key) {

        int limit = key.limit();
        int position = key.position();

        int start = indexOf(key, SUBKEY_START);
        if (start != -1) {
            int end = indexOf(key, start + 1, SUBKEY_END);
            if (end != -1 && end != start + 1) {
                key.position(start + 1).limit(end);
            }
        }

        try {
            if (key.hasArray()) {
                return CRC16.crc16(key.array(), key.position(), key.limit() - key.position()) % SLOT_COUNT;
            }
            return CRC16.crc16(key) % SLOT_COUNT;
        } finally {
            key.position(position).limit(limit);
        }
    }

    private static int indexOf(ByteBuffer haystack, byte needle) {
        return indexOf(haystack, haystack.position(), needle);
    }

    private static int indexOf(ByteBuffer haystack, int start, byte needle) {

        for (int i = start; i < haystack.remaining(); i++) {

            if (haystack.get(i) == needle) {
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
