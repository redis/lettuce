/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Scan command support exposed through {@link Iterator}.
 * <p>
 * {@link ScanIterator} uses synchronous command interfaces to scan over keys ({@code SCAN}), sets ({@code SSCAN}), sorted sets
 * ({@code ZSCAN}), and hashes ({@code HSCAN}). A {@link ScanIterator} is stateful and not thread-safe. Instances can be used
 * only once to iterate over results.
 * <p>
 * Use {@link ScanArgs#limit(long)} to set the batch size.
 * <p>
 * Data structure scanning is progressive and stateful and demand-aware. It supports full iterations (until all received cursors
 * are exhausted) and premature termination. Subsequent scan commands to fetch the cursor data get only issued if the caller
 * signals demand by consuming the {@link ScanIterator}.
 *
 * @param <T> Element type
 * @author Mark Paluch
 * @since 4.4
 */
public abstract class ScanIterator<T> implements Iterator<T> {

    private ScanIterator() {
    }

    /**
     * Sequentially iterate over keys in the keyspace. This method uses {@code SCAN} to perform an iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link ScanIterator}.
     */
    public static <K, V> ScanIterator<K> scan(RedisKeyCommands<K, V> commands) {
        return scan(commands, Optional.empty());
    }

    /**
     * Sequentially iterate over keys in the keyspace. This method uses {@code SCAN} to perform an iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param scanArgs the scan arguments, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link ScanIterator}.
     */
    public static <K, V> ScanIterator<K> scan(RedisKeyCommands<K, V> commands, ScanArgs scanArgs) {

        LettuceAssert.notNull(scanArgs, "ScanArgs must not be null");

        return scan(commands, Optional.of(scanArgs));
    }

    private static <K, V> ScanIterator<K> scan(RedisKeyCommands<K, V> commands, Optional<ScanArgs> scanArgs) {

        LettuceAssert.notNull(commands, "RedisKeyCommands must not be null");

        return new SyncScanIterator<K>() {

            @Override
            protected ScanCursor nextScanCursor(ScanCursor scanCursor) {

                KeyScanCursor<K> cursor = getNextScanCursor(scanCursor);
                chunk = cursor.getKeys().iterator();
                return cursor;
            }

            private KeyScanCursor<K> getNextScanCursor(ScanCursor scanCursor) {

                if (scanCursor == null) {
                    return scanArgs.map(commands::scan).orElseGet(commands::scan);
                }

                return scanArgs.map((scanArgs) -> commands.scan(scanCursor, scanArgs))
                        .orElseGet(() -> commands.scan(scanCursor));
            }

        };
    }

    /**
     * Sequentially iterate over entries in a hash identified by {@code key}. This method uses {@code HSCAN} to perform an
     * iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the hash to scan.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link ScanIterator}.
     */
    public static <K, V> ScanIterator<KeyValue<K, V>> hscan(RedisHashCommands<K, V> commands, K key) {
        return hscan(commands, key, Optional.empty());
    }

    /**
     * Sequentially iterate over entries in a hash identified by {@code key}. This method uses {@code HSCAN} to perform an
     * iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the hash to scan.
     * @param scanArgs the scan arguments, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link ScanIterator}.
     */
    public static <K, V> ScanIterator<KeyValue<K, V>> hscan(RedisHashCommands<K, V> commands, K key, ScanArgs scanArgs) {

        LettuceAssert.notNull(scanArgs, "ScanArgs must not be null");

        return hscan(commands, key, Optional.of(scanArgs));
    }

    private static <K, V> ScanIterator<KeyValue<K, V>> hscan(RedisHashCommands<K, V> commands, K key,
            Optional<ScanArgs> scanArgs) {

        LettuceAssert.notNull(commands, "RedisKeyCommands must not be null");
        LettuceAssert.notNull(key, "Key must not be null");

        return new SyncScanIterator<KeyValue<K, V>>() {

            @Override
            protected ScanCursor nextScanCursor(ScanCursor scanCursor) {

                MapScanCursor<K, V> cursor = getNextScanCursor(scanCursor);
                chunk = cursor.getMap().keySet().stream().map(k -> KeyValue.fromNullable(k, cursor.getMap().get(k))).iterator();
                return cursor;
            }

            private MapScanCursor<K, V> getNextScanCursor(ScanCursor scanCursor) {

                if (scanCursor == null) {
                    return scanArgs.map(scanArgs -> commands.hscan(key, scanArgs)).orElseGet(() -> commands.hscan(key));
                }

                return scanArgs.map((scanArgs) -> commands.hscan(key, scanCursor, scanArgs))
                        .orElseGet(() -> commands.hscan(key, scanCursor));
            }

        };
    }

    /**
     * Sequentially iterate over elements in a set identified by {@code key}. This method uses {@code SSCAN} to perform an
     * iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the set to scan.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link ScanIterator}.
     */
    public static <K, V> ScanIterator<V> sscan(RedisSetCommands<K, V> commands, K key) {
        return sscan(commands, key, Optional.empty());
    }

    /**
     * Sequentially iterate over elements in a set identified by {@code key}. This method uses {@code SSCAN} to perform an
     * iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the set to scan.
     * @param scanArgs the scan arguments, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link ScanIterator}.
     */
    public static <K, V> ScanIterator<V> sscan(RedisSetCommands<K, V> commands, K key, ScanArgs scanArgs) {

        LettuceAssert.notNull(scanArgs, "ScanArgs must not be null");

        return sscan(commands, key, Optional.of(scanArgs));
    }

    private static <K, V> ScanIterator<V> sscan(RedisSetCommands<K, V> commands, K key, Optional<ScanArgs> scanArgs) {

        LettuceAssert.notNull(commands, "RedisKeyCommands must not be null");
        LettuceAssert.notNull(key, "Key must not be null");

        return new SyncScanIterator<V>() {

            @Override
            protected ScanCursor nextScanCursor(ScanCursor scanCursor) {

                ValueScanCursor<V> cursor = getNextScanCursor(scanCursor);
                chunk = cursor.getValues().iterator();
                return cursor;
            }

            private ValueScanCursor<V> getNextScanCursor(ScanCursor scanCursor) {

                if (scanCursor == null) {
                    return scanArgs.map(scanArgs -> commands.sscan(key, scanArgs)).orElseGet(() -> commands.sscan(key));
                }

                return scanArgs.map((scanArgs) -> commands.sscan(key, scanCursor, scanArgs))
                        .orElseGet(() -> commands.sscan(key, scanCursor));
            }

        };
    }

    /**
     * Sequentially iterate over scored values in a sorted set identified by {@code key}. This method uses {@code ZSCAN} to
     * perform an iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the sorted set to scan.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link ScanIterator}.
     */
    public static <K, V> ScanIterator<ScoredValue<V>> zscan(RedisSortedSetCommands<K, V> commands, K key) {
        return zscan(commands, key, Optional.empty());
    }

    /**
     * Sequentially iterate over scored values in a sorted set identified by {@code key}. This method uses {@code ZSCAN} to
     * perform an iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the sorted set to scan.
     * @param scanArgs the scan arguments, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link ScanIterator}.
     */
    public static <K, V> ScanIterator<ScoredValue<V>> zscan(RedisSortedSetCommands<K, V> commands, K key, ScanArgs scanArgs) {

        LettuceAssert.notNull(scanArgs, "ScanArgs must not be null");

        return zscan(commands, key, Optional.of(scanArgs));
    }

    private static <K, V> ScanIterator<ScoredValue<V>> zscan(RedisSortedSetCommands<K, V> commands, K key,
            Optional<ScanArgs> scanArgs) {

        LettuceAssert.notNull(commands, "RedisKeyCommands must not be null");
        LettuceAssert.notNull(key, "Key must not be null");

        return new SyncScanIterator<ScoredValue<V>>() {

            @Override
            protected ScanCursor nextScanCursor(ScanCursor scanCursor) {

                ScoredValueScanCursor<V> cursor = getNextScanCursor(scanCursor);
                chunk = cursor.getValues().iterator();
                return cursor;
            }

            private ScoredValueScanCursor<V> getNextScanCursor(ScanCursor scanCursor) {

                if (scanCursor == null) {
                    return scanArgs.map(scanArgs -> commands.zscan(key, scanArgs)).orElseGet(() -> commands.zscan(key));
                }

                return scanArgs.map((scanArgs) -> commands.zscan(key, scanCursor, scanArgs))
                        .orElseGet(() -> commands.zscan(key, scanCursor));
            }

        };
    }

    /**
     * Returns a sequential {@code Stream} with this {@link ScanIterator} as its source.
     *
     * @return a {@link Stream} for this {@link ScanIterator}.
     */
    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliterator(this, 0, 0), false);
    }

    /**
     * Synchronous {@link ScanIterator} implementation.
     *
     * @param <T>
     */
    private static abstract class SyncScanIterator<T> extends ScanIterator<T> {

        private ScanCursor scanCursor;

        protected Iterator<T> chunk = null;

        @Override
        public boolean hasNext() {

            while (scanCursor == null || !scanCursor.isFinished()) {

                if (scanCursor == null || !hasChunkElements()) {
                    scanCursor = nextScanCursor(scanCursor);
                }

                if (hasChunkElements()) {
                    return true;
                }
            }

            return hasChunkElements();
        }

        private boolean hasChunkElements() {
            return chunk.hasNext();
        }

        @Override
        public T next() {

            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return chunk.next();
        }

        protected abstract ScanCursor nextScanCursor(ScanCursor scanCursor);

    }

}
