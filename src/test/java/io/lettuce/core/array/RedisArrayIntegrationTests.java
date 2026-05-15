/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.condition.RedisConditions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for Redis Array commands. Requires Redis >= 8.8.0.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
@EnabledOnCommand("ARSET")
public class RedisArrayIntegrationTests {

    private static final String KEY = "test:array";

    protected static RedisClient client;

    protected static StatefulRedisConnection<String, String> connection;

    protected static RedisCommands<String, String> redis;

    public RedisArrayIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(6480).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        connection = client.connect();
        redis = connection.sync();
    }

    protected ClientOptions getOptions() {
        return ClientOptions.builder().build();
    }

    @BeforeEach
    public void prepare() {
        assumeTrue(RedisConditions.of(redis).hasCommandArity("ARSET", -4));
        redis.del(KEY);
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    // --- ARSET / ARGET ---

    @Test
    void arsetAndArget() {
        assertThat(redis.arset(KEY, 0, "hello")).isEqualTo(1L); // new slot
        assertThat(redis.arget(KEY, 0)).isEqualTo("hello");
        assertThat(redis.arset(KEY, 0, "updated")).isEqualTo(0L); // update
        assertThat(redis.arget(KEY, 0)).isEqualTo("updated");
    }

    @Test
    void argetMissingKeyReturnsNull() {
        assertThat(redis.arget(KEY, 0)).isNull();
    }

    @Test
    void arsetSparseIndex() {
        assertThat(redis.arset(KEY, 1000, "sparse")).isEqualTo(1L);
        assertThat(redis.arget(KEY, 1000)).isEqualTo("sparse");
        assertThat(redis.arget(KEY, 500)).isNull(); // empty slot
    }

    // --- ARMSET / ARMGET ---

    @Test
    void armsetAndArmget() {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(0L, "a");
        map.put(5L, "b");
        map.put(10L, "c");
        assertThat(redis.armset(KEY, map)).isEqualTo(3L);

        List<String> values = redis.armget(KEY, 0, 5, 10, 3);
        assertThat(values).containsExactly("a", "b", "c", null);
    }

    // --- ARDEL ---

    @Test
    void ardelSingle() {
        redis.arset(KEY, 0, "val");
        assertThat(redis.ardel(KEY, 0)).isEqualTo(1L);
        assertThat(redis.ardel(KEY, 0)).isEqualTo(0L); // already deleted
    }

    @Test
    void ardelMultiple() {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(0L, "a");
        map.put(1L, "b");
        map.put(2L, "c");
        redis.armset(KEY, map);
        assertThat(redis.ardel(KEY, 0L, 1L, 99L)).isEqualTo(2L);
    }

    @Test
    void ardelLastElementRemovesKey() {
        redis.arset(KEY, 0, "only");
        redis.ardel(KEY, 0);
        assertThat(redis.exists(KEY)).isEqualTo(0L);
    }

    // --- ARDELRANGE ---

    @Test
    @SuppressWarnings("unchecked")
    void ardelrange() {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(0L, "a");
        map.put(1L, "b");
        map.put(2L, "c");
        map.put(3L, "d");
        map.put(4L, "e");
        redis.armset(KEY, map);
        assertThat(redis.ardelrange(KEY, Range.create(1L, 3L))).isEqualTo(3L);
        assertThat(redis.arcount(KEY)).isEqualTo(2L); // 0 and 4 remain
    }

    // --- ARLEN / ARCOUNT ---

    @Test
    void arlenAndArcount() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 100, "b");
        assertThat(redis.arlen(KEY)).isEqualTo(101L); // max index + 1
        assertThat(redis.arcount(KEY)).isEqualTo(2L); // populated slots
    }

    @Test
    void arlenMissingKey() {
        assertThat(redis.arlen(KEY)).isEqualTo(0L);
        assertThat(redis.arcount(KEY)).isEqualTo(0L);
    }

    // --- ARGETRANGE ---

    @Test
    void argetrange() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 2, "c");
        redis.arset(KEY, 4, "e");
        List<String> result = redis.argetrange(KEY, Range.create(0L, 4L));
        assertThat(result).hasSize(5);
        assertThat(result.get(0)).isEqualTo("a");
        assertThat(result.get(1)).isNull(); // empty slot
        assertThat(result.get(2)).isEqualTo("c");
        assertThat(result.get(3)).isNull();
        assertThat(result.get(4)).isEqualTo("e");
    }

    // --- ARSCAN ---

    @Test
    void arscan() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 5, "b");
        redis.arset(KEY, 10, "c");
        List<IndexedValue<String>> result = redis.arscan(KEY, ArScanArgs.range(0, 100));
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(new IndexedValue<>(0L, "a"));
        assertThat(result.get(1)).isEqualTo(new IndexedValue<>(5L, "b"));
        assertThat(result.get(2)).isEqualTo(new IndexedValue<>(10L, "c"));
    }

    @Test
    void arscanWithLimit() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 1, "b");
        redis.arset(KEY, 2, "c");
        List<IndexedValue<String>> result = redis.arscan(KEY, ArScanArgs.range(0, 100).limit(2));
        assertThat(result).hasSize(2);
    }

    // --- ARGREP ---

    @Test
    void argrepExact() {
        redis.arset(KEY, 0, "alpha");
        redis.arset(KEY, 1, "beta");
        redis.arset(KEY, 2, "alpha");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().exact("alpha"));
        assertThat(indices).containsExactly(0L, 2L);
    }

    @Test
    void argrepWithValues() {
        redis.arset(KEY, 0, "alpha");
        redis.arset(KEY, 1, "beta");
        List<IndexedValue<String>> result = redis.argrepWithValues(KEY, ArGrepArgs.unbounded().exact("alpha").withValues());
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(new IndexedValue<>(0L, "alpha"));
    }

    @Test
    void argrepMatch() {
        redis.arset(KEY, 0, "hello");
        redis.arset(KEY, 1, "world");
        redis.arset(KEY, 2, "hello world");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().match("hello"));
        assertThat(indices).containsExactly(0L, 2L);
    }

    // --- AROP ---

    @Test
    void aropSum() {
        redis.arset(KEY, 0, "10");
        redis.arset(KEY, 1, "20");
        redis.arset(KEY, 2, "30");
        String sum = redis.aropAggregate(KEY, Range.create(0L, 2L), ArAggregateType.SUM);
        assertThat(sum).isEqualTo("60");
    }

    @Test
    void aropUsed() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 5, "b");
        Long used = redis.aropCount(KEY, Range.create(0L, 10L), ArCountType.USED);
        assertThat(used).isEqualTo(2L);
    }

    @Test
    void aropMatch() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 1, "b");
        redis.arset(KEY, 2, "a");
        Long count = redis.aropMatch(KEY, Range.create(0L, 2L), "a");
        assertThat(count).isEqualTo(2L);
    }

    // --- ARINSERT ---

    @Test
    void arinsert() {
        Long lastIndex = redis.arinsert(KEY, "v1", "v2", "v3");
        assertThat(lastIndex).isEqualTo(2L);
        assertThat(redis.arget(KEY, 0)).isEqualTo("v1");
        assertThat(redis.arget(KEY, 1)).isEqualTo("v2");
        assertThat(redis.arget(KEY, 2)).isEqualTo("v3");
    }

    // --- ARRING ---

    @Test
    void arring() {
        Long lastIndex = redis.arring(KEY, 3, "a", "b", "c", "d", "e");
        // ring size 3, so wraps: slots 0=d, 1=e, 2=c (or similar wrap behavior)
        assertThat(redis.arlen(KEY)).isEqualTo(3L);
    }

    // --- ARNEXT ---

    @Test
    void arnextMissingKey() {
        assertThat(redis.arnext(KEY)).isEqualTo(0L);
    }

    @Test
    void arnext() {
        redis.arinsert(KEY, "a", "b");
        Long next = redis.arnext(KEY);
        assertThat(next).isGreaterThanOrEqualTo(0L);
    }

    // --- ARSEEK ---

    @Test
    void arseekMissingKey() {
        assertThat(redis.arseek(KEY, 10)).isEqualTo(0L);
    }

    @Test
    void arseek() {
        redis.arinsert(KEY, "a");
        assertThat(redis.arseek(KEY, 100)).isEqualTo(1L);
        // After seek, insert should go at index 100
        redis.arinsert(KEY, "seeked");
        assertThat(redis.arget(KEY, 100)).isEqualTo("seeked");
    }

    // --- ARLASTITEMS ---

    @Test
    void arlastitems() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 1, "b");
        redis.arset(KEY, 2, "c");
        List<String> items = redis.arlastitems(KEY, 2);
        assertThat(items).containsExactly("b", "c");
    }

    @Test
    void arlastitemsRev() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 1, "b");
        redis.arset(KEY, 2, "c");
        List<String> items = redis.arlastitemsRev(KEY, 2);
        assertThat(items).containsExactly("c", "b");
    }

    @Test
    void arlastitemsMissingKey() {
        List<String> items = redis.arlastitems(KEY, 5);
        assertThat(items).isEmpty();
    }

    // --- ARINFO ---

    @Test
    void arinfo() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 100, "b");
        ArrayMetadata meta = redis.arinfo(KEY);
        assertThat(meta.getCount()).isEqualTo(2L);
        assertThat(meta.getLen()).isEqualTo(101L);
        assertThat(meta.getNextInsertIndex()).isEqualTo(0L);
        assertThat(meta.getSlices()).isEqualTo(1L);
        assertThat(meta.getDirectorySize()).isEqualTo(1L);
        assertThat(meta.getSuperDirEntries()).isEqualTo(0L);
        assertThat(meta.getSliceSize()).isEqualTo(4096L);
    }

    @Test
    void arinfoFull() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 100, "b");
        ArrayFullMetadata meta = redis.arinfoFull(KEY);
        // base fields
        assertThat(meta.getCount()).isEqualTo(2L);
        assertThat(meta.getLen()).isEqualTo(101L);
        assertThat(meta.getNextInsertIndex()).isEqualTo(0L);
        assertThat(meta.getSlices()).isEqualTo(1L);
        assertThat(meta.getDirectorySize()).isEqualTo(1L);
        assertThat(meta.getSuperDirEntries()).isEqualTo(0L);
        assertThat(meta.getSliceSize()).isEqualTo(4096L);
        // extended fields
        assertThat(meta.getDenseSlices()).isEqualTo(0L);
        assertThat(meta.getSparseSlices()).isEqualTo(1L);
        assertThat(meta.getAvgDenseSize()).isIn("0", "0.0");
        assertThat(meta.getAvgDenseFill()).isIn("0", "0.0");
        assertThat(meta.getAvgSparseSize()).isNotNull();
    }

}
