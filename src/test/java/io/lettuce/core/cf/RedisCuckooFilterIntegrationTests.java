/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cf;

import javax.inject.Inject;
import java.util.List;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cf.arguments.CfInsertArgs;
import io.lettuce.core.cf.arguments.CfReserveArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisCuckooFilterCommands}.
 *
 * @author Yordan Tsintsov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("CF.ADD")
public class RedisCuckooFilterIntegrationTests {

    private static final String MY_KEY = "books:name";

    private static final String MY_VALUE = "Dune";

    private static final String MY_VALUE_2 = "Dune Messiah";

    private final RedisCommands<String, String> redis;

    @Inject
    protected RedisCuckooFilterIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void prepare() {
        redis.flushall();
    }

    @AfterAll
    void teardown() {
        redis.flushall();
    }

    @Test
    void cfReserve() {
        String result = redis.cfReserve(MY_KEY, 100);

        assertThat(result).isEqualTo("OK");
    }

    @Test
    void cfReserveWithArgs() {
        String result = redis.cfReserve(MY_KEY, 100, CfReserveArgs.Builder.bucketSize(2).expansion(1));

        assertThat(result).isEqualTo("OK");
    }

    @Test
    void cfAdd() {
        Boolean result = redis.cfAdd(MY_KEY, MY_VALUE);

        assertThat(result).isTrue();
        assertThat(redis.cfExists(MY_KEY, MY_VALUE)).isTrue();
    }

    @Test
    void cfAddNx() {
        assertThat(redis.cfAddNx(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.cfAddNx(MY_KEY, MY_VALUE)).isFalse();
    }

    @Test
    void cfInsert() {
        List<Boolean> result = redis.cfInsert(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(result).containsExactly(true, true);
        assertThat(redis.cfExists(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.cfExists(MY_KEY, MY_VALUE_2)).isTrue();
    }

    @Test
    void cfInsertWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);
        List<Boolean> result = redis.cfInsert(MY_KEY, insertArgs, MY_VALUE, MY_VALUE_2);

        assertThat(result).containsExactly(true, true);
    }

    @Test
    void cfInsertNx() {
        redis.cfAdd(MY_KEY, MY_VALUE);

        List<Boolean> result = redis.cfInsertNx(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(result).containsExactly(false, true);
    }

    @Test
    void cfInsertNxWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);
        redis.cfAdd(MY_KEY, MY_VALUE);

        List<Boolean> result = redis.cfInsertNx(MY_KEY, insertArgs, MY_VALUE, MY_VALUE_2);

        assertThat(result).containsExactly(false, true);
    }

