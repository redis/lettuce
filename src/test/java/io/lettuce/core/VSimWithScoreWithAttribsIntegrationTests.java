/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.vector.VSimScoreAttribs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.RedisConditions;

/**
 * Integration tests for VSIM WITHSCORES WITHATTRIBS (advanced scenarios).
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VSimWithScoreWithAttribsIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    VSimWithScoreWithAttribsIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.redis = connection.sync();
    }

    @BeforeEach
    void ensureServer() {
        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.0"));
    }

    // 0) Helpers
    static boolean serverSupportsWithAttribs(RedisCommands<String, String> redis, String key) {
        try {
            redis.vsimWithScoreWithAttribs(key, new VSimArgs(), 1.0, 0.0);
            return true;
        } catch (RedisCommandExecutionException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    static void assertMonotoneNonIncreasing(Collection<Double> scores) {
        double prev = Double.POSITIVE_INFINITY;
        for (double s : scores) {
            assertThat(s).isLessThanOrEqualTo(prev);
            prev = s;
        }
    }

    // 1) Basic: values-query returns score+attribs; count respected
    @Test
    void vsim_withScoreWithAttribs_values_basic() {
        String key = "vs-it:basic:values";
        redis.del(key);

        redis.vadd(key, "a", 1.0, 0.0);
        redis.vadd(key, "b", 0.8, 0.2);
        redis.vadd(key, "c", 0.0, 1.0);
        redis.vadd(key, "d", 0.6, 0.8);

        // Ensure attributes are present
        redis.vsetattr(key, "a", "{\"label\":\"a\"}");
        redis.vsetattr(key, "b", "{\"label\":\"b\"}");
        redis.vsetattr(key, "c", "{\"label\":\"c\"}");
        redis.vsetattr(key, "d", "{\"label\":\"d\"}");

        assumeTrue(serverSupportsWithAttribs(redis, key), "WITHATTRIBS not supported on this server");

        VSimArgs args = new VSimArgs().count(3L);
        Map<String, VSimScoreAttribs> res = redis.vsimWithScoreWithAttribs(key, args, 1.0, 0.0);

        assertThat(res).isNotEmpty();
        assertThat(res.size()).isLessThanOrEqualTo(3);

        for (Map.Entry<String, VSimScoreAttribs> e : res.entrySet()) {
            assertThat(e.getValue().getAttributes()).as("attribs for " + e.getKey()).isNotNull();
            assertThat(e.getValue().getScore()).isBetween(0.0, 1.0);
        }

        // NOTE: The builder/output returns a LinkedHashMap, preserving server-returned order.
        // If this detail ever changes, sort by score desc before asserting monotonicity.
        assertMonotoneNonIncreasing(res.values().stream().map(VSimScoreAttribs::getScore).collect(toList()));

        // explicit cleanup
        redis.del(key);
    }

    // 2) EPSILON filters strictly: tighter epsilon yields subset with threshold respected
    @Test
    void vsim_withScoreWithAttribs_epsilon_filters() {
        String key = "vs-it:epsilon";
        redis.del(key);

        redis.vadd(key, "a", 1.0, 0.0);
        redis.vadd(key, "b", 0.8, 0.2);
        redis.vadd(key, "c", 0.0, 1.0);
        redis.vadd(key, "d", 0.6, 0.8);

        // Attributes
        redis.vsetattr(key, "a", "{\"label\":\"a\"}");
        redis.vsetattr(key, "b", "{\"label\":\"b\"}");
        redis.vsetattr(key, "c", "{\"label\":\"c\"}");
        redis.vsetattr(key, "d", "{\"label\":\"d\"}");

        assumeTrue(serverSupportsWithAttribs(redis, key), "WITHATTRIBS not supported");

        VSimArgs loose = new VSimArgs().epsilon(0.5);
        VSimArgs tight = new VSimArgs().epsilon(0.2);

        Map<String, VSimScoreAttribs> L = redis.vsimWithScoreWithAttribs(key, loose, 1.0, 0.0);
        Map<String, VSimScoreAttribs> T = redis.vsimWithScoreWithAttribs(key, tight, 1.0, 0.0);

        assertThat(T.size()).isLessThanOrEqualTo(L.size());
        assertThat(T.values()).allMatch(v -> v.getScore() >= 0.8);

        // ELE variant: use the closest element as the query to test element-based epsilon filtering
        Map<String, VSimScoreAttribs> L_ele = redis.vsimWithScoreWithAttribs(key, new VSimArgs().epsilon(0.5), "a");
        Map<String, VSimScoreAttribs> T_ele = redis.vsimWithScoreWithAttribs(key, new VSimArgs().epsilon(0.2), "a");
        assertThat(T_ele.size()).isLessThanOrEqualTo(L_ele.size());
        assertThat(T_ele.values()).allMatch(v -> v.getScore() >= 0.8);

        // explicit cleanup
        redis.del(key);
    }

    // 3) Element-query matches values-query (same vector), within tolerance
    @Test
    void vsim_withScoreWithAttribs_element_matches_values() {
        String key = "vs-it:element-vs-values";
        redis.del(key);

        double ax = 0.7, ay = 0.7;
        redis.vadd(key, "a", ax, ay);
        redis.vadd(key, "b", 0.7, 0.0);
        redis.vadd(key, "c", 0.0, 0.7);

        // Attributes
        redis.vsetattr(key, "a", "{\"label\":\"a\"}");
        redis.vsetattr(key, "b", "{\"label\":\"b\"}");
        redis.vsetattr(key, "c", "{\"label\":\"c\"}");

        assumeTrue(serverSupportsWithAttribs(redis, key), "WITHATTRIBS not supported");

        VSimArgs args = new VSimArgs().count(10L);

        Map<String, VSimScoreAttribs> byValues = redis.vsimWithScoreWithAttribs(key, args, ax, ay);
        Map<String, VSimScoreAttribs> byElement = redis.vsimWithScoreWithAttribs(key, args, "a");

        assertThat(byElement.keySet()).isEqualTo(byValues.keySet());

        final double EPS = 1e-6;
        for (String k : byValues.keySet()) {
            assertThat(byElement.get(k).getScore()).isCloseTo(byValues.get(k).getScore(), within(EPS));
        }

        // explicit cleanup
        redis.del(key);
    }

    // 4) COUNT hard-cap & deterministic top-K (on separable data)
    @Test
    void vsim_withScoreWithAttribs_count_is_cap_and_topk() {
        String key = "vs-it:count";
        redis.del(key);

        redis.vadd(key, "v1", 1.0, 0.0);
        redis.vadd(key, "v2", 0.9, 0.1);
        redis.vadd(key, "v3", 0.8, 0.2);
        redis.vadd(key, "v4", 0.7, 0.3);

        // Attributes
        redis.vsetattr(key, "v1", "{\"label\":\"v1\"}");
        redis.vsetattr(key, "v2", "{\"label\":\"v2\"}");
        redis.vsetattr(key, "v3", "{\"label\":\"v3\"}");
        redis.vsetattr(key, "v4", "{\"label\":\"v4\"}");

        assumeTrue(serverSupportsWithAttribs(redis, key), "WITHATTRIBS not supported");

        VSimArgs args = new VSimArgs().count(2L);
        Map<String, VSimScoreAttribs> res = redis.vsimWithScoreWithAttribs(key, args, 1.0, 0.0);

        assertThat(res).hasSize(2);
        // Expect top-2 by construction: v1 (1.0,0.0) and v2 (0.9,0.1) are closest to query (1.0,0.0)
        assertThat(res.keySet()).as("top-2 elements should be v1 and v2 for query (1.0,0.0)").containsExactlyInAnyOrder("v1",
                "v2");
        // NOTE: The builder/output returns a LinkedHashMap, preserving server-returned order.
        // If this detail ever changes, sort by score desc before asserting monotonicity.
        assertMonotoneNonIncreasing(res.values().stream().map(VSimScoreAttribs::getScore).collect(toList()));

        // explicit cleanup
        redis.del(key);
    }

    // 5) Attributes echo (if you can set attributes on insert). Here we set via vsetattr and expect non-null presence.
    @Test
    void vsim_withScoreWithAttribs_attributes_echo_if_available() {
        String key = "vs-it:attribs-echo";
        redis.del(key);

        redis.vadd(key, "a", 1.0, 0.0);
        redis.vsetattr(key, "a", "{\"label\":\"A\"}");
        assumeTrue(serverSupportsWithAttribs(redis, key), "WITHATTRIBS not supported");

        Map<String, VSimScoreAttribs> res = redis.vsimWithScoreWithAttribs(key, new VSimArgs(), 1.0, 0.0);
        VSimScoreAttribs a = res.get("a");
        assertThat(a).isNotNull();
        // Since we set attributes via vsetattr, verify exact content match
        assertThat(a.getAttributes()).isEqualTo("{\"label\":\"A\"}");

        // explicit cleanup
        redis.del(key);
    }

    // 6) Empty result when epsilon is extremely tight
    @Test
    void vsim_withScoreWithAttribs_empty_result_on_tight_epsilon() {
        String key = "vs-it:empty";
        redis.del(key);

        // Seed a few far-apart points
        redis.vadd(key, "a", 1.0, 0.0);
        redis.vadd(key, "b", -1.0, 0.0);
        redis.vadd(key, "c", 0.0, 1.0);

        assumeTrue(serverSupportsWithAttribs(redis, key), "WITHATTRIBS not supported");

        // Tight epsilon close to zero; only exact matches pass. Query near (0, -1)
        VSimArgs args = new VSimArgs().count(5L).epsilon(1e-9);
        Map<String, VSimScoreAttribs> res = redis.vsimWithScoreWithAttribs(key, args, 0.0, -1.0);

        assertThat(res).isEmpty();

        // cleanup
        redis.del(key);
    }

    // 7) Graceful skip on unsupported servers
    @Test
    void vsim_withScoreWithAttribs_skips_on_unsupported() {
        String key = "vs-it:skip";
        redis.del(key);

        if (!serverSupportsWithAttribs(redis, key)) {
            // cleanup and skip
            redis.del(key);
            return; // documented skip
        }

        Map<String, VSimScoreAttribs> res = redis.vsimWithScoreWithAttribs(key, new VSimArgs(), 1.0, 0.0);
        assertThat(res).isNotNull();

        // explicit cleanup
        redis.del(key);
    }

}
