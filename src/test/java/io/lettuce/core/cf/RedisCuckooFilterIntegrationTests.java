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
import io.lettuce.core.probabilistic.CfInfoValue;
import io.lettuce.core.probabilistic.ScanDumpValue;
import io.lettuce.core.probabilistic.arguments.CfInsertArgs;
import io.lettuce.core.probabilistic.arguments.CfReserveArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisCuckooFilterCommands}.
 *
 * @author Gyumin Hwang
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("CF.ADD")
public class RedisCuckooFilterIntegrationTests {

    private static final String MY_KEY = "books:name";

    private static final String MY_VALUE = "Dune";

    private static final String MY_VALUE_2 = "Dune Messiah";

    protected final RedisCommands<String, String> redis;

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
    void cfInsertSingleValue() {
        List<Boolean> result = redis.cfInsert(MY_KEY, MY_VALUE);

        assertThat(result).containsExactly(true);
        assertThat(redis.cfExists(MY_KEY, MY_VALUE)).isTrue();
    }

    @Test
    void cfInsertNx() {
        redis.cfAdd(MY_KEY, MY_VALUE);

        List<Long> result = redis.cfInsertNx(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(result).containsExactly(0L, 1L);
    }

    @Test
    void cfInsertNxWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);
        redis.cfAdd(MY_KEY, MY_VALUE);

        List<Long> result = redis.cfInsertNx(MY_KEY, insertArgs, MY_VALUE, MY_VALUE_2);

        assertThat(result).containsExactly(0L, 1L);
    }

    @Test
    void cfInsertNxSingleValue() {
        List<Long> result = redis.cfInsertNx(MY_KEY, MY_VALUE);

        assertThat(result).containsExactly(1L);
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
     * Verifies that CF.INSERTNX returns {@code 0L} ("already exists") which is distinct from {@code -1L} ("filter full").
     */
    @Test
    void cfInsertNxDistinguishesAlreadyExistsFromFilterFull() {
        String key = "cf:full:insertnx";
        redis.cfReserve(key, 100, CfReserveArgs.Builder.bucketSize(2).expansion(1));

        redis.cfAdd(key, "known");

        List<Long> existing = redis.cfInsertNx(key, "known");
        assertThat(existing).hasSize(1);
        assertThat(existing.get(0)).isEqualTo(0L);
    }

    /**
     * Verifies that CF.INSERT returns {@code false} when the filter is full (server returns {@code -1}).
     *
     * <p>
     * BUCKETSIZE 1 EXPANSION 0: same value fills at most 2 slots, so inserting the same value 6 times causes filter-full
     * conditions. RESP2 and RESP3 both produce {@code false} for filter-full via
     * {@link io.lettuce.core.output.BooleanListOutput}.
     */
    @Test
    void cfInsertReturnsFalseWhenFilterIsFull() {
        String key = "cf:full:insert";
        redis.cfReserve(key, 1000, CfReserveArgs.Builder.bucketSize(1).expansion(0));

        List<Boolean> result = redis.cfInsert(key, "W", "W", "W", "W", "W", "W");

        assertThat(result).hasSize(6);
        assertThat(result).startsWith(true, true);
        for (int i = 2; i < result.size(); i++) {
            assertThat(result.get(i)).as("result[%d] must be false (filter full)", i).isEqualTo(Boolean.FALSE);
        }
    }

    @Test
    @Timeout(2)
    void cfScanDumpAndLoadChunk() {
        redis.cfAdd("cuckoo-dump", MY_VALUE);

        long cursor = 0;
        while (true) {
            ScanDumpValue chunkData = redis.cfScanDump("cuckoo-dump", cursor);
            cursor = chunkData.getIterator();
            if (cursor == 0L) {
                break;
            }
            assertThat(redis.cfLoadChunk("cuckoo-load", cursor, chunkData.getData())).isEqualTo("OK");
        }

        assertThat(redis.cfExists("cuckoo-load", MY_VALUE)).isTrue();
    }

}
