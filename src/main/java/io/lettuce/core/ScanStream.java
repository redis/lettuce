/*
 * Copyright 2017-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.api.reactive.RedisHashReactiveCommands;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import io.lettuce.core.api.reactive.RedisSetReactiveCommands;
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Scan command support exposed through {@link Flux}.
 * <p>
 * {@link ScanStream} uses reactive command interfaces to scan over keys ({@code SCAN}), sets ({@code SSCAN}), sorted sets (
 * {@code ZSCAN}), and hashes ({@code HSCAN}).
 * <p>
 * Use {@link ScanArgs#limit(long)} to set the batch size.
 * <p>
 * Data structure scanning is progressive and stateful and demand-aware. It supports full iterations (until all received cursors
 * are exhausted) and premature termination. Subsequent scan commands to fetch the cursor data get only issued if the subscriber
 * signals demand.
 *
 * @author Mark Paluch
 * @author Mikhael Sokolov
 * @since 5.1
 */
public abstract class ScanStream {

    private ScanStream() {
    }

    /**
     * Sequentially iterate over keys in the keyspace. This method uses {@code SCAN} to perform an iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link Flux}.
     */
    public static <K, V> Flux<K> scan(RedisKeyReactiveCommands<K, V> commands) {
        return scan(commands, Optional.empty());
    }

    /**
     * Sequentially iterate over keys in the keyspace. This method uses {@code SCAN} to perform an iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param scanArgs the scan arguments, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link Flux}.
     */
    public static <K, V> Flux<K> scan(RedisKeyReactiveCommands<K, V> commands, ScanArgs scanArgs) {

        LettuceAssert.notNull(scanArgs, "ScanArgs must not be null");

        return scan(commands, Optional.of(scanArgs));
    }

    private static <K, V> Flux<K> scan(RedisKeyReactiveCommands<K, V> commands, Optional<ScanArgs> scanArgs) {

        LettuceAssert.notNull(commands, "RedisKeyCommands must not be null");

        return scanArgs.map(commands::scan).orElseGet(commands::scan)
                .expand(c -> !c.isFinished() ? scanArgs.map(it -> commands.scan(c, it)).orElseGet(() -> commands.scan(c))
                        : Mono.empty())
                .flatMapIterable(KeyScanCursor::getKeys);
    }

    /**
     * Sequentially iterate over entries in a hash identified by {@code key}. This method uses {@code HSCAN} to perform an
     * iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the hash to scan.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link Flux}.
     */
    public static <K, V> Flux<KeyValue<K, V>> hscan(RedisHashReactiveCommands<K, V> commands, K key) {
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
     * @return a new {@link Flux}.
     */
    public static <K, V> Flux<KeyValue<K, V>> hscan(RedisHashReactiveCommands<K, V> commands, K key, ScanArgs scanArgs) {

        LettuceAssert.notNull(scanArgs, "ScanArgs must not be null");

        return hscan(commands, key, Optional.of(scanArgs));
    }

    private static <K, V> Flux<KeyValue<K, V>> hscan(RedisHashReactiveCommands<K, V> commands, K key,
            Optional<ScanArgs> scanArgs) {

        LettuceAssert.notNull(commands, "RedisHashReactiveCommands must not be null");
        LettuceAssert.notNull(key, "Key must not be null");

        return scanArgs.map(it -> commands.hscan(key, it)).orElseGet(() -> commands.hscan(key))
                .expand(c -> !c.isFinished()
                        ? scanArgs.map(it -> commands.hscan(key, c, it)).orElseGet(() -> commands.hscan(key, c))
                        : Mono.empty())
                .flatMapIterable(c -> {
                    List<KeyValue<K, V>> list = new ArrayList<>(c.getMap().size());

                    for (Map.Entry<K, V> kvEntry : c.getMap().entrySet()) {
                        list.add(KeyValue.fromNullable(kvEntry.getKey(), kvEntry.getValue()));
                    }
                    return list;
                });
    }

    /**
     * Sequentially iterate over elements in a set identified by {@code key}. This method uses {@code SSCAN} to perform an
     * iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the set to scan.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link Flux}.
     */
    public static <K, V> Flux<V> sscan(RedisSetReactiveCommands<K, V> commands, K key) {
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
     * @return a new {@link Flux}.
     */
    public static <K, V> Flux<V> sscan(RedisSetReactiveCommands<K, V> commands, K key, ScanArgs scanArgs) {

        LettuceAssert.notNull(scanArgs, "ScanArgs must not be null");

        return sscan(commands, key, Optional.of(scanArgs));
    }

    private static <K, V> Flux<V> sscan(RedisSetReactiveCommands<K, V> commands, K key, Optional<ScanArgs> scanArgs) {

        LettuceAssert.notNull(commands, "RedisSetReactiveCommands must not be null");
        LettuceAssert.notNull(key, "Key must not be null");

        return scanArgs.map(it -> commands.sscan(key, it)).orElseGet(() -> commands.sscan(key))
                .expand(c -> !c.isFinished()
                        ? scanArgs.map(it -> commands.sscan(key, c, it)).orElseGet(() -> commands.sscan(key, c))
                        : Mono.empty())
                .flatMapIterable(ValueScanCursor::getValues);
    }

    /**
     * Sequentially iterate over elements in a set identified by {@code key}. This method uses {@code SSCAN} to perform an
     * iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the sorted set to scan.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link Flux}.
     */
    public static <K, V> Flux<ScoredValue<V>> zscan(RedisSortedSetReactiveCommands<K, V> commands, K key) {
        return zscan(commands, key, Optional.empty());
    }

    /**
     * Sequentially iterate over elements in a set identified by {@code key}. This method uses {@code SSCAN} to perform an
     * iterative scan.
     *
     * @param commands the commands interface, must not be {@code null}.
     * @param key the sorted set to scan.
     * @param scanArgs the scan arguments, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link Flux}.
     */
    public static <K, V> Flux<ScoredValue<V>> zscan(RedisSortedSetReactiveCommands<K, V> commands, K key, ScanArgs scanArgs) {

        LettuceAssert.notNull(scanArgs, "ScanArgs must not be null");

        return zscan(commands, key, Optional.of(scanArgs));
    }

    private static <K, V> Flux<ScoredValue<V>> zscan(RedisSortedSetReactiveCommands<K, V> commands, K key,
            Optional<ScanArgs> scanArgs) {

        LettuceAssert.notNull(commands, "RedisSortedSetReactiveCommands must not be null");
        LettuceAssert.notNull(key, "Key must not be null");

        return scanArgs.map(it -> commands.zscan(key, it)).orElseGet(() -> commands.zscan(key))
                .expand(c -> !c.isFinished()
                        ? scanArgs.map(it -> commands.zscan(key, c, it)).orElseGet(() -> commands.zscan(key, c))
                        : Mono.empty())
                .flatMapIterable(ScoredValueScanCursor::getValues);
    }

}
