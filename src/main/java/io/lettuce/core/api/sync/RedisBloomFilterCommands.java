/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.api.sync;

import io.lettuce.core.bf.arguments.BfInfoArgs;
import io.lettuce.core.bf.BfInsertValue;
import io.lettuce.core.bf.arguments.BfInsertArgs;
import io.lettuce.core.bf.arguments.BfReserveArgs;
import io.lettuce.core.bf.BfScanDumpValue;

import java.util.List;
import java.util.Map;

/**
 * Synchronous executed commands for Bloom Filter
 *
 * @author Yordan Tsintsov
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/probabilistic/bloom-filter/">Redis Bloom Filter</a>
 * @since 7.6
 */
public interface RedisBloomFilterCommands<K, V> {

    Boolean bfAdd(K key, V value);

    Long bfCard(K key);

    Boolean bfExists(K key, V value);

    Map<String, Long> bfInfo(K key);

    Map<String, Long> bfInfo(K key, BfInfoArgs infoArgs);

    @SuppressWarnings("unchecked")
    List<BfInsertValue> bfInsert(K key, V... values);

    @SuppressWarnings("unchecked")
    List<BfInsertValue> bfInsert(K key, BfInsertArgs insertArgs, V... values);

    String bfLoadChunk(K key, long iterator, byte[] data);

    @SuppressWarnings("unchecked")
    List<Boolean> bfMAdd(K key, V... values);

    @SuppressWarnings("unchecked")
    List<Boolean> bfMExists(K key, V... values);

    String bfReserve(K key, double errorRate, long capacity);

    String bfReserve(K key, double errorRate, long capacity, BfReserveArgs reserveArgs);

    BfScanDumpValue bfScanDump(K key, long iterator);

}
