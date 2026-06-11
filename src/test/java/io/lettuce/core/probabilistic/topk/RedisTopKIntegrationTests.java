/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.topk;

import io.lettuce.core.Pair;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.probabilistic.topk.arguments.TopKReserveArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.List;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisTopKCommands}.
 *
 * @author Yordan Tsintsov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TOPK.ADD")
public class RedisTopKIntegrationTests {

    protected static final String MY_KEY = "topk:name";

    protected static final String MY_VALUE = "Dune";

    protected static final String MY_VALUE_2 = "Dune Messiah";

    private final RedisCommands<String, String> redis;

    @Inject
    protected RedisTopKIntegrationTests(RedisCommands<String, String> redis) {
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
    void topKAdd() {
        redis.topKReserve(MY_KEY, 3);

        assertThat(redis.topKAdd(MY_KEY, MY_VALUE)).containsExactly((String) null);
        assertThat(redis.topKQuery(MY_KEY, MY_VALUE)).containsExactly(true);
    }

    @Test
    void topKAddVararg() {
        redis.topKReserve(MY_KEY, 3);

        assertThat(redis.topKAdd(MY_KEY, MY_VALUE, MY_VALUE_2)).containsExactly(null, null);
        assertThat(redis.topKQuery(MY_KEY, MY_VALUE, MY_VALUE_2)).containsExactly(true, true);
    }

    @Test
    void topKIncrBy() {
        redis.topKReserve(MY_KEY, 3);

        assertThat(redis.topKIncrBy(MY_KEY, new Pair<>(MY_VALUE, 3L))).containsExactly((String) null);
        assertThat(redis.topKQuery(MY_KEY, MY_VALUE)).containsExactly(true);
    }

    @Test
    void topKIncrByVararg() {
        redis.topKReserve(MY_KEY, 3);

        assertThat(redis.topKIncrBy(MY_KEY, new Pair<>(MY_VALUE, 3L), new Pair<>(MY_VALUE_2, 5L))).containsExactly(null, null);
        assertThat(redis.topKQuery(MY_KEY, MY_VALUE, MY_VALUE_2)).containsExactly(true, true);
    }

    @Test
    void topKInfo() {
        redis.topKReserve(MY_KEY, 50);

        TopKInfoValue info = redis.topKInfo(MY_KEY);
        assertThat(info.getK()).isEqualTo(50L);
        assertThat(info.getWidth()).isNotNull();
        assertThat(info.getDepth()).isNotNull();
        assertThat(info.getDecay()).isNotNull();
        assertThat(info.getRawInfo()).isNotEmpty();
    }

    @Test
    void topKList() {
        redis.topKReserve(MY_KEY, 3);
        redis.topKAdd(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(redis.topKList(MY_KEY)).contains(MY_VALUE, MY_VALUE_2);
    }

    @Test
    void topKListWithCount() {
        redis.topKReserve(MY_KEY, 3);
        redis.topKIncrBy(MY_KEY, new Pair<>(MY_VALUE, 3L));
        redis.topKAdd(MY_KEY, MY_VALUE_2);

        List<TopKListValue> list = redis.topKList(MY_KEY, true);
        assertThat(list.get(0).getName()).isEqualTo(MY_VALUE);
        assertThat(list.get(0).getCount()).isEqualTo(3);
        assertThat(list).extracting(TopKListValue::getName).contains(MY_VALUE, MY_VALUE_2);
    }

    @Test
    void topKListWithoutCount() {
        redis.topKReserve(MY_KEY, 3);
        redis.topKAdd(MY_KEY, MY_VALUE);

        List<TopKListValue> list = redis.topKList(MY_KEY, false);
        assertThat(list).extracting(TopKListValue::getName).contains(MY_VALUE);
        assertThat(list).allSatisfy(value -> assertThat(value.getCount()).isNull());
    }

    @Test
    void topKQuery() {
        redis.topKReserve(MY_KEY, 3);
        redis.topKAdd(MY_KEY, MY_VALUE);

        assertThat(redis.topKQuery(MY_KEY, MY_VALUE)).containsExactly(true);
        assertThat(redis.topKQuery(MY_KEY, "missing")).containsExactly(false);
    }

    @Test
    void topKQueryVararg() {
        redis.topKReserve(MY_KEY, 3);
        redis.topKAdd(MY_KEY, MY_VALUE);

        assertThat(redis.topKQuery(MY_KEY, MY_VALUE, "missing")).containsExactly(true, false);
    }

    @Test
    void topKReserve() {
        assertThat(redis.topKReserve(MY_KEY, 3)).isEqualTo("OK");

        assertThat(redis.topKInfo(MY_KEY).getK()).isEqualTo(3L);
    }

    @Test
    void topKReserveWithArgs() {
        TopKReserveArgs reserveArgs = TopKReserveArgs.Builder.width(8).depth(7).decay(0.9);

        assertThat(redis.topKReserve(MY_KEY, 3, reserveArgs)).isEqualTo("OK");

        TopKInfoValue info = redis.topKInfo(MY_KEY);
        assertThat(info.getK()).isEqualTo(3L);
        assertThat(info.getWidth()).isEqualTo(8L);
        assertThat(info.getDepth()).isEqualTo(7L);
        assertThat(info.getDecay()).isEqualTo(0.9);
    }

}
