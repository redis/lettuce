/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis Array commands.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("ARSET")
public class RedisArrayIntegrationTests extends TestSupport {

    static final String KEY = "test:array";

    protected final RedisCommands<String, String> redis;

    @Inject
    protected RedisArrayIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    public void prepare() {
        redis.del(KEY);
    }

    @AfterAll
    void teardown() {
        redis.del(KEY);
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

    @Test
    void arsetVarargs() {
        Long created = redis.arset(KEY, 0, "a", "b", "c");
        assertThat(created).isEqualTo(3L);
        assertThat(redis.arget(KEY, 0)).isEqualTo("a");
        assertThat(redis.arget(KEY, 1)).isEqualTo("b");
        assertThat(redis.arget(KEY, 2)).isEqualTo("c");
        assertThat(redis.arlen(KEY)).isEqualTo(3L);
    }

    @Test
    void arsetVarargsWithOffset() {
        redis.arset(KEY, 0, "existing");
        Long created = redis.arset(KEY, 5, "x", "y");
        assertThat(created).isEqualTo(2L);
        assertThat(redis.arget(KEY, 5)).isEqualTo("x");
        assertThat(redis.arget(KEY, 6)).isEqualTo("y");
        assertThat(redis.arget(KEY, 0)).isEqualTo("existing");
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
    void ardelrange() {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(0L, "a");
        map.put(1L, "b");
        map.put(2L, "c");
        map.put(3L, "d");
        map.put(4L, "e");
        redis.armset(KEY, map);
        // single range
        assertThat(redis.ardelrange(KEY, ArrayIndexRange.of(1, 3))).isEqualTo(3L);
        assertThat(redis.arcount(KEY)).isEqualTo(2L); // 0 and 4 remain
        // multi-range varargs
        redis.del(KEY);
        redis.armset(KEY, map);
        assertThat(redis.ardelrange(KEY, ArrayIndexRange.of(0, 1), ArrayIndexRange.of(3, 4))).isEqualTo(4L);
        assertThat(redis.arcount(KEY)).isEqualTo(1L); // only 2 remains
        assertThat(redis.arget(KEY, 2)).isEqualTo("c");
    }

    @Test
    void ardelrangeSinglePrimitive() {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(0L, "a");
        map.put(1L, "b");
        map.put(2L, "c");
        map.put(3L, "d");
        redis.armset(KEY, map);
        assertThat(redis.ardelrange(KEY, 1, 2)).isEqualTo(2L);
        assertThat(redis.arget(KEY, 0)).isEqualTo("a");
        assertThat(redis.arget(KEY, 1)).isNull();
        assertThat(redis.arget(KEY, 2)).isNull();
        assertThat(redis.arget(KEY, 3)).isEqualTo("d");
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
        List<String> result = redis.argetrange(KEY, 0, 4);
        assertThat(result).hasSize(5);
        assertThat(result.get(0)).isEqualTo("a");
        assertThat(result.get(1)).isNull(); // empty slot
        assertThat(result.get(2)).isEqualTo("c");
        assertThat(result.get(3)).isNull();
        assertThat(result.get(4)).isEqualTo("e");
    }

    @Test
    void argetrangeReversed() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 2, "c");
        redis.arset(KEY, 4, "e");
        // start > end triggers reverse order on the server
        List<String> result = redis.argetrange(KEY, 4, 0);
        assertThat(result).hasSize(5);
        assertThat(result.get(0)).isEqualTo("e");
        assertThat(result.get(1)).isNull();
        assertThat(result.get(2)).isEqualTo("c");
        assertThat(result.get(3)).isNull();
        assertThat(result.get(4)).isEqualTo("a");
    }

    // --- ARSCAN ---

