/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import javax.inject.Inject;
import java.util.List;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisCMSCommands}.
 *
 * @author Yordan Tsintsov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("CMS.INITBYDIM")
public class RedisCMSIntegrationTests {

    private static final String MY_KEY = "sketch";

    private static final String MY_ITEM = "item1";

    private static final String MY_ITEM_2 = "item2";

    protected final RedisCommands<String, String> redis;

    @Inject
    protected RedisCMSIntegrationTests(RedisCommands<String, String> redis) {
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
    void cmsInitByDim() {
        assertThat(redis.cmsInitByDim(MY_KEY, 2000, 5)).isEqualTo("OK");
    }

    @Test
    void cmsInitByProb() {
        assertThat(redis.cmsInitByProb(MY_KEY, 0.001, 0.01)).isEqualTo("OK");
    }

    @Test
    void cmsIncrBy() {
        redis.cmsInitByDim(MY_KEY, 2000, 5);

        List<Long> result = redis.cmsIncrBy(MY_KEY, IncrementPair.of(MY_ITEM, 5));

        assertThat(result).containsExactly(5L);
    }

    @Test
    void cmsIncrByMultiple() {
        redis.cmsInitByDim(MY_KEY, 2000, 5);

        List<Long> result = redis.cmsIncrBy(MY_KEY, IncrementPair.of(MY_ITEM, 5), IncrementPair.of(MY_ITEM_2, 3));

        assertThat(result).containsExactly(5L, 3L);
    }

    @Test
    void cmsQuery() {
        redis.cmsInitByDim(MY_KEY, 2000, 5);
        redis.cmsIncrBy(MY_KEY, IncrementPair.of(MY_ITEM, 5), IncrementPair.of(MY_ITEM_2, 3));

        List<Long> result = redis.cmsQuery(MY_KEY, MY_ITEM, MY_ITEM_2);

        assertThat(result).containsExactly(5L, 3L);
    }

    @Test
    void cmsInfo() {
        redis.cmsInitByDim(MY_KEY, 2000, 5);
        redis.cmsIncrBy(MY_KEY, IncrementPair.of(MY_ITEM, 5));

        CMSInfoValue result = redis.cmsInfo(MY_KEY);

        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(2000L);
        assertThat(result.getDepth()).isEqualTo(5L);
        assertThat(result.getCount()).isEqualTo(5L);
    }

    @Test
    void cmsMerge() {
        redis.cmsInitByDim("{cms}dest", 2000, 5);
        redis.cmsInitByDim("{cms}src", 2000, 5);
        redis.cmsIncrBy("{cms}src", IncrementPair.of(MY_ITEM, 5));

        assertThat(redis.cmsMerge("{cms}dest", "{cms}src")).isEqualTo("OK");
        assertThat(redis.cmsQuery("{cms}dest", MY_ITEM)).containsExactly(5L);
    }

    @Test
    void cmsMergeMultiple() {
        redis.cmsInitByDim("{cms}dest", 2000, 5);
        redis.cmsInitByDim("{cms}src1", 2000, 5);
        redis.cmsInitByDim("{cms}src2", 2000, 5);
        redis.cmsIncrBy("{cms}src1", IncrementPair.of(MY_ITEM, 5));
        redis.cmsIncrBy("{cms}src2", IncrementPair.of(MY_ITEM, 3));

        assertThat(redis.cmsMerge("{cms}dest", "{cms}src1", "{cms}src2")).isEqualTo("OK");
        assertThat(redis.cmsQuery("{cms}dest", MY_ITEM)).containsExactly(8L);
    }

    @Test
    void cmsMergeWithWeight() {
        redis.cmsInitByDim("{cms}dest", 2000, 5);
        redis.cmsInitByDim("{cms}src", 2000, 5);
        redis.cmsIncrBy("{cms}src", IncrementPair.of(MY_ITEM, 5));

        assertThat(redis.cmsMerge("{cms}dest", "{cms}src", 2)).isEqualTo("OK");
        assertThat(redis.cmsQuery("{cms}dest", MY_ITEM)).containsExactly(10L);
    }

    @Test
    void cmsMergeWithMergePairs() {
        redis.cmsInitByDim("{cms}dest", 2000, 5);
        redis.cmsInitByDim("{cms}src1", 2000, 5);
        redis.cmsInitByDim("{cms}src2", 2000, 5);
        redis.cmsIncrBy("{cms}src1", IncrementPair.of(MY_ITEM, 5));
        redis.cmsIncrBy("{cms}src2", IncrementPair.of(MY_ITEM, 3));

        assertThat(redis.cmsMerge("{cms}dest", MergePair.of("{cms}src1", 2), MergePair.of("{cms}src2", 3))).isEqualTo("OK");
        assertThat(redis.cmsQuery("{cms}dest", MY_ITEM)).containsExactly(19L);
    }

}
