/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.bf;

import javax.inject.Inject;
import java.util.List;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.probabilistic.BfInfoValue;
import io.lettuce.core.probabilistic.ScanDumpValue;
import io.lettuce.core.probabilistic.arguments.BfInsertArgs;
import io.lettuce.core.probabilistic.arguments.BfReserveArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisBloomFilterCommands}.
 *
 * @author Yordan Tsintsov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("BF.ADD")
public class RedisBloomFilterIntegrationTests {

    private static final String MY_KEY = "books:name";

    private static final String MY_VALUE = "Dune";

    private static final String MY_VALUE_2 = "Dune Messiah";

    private final RedisCommands<String, String> redis;

    @Inject
    protected RedisBloomFilterIntegrationTests(RedisCommands<String, String> redis) {
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
    void bfAdd() {
        Boolean result = redis.bfAdd(MY_KEY, MY_VALUE);

        assertThat(result).isTrue();
        assertThat(redis.bfExists(MY_KEY, MY_VALUE)).isTrue();
    }

    @Test
    void bfCard() {
        redis.bfAdd(MY_KEY, MY_VALUE);
        redis.bfAdd(MY_KEY, MY_VALUE_2);

        Long result = redis.bfCard(MY_KEY);

        assertThat(result).isEqualTo(2L);
    }

    @Test
    void bfCardOnMissingKey() {
        assertThat(redis.bfCard("not_exist")).isEqualTo(0L);
    }

    @Test
    void bfExists() {
        redis.bfAdd(MY_KEY, MY_VALUE);

        Boolean result = redis.bfExists(MY_KEY, MY_VALUE);

        assertThat(result).isTrue();
    }

    @Test
    void bfInfo() {
        redis.bfAdd(MY_KEY, MY_VALUE);

        BfInfoValue result = redis.bfInfo(MY_KEY);

        assertThat(result.getCapacity()).isEqualTo(100L);
    }

    @Test
    void bfInsert() {
        redis.bfInsert(MY_KEY, MY_VALUE);

        assertThat(redis.bfExists(MY_KEY, MY_VALUE)).isTrue();
    }

    @Test
    void bfInsertWithArgs() {
        BfInsertArgs insertArgs = BfInsertArgs.Builder.capacity(100).error(0.01);
        redis.bfInsert(MY_KEY, insertArgs, MY_VALUE);

        BfInfoValue result = redis.bfInfo(MY_KEY);

        assertThat(redis.bfExists(MY_KEY, MY_VALUE)).isTrue();
        assertThat(result.getCapacity()).isEqualTo(100L);
    }

    @Test
    void bfInsertVararg() {
        redis.bfInsert(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(redis.bfExists(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.bfExists(MY_KEY, MY_VALUE_2)).isTrue();
    }

    @Test
    void bfInsertVarargWithArgs() {
        BfInsertArgs insertArgs = BfInsertArgs.Builder.capacity(100).error(0.01);
        redis.bfInsert(MY_KEY, insertArgs, MY_VALUE, MY_VALUE_2);
        BfInfoValue result = redis.bfInfo(MY_KEY);

        assertThat(redis.bfExists(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.bfExists(MY_KEY, MY_VALUE_2)).isTrue();
        assertThat(result.getCapacity()).isEqualTo(100L);
        assertThat(result.getNumberOfItemsInserted()).isEqualTo(2L);
    }

    @Test
    void bfInsertNoCreate() {
        BfInsertArgs insertArgs = BfInsertArgs.Builder.noCreate();

        assertThatThrownBy(() -> redis.bfInsert(MY_KEY, insertArgs, MY_VALUE))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessage("ERR not found");
    }

    @Test
    void bfInsertNonScaling() {
        BfInsertArgs insertArgs = BfInsertArgs.Builder.capacity(4).nonScaling();

        List<Boolean> insert = redis.bfInsert("nonscaling_err", insertArgs, "a", "b", "c");

        assertThat(insert).containsExactly(true, true, true);
        assertThat(redis.bfInsert("nonscaling_err", "d", "e")).containsExactly(true, null);
    }

    @Test
    void bfMAdd() {
        redis.bfMAdd(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(redis.bfExists(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.bfExists(MY_KEY, MY_VALUE_2)).isTrue();
    }

    @Test
    void bfMExists() {
        redis.bfMAdd(MY_KEY, MY_VALUE, MY_VALUE_2);

        List<Boolean> result = redis.bfMExists(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(result).containsExactly(true, true);
    }

    @Test
    void bfReserve() {
        redis.bfReserve(MY_KEY, 0.01, 100);

        BfInfoValue result = redis.bfInfo(MY_KEY);

        assertThat(result.getCapacity()).isEqualTo(100L);
    }

    @Test
    void bfReserveValidateZeroCapacity() {
        assertThatThrownBy(() -> redis.bfReserve(MY_KEY, 0.01, 0)).isInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    void bfReserveValidateZeroErrorRate() {
        assertThatThrownBy(() -> redis.bfReserve(MY_KEY, 0, 100)).isInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    void bfReserveAlreadyExists() {
        redis.bfReserve(MY_KEY, 0.1, 100);

        assertThatThrownBy(() -> redis.bfReserve(MY_KEY, 0.1, 100)).isInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    void bfReserveNonScaling() {
        redis.bfReserve("nonscaling", 0.001, 2, BfReserveArgs.Builder.nonScaling());

        assertThat(redis.bfInsert("nonscaling", "a")).containsExactly(true);
        assertThat(redis.bfInsert("nonscaling", "b")).containsExactly(true);
        assertThat(redis.bfInsert("nonscaling", "c")).containsExactly((Boolean) null);
    }

    @Test
    @Timeout(2)
    void bfScanDumpAndLoadChunk() {
        redis.bfAdd("bloom-dump", MY_VALUE);

        long iterator = 0;
        while (true) {
            ScanDumpValue chunkData = redis.bfScanDump("bloom-dump", iterator);
            iterator = chunkData.getIterator();
            if (iterator == 0L) {
                break;
            }
            assertThat(redis.bfLoadChunk("bloom-load", iterator, chunkData.getData())).isEqualTo("OK");
        }

        assertThat(redis.bfInfo("bloom-load").getRawInfo()).isEqualTo(redis.bfInfo("bloom-dump").getRawInfo());
        assertThat(redis.bfExists("bloom-load", MY_VALUE)).isTrue();
    }

}