    @Test
    void arscan() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 5, "b");
        redis.arset(KEY, 10, "c");
        List<IndexedValue<String>> result = redis.arscan(KEY, 0, 100);
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
        List<IndexedValue<String>> result = redis.arscan(KEY, 0, 100, 2);
        assertThat(result).hasSize(2);
    }

    @Test
    void arscanPartialRange() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 3, "b");
        redis.arset(KEY, 7, "c");
        redis.arset(KEY, 10, "d");
        // Only scan indices 2-8
        List<IndexedValue<String>> result = redis.arscan(KEY, 2, 8);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(new IndexedValue<>(3L, "b"));
        assertThat(result.get(1)).isEqualTo(new IndexedValue<>(7L, "c"));
        // empty subrange — no populated slots between 4 and 6
        assertThat(redis.arscan(KEY, 4, 6)).isEmpty();
    }

    @Test
    void arscanReversed() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 3, "b");
        redis.arset(KEY, 7, "c");
        redis.arset(KEY, 10, "d");
        // start > end triggers reverse iteration
        List<IndexedValue<String>> result = redis.arscan(KEY, 10, 0);
        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo(new IndexedValue<>(10L, "d"));
        assertThat(result.get(1)).isEqualTo(new IndexedValue<>(7L, "c"));
        assertThat(result.get(2)).isEqualTo(new IndexedValue<>(3L, "b"));
        assertThat(result.get(3)).isEqualTo(new IndexedValue<>(0L, "a"));
    }

    // --- ARGREP ---

    @Test
    void argrepExact() {
        redis.arset(KEY, 0, "alpha");
        redis.arset(KEY, 1, "beta");
        redis.arset(KEY, 2, "alpha");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().exact("alpha"));
        assertThat(indices).containsExactly(0L, 2L);
        // no matches
        assertThat(redis.argrep(KEY, ArGrepArgs.unbounded().exact("nonexistent"))).isEmpty();
    }

    @Test
    void argrepWithValues() {
        redis.arset(KEY, 0, "target");
        redis.arset(KEY, 1, "other");
        redis.arset(KEY, 2, "target");
        redis.arset(KEY, 5, "target");
        List<IndexedValue<String>> result = redis.argrepWithValues(KEY, ArGrepArgs.unbounded().exact("target"));
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(new IndexedValue<>(0L, "target"));
        assertThat(result.get(1)).isEqualTo(new IndexedValue<>(2L, "target"));
        assertThat(result.get(2)).isEqualTo(new IndexedValue<>(5L, "target"));
    }

    @Test
    void argrepMatch() {
        redis.arset(KEY, 0, "hello");
        redis.arset(KEY, 1, "world");
        redis.arset(KEY, 2, "hello world");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().match("hello"));
        assertThat(indices).containsExactly(0L, 2L);
    }

    @Test
    void argrepGlob() {
        redis.arset(KEY, 0, "foo");
        redis.arset(KEY, 1, "foobar");
        redis.arset(KEY, 2, "bar");
        redis.arset(KEY, 3, "foobaz");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().glob("foo*"));
        assertThat(indices).containsExactly(0L, 1L, 3L);
    }

    @Test
    void argrepRegex() {
        redis.arset(KEY, 0, "abc123");
        redis.arset(KEY, 1, "def456");
        redis.arset(KEY, 2, "abc789");
        redis.arset(KEY, 3, "xyz");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().re("^abc"));
        assertThat(indices).containsExactly(0L, 2L);
    }

    @Test
    void argrepBoundedRange() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 3, "b");
        redis.arset(KEY, 7, "c");
        redis.arset(KEY, 10, "d");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.range(2, 8).exact("b"));
        assertThat(indices).containsExactly(3L);
    }

    @Test
    void argrepFromRange() {
        redis.arset(KEY, 0, "bcd");
        redis.arset(KEY, 3, "abc");
        redis.arset(KEY, 7, "bcd");
        redis.arset(KEY, 10, "abcd");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.from(3).glob("*bc*").re("^.{3}$").and());
        assertThat(indices).containsExactly(3L, 7L);
    }

    @Test
    void argrepToRange() {
        redis.arset(KEY, 0, "b");
        redis.arset(KEY, 3, "abc");
        redis.arset(KEY, 7, "bcd");
        redis.arset(KEY, 10, "c");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.to(7).glob("*bc*"));
        assertThat(indices).containsExactly(3L, 7L);
    }

    @Test
    void argrepReversed() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 3, "b");
        redis.arset(KEY, 5, "c");
        redis.arset(KEY, 7, "d");
        redis.arset(KEY, 10, "e");
        // unbounded reversed: all elements, descending order
        List<Long> all = redis.argrep(KEY, ArGrepArgs.unbounded().reversed().glob("*"));
        assertThat(all).containsExactly(10L, 7L, 5L, 3L, 0L);
        // range reversed: elements in [3,7], descending
        List<Long> bounded = redis.argrep(KEY, ArGrepArgs.range(3, 7).reversed().glob("*"));
        assertThat(bounded).containsExactly(7L, 5L, 3L);
        // from reversed: elements >= 5, descending
        List<Long> fromRev = redis.argrep(KEY, ArGrepArgs.from(5).reversed().glob("*"));
        assertThat(fromRev).containsExactly(10L, 7L, 5L);
        // to reversed: elements <= 5, descending
        List<Long> toRev = redis.argrep(KEY, ArGrepArgs.to(5).reversed().glob("*"));
        assertThat(toRev).containsExactly(5L, 3L, 0L);
    }

    @Test
    void argrepLimit() {
        redis.arset(KEY, 0, "x");
        redis.arset(KEY, 1, "x");
        redis.arset(KEY, 2, "x");
        redis.arset(KEY, 3, "x");
        redis.arset(KEY, 4, "x");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().exact("x").limit(3));
        assertThat(indices).hasSize(3);
        assertThat(indices).containsExactly(0L, 1L, 2L);
    }

    @Test
    void argrepNocase() {
        redis.arset(KEY, 0, "Hello");
        redis.arset(KEY, 1, "HELLO");
        redis.arset(KEY, 2, "hello");
        redis.arset(KEY, 3, "world");
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().exact("hello").nocase());
        assertThat(indices).containsExactly(0L, 1L, 2L);
    }

    @Test
    void argrepMultiplePredicatesOr() {
        redis.arset(KEY, 0, "alpha");
        redis.arset(KEY, 1, "beta");
        redis.arset(KEY, 2, "gamma");
        redis.arset(KEY, 3, "delta");
        // OR is default: matches "alpha" OR anything matching ".*ta"
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().exact("alpha").re(".*ta"));
        assertThat(indices).containsExactly(0L, 1L, 3L);
    }

    @Test
    void argrepMultiplePredicatesAnd() {
        redis.arset(KEY, 0, "foobar");
        redis.arset(KEY, 1, "foobaz");
        redis.arset(KEY, 2, "barfoo");
        redis.arset(KEY, 3, "hello");
        // AND: must match glob "foo*" AND contain "bar"
        List<Long> indices = redis.argrep(KEY, ArGrepArgs.unbounded().glob("foo*").match("bar").and());
        assertThat(indices).containsExactly(0L);
    }

    @Test
    void argrepWithValuesBoundedAndLimit() {
        redis.arset(KEY, 0, "x");
        redis.arset(KEY, 1, "x");
        redis.arset(KEY, 2, "x");
        redis.arset(KEY, 3, "x");
        List<IndexedValue<String>> result = redis.argrepWithValues(KEY, ArGrepArgs.range(1, 3).exact("x").limit(2));
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(new IndexedValue<>(1L, "x"));
        assertThat(result.get(1)).isEqualTo(new IndexedValue<>(2L, "x"));
    }

    // --- AROP ---

    @Test
    void aropAggregate() {
        redis.arset(KEY, 0, "30");
        redis.arset(KEY, 1, "10");
        redis.arset(KEY, 2, "20");
        assertThat(redis.aropAggregate(KEY, 0, 2, ArAggregateType.SUM)).isEqualTo("60");
        assertThat(redis.aropAggregate(KEY, 0, 2, ArAggregateType.MIN)).isEqualTo("10");
        assertThat(redis.aropAggregate(KEY, 0, 2, ArAggregateType.MAX)).isEqualTo("30");
    }

    @Test
    void aropCount() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 5, "b");
        assertThat(redis.aropCount(KEY, 0, 10)).isEqualTo(2L);
    }

    @Test
    void aropCountMatch() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 1, "b");
        redis.arset(KEY, 2, "a");
        assertThat(redis.aropCount(KEY, 0, 2, "a")).isEqualTo(2L);
        // no matches
        assertThat(redis.aropCount(KEY, 0, 2, "z")).isEqualTo(0L);
    }

    @Test
    void aropBitwise() {
        redis.arset(KEY, 0, "7"); // 0b111
        redis.arset(KEY, 1, "5"); // 0b101
        redis.arset(KEY, 2, "3"); // 0b011
        assertThat(redis.aropBitwise(KEY, 0, 2, ArBitwiseType.AND)).isEqualTo(1L); // 0b001
        assertThat(redis.aropBitwise(KEY, 0, 2, ArBitwiseType.OR)).isEqualTo(7L); // 0b111
        assertThat(redis.aropBitwise(KEY, 0, 2, ArBitwiseType.XOR)).isEqualTo(1L); // 0b001
    }

    @Test
    void aropAggregateReversed() {
        redis.arset(KEY, 0, "30");
        redis.arset(KEY, 1, "10");
        redis.arset(KEY, 2, "20");
        // start > end: aggregation is order-independent, results should match forward
        assertThat(redis.aropAggregate(KEY, 2, 0, ArAggregateType.SUM)).isEqualTo("60");
        assertThat(redis.aropAggregate(KEY, 2, 0, ArAggregateType.MIN)).isEqualTo("10");
        assertThat(redis.aropAggregate(KEY, 2, 0, ArAggregateType.MAX)).isEqualTo("30");
    }

    @Test
    void aropCountReversed() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 5, "b");
        // reversed range still counts all populated elements
        assertThat(redis.aropCount(KEY, 10, 0)).isEqualTo(2L);
    }

    // --- ARINSERT ---

    @Test
    void arinsert() {
        // single insert
        assertThat(redis.arinsert(KEY, "only")).isEqualTo(0L);
        assertThat(redis.arget(KEY, 0)).isEqualTo("only");
        redis.del(KEY);
        // varargs insert
        Long lastIndex = redis.arinsert(KEY, "v1", "v2", "v3");
        assertThat(lastIndex).isEqualTo(2L);
        assertThat(redis.arget(KEY, 0)).isEqualTo("v1");
        assertThat(redis.arget(KEY, 1)).isEqualTo("v2");
        assertThat(redis.arget(KEY, 2)).isEqualTo("v3");
    }

    // --- ARRING ---

    @Test
    void arring() {
        // single insert into ring
        assertThat(redis.arring(KEY, 5, "first")).isEqualTo(0L);
        assertThat(redis.arget(KEY, 0)).isEqualTo("first");
        assertThat(redis.arcount(KEY)).isEqualTo(1L);
        redis.del(KEY);
        // varargs with wrap-around
        redis.arring(KEY, 3, "a", "b", "c", "d", "e");
        assertThat(redis.arlen(KEY)).isEqualTo(3L);
        assertThat(redis.arcount(KEY)).isEqualTo(3L);
    }

    @Test
    void arringWrapsAround() {
        // Insert 3 items into ring of size 3
        redis.arring(KEY, 3, "a", "b", "c");
        assertThat(redis.arget(KEY, 0)).isEqualTo("a");
        assertThat(redis.arget(KEY, 1)).isEqualTo("b");
        assertThat(redis.arget(KEY, 2)).isEqualTo("c");
        // Insert 2 more — wraps around, overwrites indices 0 and 1
        redis.arring(KEY, 3, "d", "e");
        assertThat(redis.arlen(KEY)).isEqualTo(3L);
        assertThat(redis.arget(KEY, 0)).isEqualTo("d");
        assertThat(redis.arget(KEY, 1)).isEqualTo("e");
        assertThat(redis.arget(KEY, 2)).isEqualTo("c");
    }

    // --- ARNEXT ---

    @Test
    void arnextMissingKey() {
        assertThat(redis.arnext(KEY)).isEqualTo(0L);
    }

    @Test
    void arnext() {
        redis.arinsert(KEY, "a", "b");
        assertThat(redis.arnext(KEY)).isEqualTo(2L);
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
        assertThat(redis.arlastitems(KEY, 2)).containsExactly("b", "c");
        // reversed order
        assertThat(redis.arlastitems(KEY, 2, true)).containsExactly("c", "b");
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
        ArrayInfo meta = redis.arinfo(KEY);
        assertThat(meta.getCount()).isEqualTo(2L);
        assertThat(meta.getLen()).isEqualTo(101L);
        assertThat(meta.getNextInsertIndex()).isEqualTo(0L);
        assertThat(meta.getSlices()).isEqualTo(1L);
        assertThat(meta.getDirectorySize()).isEqualTo(1L);
        assertThat(meta.getSuperDirEntries()).isEqualTo(0L);
        assertThat(meta.getSliceSize()).isEqualTo(4096L);
        // raw map access
        Map<String, Object> raw = meta.getInfo();
        assertThat(raw).isNotEmpty();
        assertThat(raw).containsKey("count");
        assertThat(raw).containsKey("len");
    }

    @Test
    void arinfoFull() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 100, "b");
        ArrayInfoFull meta = redis.arinfoFull(KEY);
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
        // raw map access
        Map<String, Object> raw = meta.getInfo();
        assertThat(raw).isNotEmpty();
        assertThat(raw).containsKey("count");
        assertThat(raw).containsKey("dense-slices");
    }

}