    @Test
    void cfExists() {
        redis.cfAdd(MY_KEY, MY_VALUE);

        assertThat(redis.cfExists(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.cfExists(MY_KEY, MY_VALUE_2)).isFalse();
    }

    @Test
    void cfMExists() {
        redis.cfAdd(MY_KEY, MY_VALUE);

        List<Boolean> result = redis.cfMExists(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(result).containsExactly(true, false);
    }

    @Test
    void cfDel() {
        redis.cfAdd(MY_KEY, MY_VALUE);

        assertThat(redis.cfDel(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.cfExists(MY_KEY, MY_VALUE)).isFalse();
    }

    @Test
    void cfCount() {
        redis.cfAdd(MY_KEY, MY_VALUE);

        assertThat(redis.cfCount(MY_KEY, MY_VALUE)).isEqualTo(1L);
        assertThat(redis.cfCount(MY_KEY, MY_VALUE_2)).isEqualTo(0L);
    }

    @Test
    void cfInfo() {
        redis.cfAdd(MY_KEY, MY_VALUE);

        CfInfoValue result = redis.cfInfo(MY_KEY);

        assertThat(result).isNotNull();
        assertThat(result.getNumberOfItemsInserted()).isEqualTo(1L);
        assertThat(result.getRawInfo()).containsKey("Number of filters");
        assertThat(result.getRawInfo()).containsKey("Max iterations");
    }

    /**
     * Verifies that CF.INSERT correctly handles items that cannot be inserted because the filter is full.
     *
     * <p>
     * Redis returns integer {@code -1} per-item when the filter is full. The output class
     * {@link io.lettuce.core.output.CuckooInsertBooleanListOutput} maps:
     * <ul>
     * <li>1 → {@code true} (inserted)</li>
     * <li>0 → {@code false} (already exists)</li>
     * <li>-1 → {@code null} (filter full)</li>
     * </ul>
     * This test reserves a tiny filter (BUCKETSIZE 1, EXPANSION 0) and inserts the same value repeatedly: the first two
     * insertions succeed ({@code true}) while the remaining ones are full and return {@code -1}, which must map to {@code null}
     * (distinct from {@code false}).
     */
    @Test
    void cfInsertReturnsNullWhenFilterIsFull() {
        String key = "cf:full:insert";
        // BUCKETSIZE 1 EXPANSION 0: same value can appear in at most 2*BUCKETSIZE=2 slots; EXPANSION 0 disables growth
        redis.cfReserve(key, 1000, CfReserveArgs.Builder.bucketSize(1).expansion(0));

        // Same value repeated: first 2 succeed; subsequent inserts cannot be placed in the filter
        List<Boolean> result = redis.cfInsert(key, "W", "W", "W", "W", "W", "W");

        assertThat(result).hasSize(6);
        assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
        assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
        // Elements 2-5: filter is full, server returns -1, which must map to null (distinct from false)
        for (int i = 2; i < result.size(); i++) {
            assertThat(result.get(i)).as("result[%d] must be null (filter full = -1)", i).isNull();
        }
    }

    /**
     * Verifies that CF.INSERTNX distinguishes between "already exists" ({@code false}) and "filter full" ({@code null}).
     *
     * <p>
     * With the old {@link io.lettuce.core.output.ErrorTolerantBooleanListOutput} both 0 and -1 were mapped to {@code false},
     * making them indistinguishable. The new {@link io.lettuce.core.output.CuckooInsertBooleanListOutput} maps 0 →
     * {@code false} and -1 → {@code null}.
     *
     * <p>
     * Note: reproducing -1 via INSERTNX is difficult because inserting the same value always returns 0 ("already exists"). The
     * -1 → null mapping is verified end-to-end by {@link #cfInsertReturnsNullWhenFilterIsFull()}, which uses CF.INSERT and the
     * same underlying output class ({@link io.lettuce.core.output.CuckooInsertBooleanListOutput}). This test focuses on
     * confirming that 0 ("already exists") maps to {@code false}, NOT to {@code null}.
     */
    @Test
    void cfInsertNxDistinguishesAlreadyExistsFromFilterFull() {
        String key = "cf:full:insertnx";
        redis.cfReserve(key, 100, CfReserveArgs.Builder.bucketSize(2).expansion(1));

        // Pre-populate the filter so INSERTNX sees "already exists" (0) for known items
        redis.cfAdd(key, "known");

        // existing item must return false (0), NOT null — 0 and -1 must remain distinguishable
        List<Boolean> existing = redis.cfInsertNx(key, "known");
        assertThat(existing).hasSize(1);
        assertThat(existing.get(0)).isEqualTo(Boolean.FALSE); // "already exists" = false, NOT null
    }

    @Test
    @Timeout(2)
    void cfScanDumpAndLoadChunk() {
        redis.cfAdd("cuckoo-dump", MY_VALUE);

        long cursor = 0;
        while (true) {
            CfScanDumpValue chunkData = redis.cfScanDump("cuckoo-dump", cursor);
            cursor = chunkData.getIterator();
            if (cursor == 0L) {
                break;
            }
            assertThat(redis.cfLoadChunk("cuckoo-load", cursor, chunkData.getData())).isEqualTo("OK");
        }

        assertThat(redis.cfExists("cuckoo-load", MY_VALUE)).isTrue();
    }

}
