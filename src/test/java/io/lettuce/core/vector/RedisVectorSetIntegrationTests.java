/*
 * Copyright 2024-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.vector;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.VAddArgs;
import io.lettuce.core.VSimArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.condition.RedisConditions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag(INTEGRATION_TEST)
public class RedisVectorSetIntegrationTests {

    private static final String MISSING_KEY = "test:missing";

    private static final String WRONG_KEY = "test:not-a-vector";

    private static final String VECTOR_SET_KEY = "test:vectors";

    private static final String ELEMENT1 = "item1";

    private static final String ELEMENT2 = "item2";

    private static final String ELEMENT3 = "item3";

    private static final Double[] VECTOR1 = new Double[] { 0.1, 0.2, 0.3 };

    private static final Double[] VECTOR2 = new Double[] { 0.2, 0.3, 0.4 };

    private static final Double[] VECTOR3 = new Double[] { 0.3, 0.4, 0.5 };

    private static final String JSON_ATTRS = "{\"category\":\"test\",\"price\":10.5}";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    protected static RedisAsyncCommands<String, String> asyncRedis;

    protected static RedisReactiveCommands<String, String> reactiveRedis;

    public RedisVectorSetIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        StatefulRedisConnection<String, String> connection = client.connect();
        redis = connection.sync();
        asyncRedis = connection.async();
        reactiveRedis = connection.reactive();
    }

    @BeforeEach
    public void prepare() {

        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.0"));

        redis.flushall();

        // Add some test vectors
        redis.vadd(VECTOR_SET_KEY, ELEMENT1, VECTOR1);
        redis.vadd(VECTOR_SET_KEY, ELEMENT2, VECTOR2);
        redis.vadd(VECTOR_SET_KEY, ELEMENT3, VECTOR3);

        redis.set(WRONG_KEY, "value");

    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    protected ClientOptions getOptions() {
        return ClientOptions.builder().build();
    }

    @Test
    void vadd() {
        // Test basic vadd
        Boolean result = redis.vadd(VECTOR_SET_KEY + ":new", "item4", 0.4, 0.5, 0.6);
        assertThat(result).isTrue();

        // Test vadd with dimensionality
        result = redis.vadd(VECTOR_SET_KEY + ":dim", 3, "item5", 0.5, 0.6, 0.7);
        assertThat(result).isTrue();

        // Test vadd with args
        VAddArgs args = new VAddArgs();
        args.quantizationType(QuantizationType.NO_QUANTIZATION);
        args.explorationFactor(200L);
        result = redis.vadd(VECTOR_SET_KEY + ":args", "item6", args, 0.6, 0.7, 0.8);
        assertThat(result).isTrue();

        // Test vadd with dimensionality and args
        result = redis.vadd(VECTOR_SET_KEY + ":dimargs", 3, "item7", args, 0.7, 0.8, 0.9);
        assertThat(result).isTrue();
    }

    @Test
    void vcard() {
        Long count = redis.vcard(VECTOR_SET_KEY);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void vcardMissingOrWrong() {
        Long count = redis.vcard(MISSING_KEY);
        assertThat(count).isEqualTo(0L);
        assertThrows(RedisCommandExecutionException.class, () -> redis.vcard(WRONG_KEY));
    }

    @Test
    void vdim() {
        Long dim = redis.vdim(VECTOR_SET_KEY);
        assertThat(dim).isEqualTo(3L);
    }

    @Test
    void vdimMissingOrWrong() {
        assertThrows(RedisCommandExecutionException.class, () -> redis.vdim(MISSING_KEY));
        assertThrows(RedisCommandExecutionException.class, () -> redis.vdim(WRONG_KEY));

    }

    @Test
    void vemb() {
        List<Double> vector = redis.vemb(VECTOR_SET_KEY, ELEMENT1);
        assertThat(vector).hasSize(3);
        // The values might be slightly different due to quantization
        assertThat(vector.get(0)).isCloseTo(VECTOR1[0], within(0.1));
        assertThat(vector.get(1)).isCloseTo(VECTOR1[1], within(0.1));
        assertThat(vector.get(2)).isCloseTo(VECTOR1[2], within(0.1));
    }

    @Test
    void vembMissingOrWrong() {
        List<Double> vector = redis.vemb(VECTOR_SET_KEY, MISSING_KEY);
        assertThat(vector).containsExactly((Double) null);
        vector = redis.vemb(MISSING_KEY, MISSING_KEY);
        assertThat(vector).containsExactly((Double) null);
        assertThrows(RedisCommandExecutionException.class, () -> redis.vemb(WRONG_KEY, MISSING_KEY));
    }

    @Test
    void vembRaw() {
        RawVector rawVector = redis.vembRaw(VECTOR_SET_KEY, ELEMENT1);
        assertThat(rawVector).isNotNull();
        assertThat(rawVector.getVector()).isNotNull();
    }

    @Test
    void vgetattr_vsetattr() {
        // First set attributes
        Boolean result = redis.vsetattr(VECTOR_SET_KEY, ELEMENT1, JSON_ATTRS);
        assertThat(result).isTrue();

        // Then get and verify attributes
        String attrs = redis.vgetattr(VECTOR_SET_KEY, ELEMENT1);
        assertThat(attrs).contains("category");
        assertThat(attrs).contains("test");
        assertThat(attrs).contains("price");
        assertThat(attrs).contains("10.5");
    }

    @Test
    void vgetattrMissingOrEmptyOrWrong() {
        String attrs = redis.vgetattr(VECTOR_SET_KEY, MISSING_KEY);
        assertThat(attrs).isNull();
        attrs = redis.vgetattr(VECTOR_SET_KEY, ELEMENT3);
        assertThat(attrs).isNull();
        attrs = redis.vgetattr(MISSING_KEY, ELEMENT3);
        assertThat(attrs).isNull();
        assertThrows(RedisCommandExecutionException.class, () -> redis.vgetattr(WRONG_KEY, MISSING_KEY));
    }

    @Test
    void vinfo() {
        VectorMetadata info = redis.vinfo(VECTOR_SET_KEY);
        assertThat(info).isNotNull();
        assertThat(info.getSize()).isEqualTo(3);
        assertThat(info.getDimensionality()).isEqualTo(3);
        assertThat(info.getType()).isEqualTo(QuantizationType.Q8);
        assertThat(info.getMaxNodeUid()).isNotNull();
        assertThat(info.getMaxNodes()).isEqualTo(16);
        assertThat(info.getvSetUid()).isNotNull();
        assertThat(info.getProjectionInputDim()).isEqualTo(0);
        assertThat(info.getAttributesCount()).isEqualTo(0);
    }

    @Test
    void vinfoMissingOrWrong() {
        VectorMetadata info = redis.vinfo(MISSING_KEY);
        assertThat(info).isNull();
        assertThrows(RedisCommandExecutionException.class, () -> redis.vinfo(WRONG_KEY));
    }

    @Test
    void vlinks() {
        // Add more vectors to create links
        redis.vadd(VECTOR_SET_KEY, "item4", 0.11, 0.21, 0.31);
        redis.vadd(VECTOR_SET_KEY, "item5", 0.12, 0.22, 0.32);

        List<String> links = redis.vlinks(VECTOR_SET_KEY, ELEMENT1);
        assertThat(links).isNotEmpty();
    }

    @Test
    void vlinksMissingOrWrong() {
        List<String> links = redis.vlinks(VECTOR_SET_KEY, MISSING_KEY);
        assertThat(links).isEmpty();
        links = redis.vlinks(MISSING_KEY, MISSING_KEY);
        assertThat(links).isEmpty();
        assertThrows(RedisCommandExecutionException.class, () -> redis.vlinks(WRONG_KEY, MISSING_KEY));
    }

    @Test
    void vlinksWithScores() {
        // Add more vectors to create links
        redis.vadd(VECTOR_SET_KEY, "item4", 0.11, 0.21, 0.31);
        redis.vadd(VECTOR_SET_KEY, "item5", 0.12, 0.22, 0.32);

        Map<String, Double> linksWithScores = redis.vlinksWithScores(VECTOR_SET_KEY, ELEMENT1);
        assertThat(linksWithScores).isNotEmpty();
        assertThat(linksWithScores.get(ELEMENT2)).isEqualTo(0.9964823722839355D);
        assertThat(linksWithScores.get(ELEMENT3)).isEqualTo(0.9919525384902954D);
        assertThat(linksWithScores.get("item4")).isEqualTo(1.0);
        assertThat(linksWithScores.get("item5")).isEqualTo(0.9997878074645996D);
    }

    @Test
    void vrandmember() {
        String randomElement = redis.vrandmember(VECTOR_SET_KEY);
        assertThat(randomElement).isNotNull();
        assertThat(Arrays.asList(ELEMENT1, ELEMENT2, ELEMENT3)).contains(randomElement);
    }

    @Test
    void vrandmemberWithCount() {
        List<String> randomElements = redis.vrandmember(VECTOR_SET_KEY, 2);
        assertThat(randomElements).hasSize(2);
        assertThat(randomElements).allMatch(element -> Arrays.asList(ELEMENT1, ELEMENT2, ELEMENT3).contains(element));
    }

    @Test
    void vrandmemberMissingOrWrong() {
        List<String> randomElements = redis.vrandmember(MISSING_KEY, 2);
        assertThat(randomElements).isEmpty();
        assertThrows(RedisCommandExecutionException.class, () -> redis.vrandmember(WRONG_KEY, 2));
    }

    @Test
    void vrem() {
        Boolean result = redis.vrem(VECTOR_SET_KEY, ELEMENT1);
        assertThat(result).isTrue();

        Long count = redis.vcard(VECTOR_SET_KEY);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void vremMissingOrWrong() {
        Boolean result = redis.vrem(VECTOR_SET_KEY, MISSING_KEY);
        assertThat(result).isFalse();
        result = redis.vrem(MISSING_KEY, MISSING_KEY);
        assertThat(result).isFalse();
        assertThrows(RedisCommandExecutionException.class, () -> redis.vrem(WRONG_KEY, MISSING_KEY));
    }

    @Test
    void setattrMissingOrWrong() {
        Boolean result = redis.vsetattr(MISSING_KEY, ELEMENT1, JSON_ATTRS);
        assertThat(result).isFalse();
        assertThrows(RedisCommandExecutionException.class, () -> redis.vsetattr(WRONG_KEY, ELEMENT1, "{malformed}"));
    }

    @Test
    void vsim() {
        // Test vsim with vector
        List<String> similar = redis.vsim(VECTOR_SET_KEY, 0.15, 0.25, 0.35);
        assertThat(similar).isNotEmpty();

        // Test vsim with element
        similar = redis.vsim(VECTOR_SET_KEY, VECTOR1);
        assertThat(similar).isNotEmpty();
        // The most similar element to ELEMENT1 should be ELEMENT2
        assertThat(similar.size()).isEqualTo(3);
        assertThat(similar.containsAll(Arrays.asList(ELEMENT1, ELEMENT2, ELEMENT3))).isTrue();

        // Test vsim with args
        VSimArgs args = new VSimArgs();
        args.count(2L);
        similar = redis.vsim(VECTOR_SET_KEY, args, 0.15, 0.25, 0.35);
        assertThat(similar).hasSize(2);

        // Test vsim with element and args
        similar = redis.vsim(VECTOR_SET_KEY, args, ELEMENT1);
        assertThat(similar).hasSize(2);
    }

    @Test
    void vsimWithScore() {
        // Test vsimWithScore with vector
        Map<String, Double> similarWithScores = redis.vsimWithScore(VECTOR_SET_KEY, 0.15, 0.25, 0.35);
        assertThat(similarWithScores).isNotEmpty();

        // Test vsimWithScore with element
        similarWithScores = redis.vsimWithScore(VECTOR_SET_KEY, ELEMENT1);
        assertThat(similarWithScores).isNotEmpty();

        // Test vsimWithScore with args
        VSimArgs args = new VSimArgs();
        args.count(2L);
        similarWithScores = redis.vsimWithScore(VECTOR_SET_KEY, args, 0.15, 0.25, 0.35);
        assertThat(similarWithScores).hasSize(2);

        // Test vsimWithScore with element and args
        similarWithScores = redis.vsimWithScore(VECTOR_SET_KEY, args, ELEMENT1);
        assertThat(similarWithScores).hasSize(2);
    }

    @Test
    void vsimMissingOrWrong() {
        List<String> similar = redis.vsim(MISSING_KEY, 0.15, 0.25, 0.35);
        assertThat(similar).isEmpty();
        assertThrows(RedisCommandExecutionException.class, () -> redis.vsim(VECTOR_SET_KEY, "missing"));
        assertThrows(RedisCommandExecutionException.class, () -> redis.vsim(WRONG_KEY, 0.15, 0.25, 0.35));
    }

    @Test
    void asyncVadd() throws ExecutionException, InterruptedException {
        RedisFuture<Boolean> future = asyncRedis.vadd(VECTOR_SET_KEY + ":async", "async1", 0.1, 0.2, 0.3);
        Boolean result = future.get();
        assertThat(result).isTrue();
    }

    @Test
    void reactiveVadd() {
        StepVerifier.create(reactiveRedis.vadd(VECTOR_SET_KEY + ":reactive", "reactive1", 0.1, 0.2, 0.3)).expectNext(true)
                .verifyComplete();
    }

    @Test
    void reactiveVsim() {
        StepVerifier.create(reactiveRedis.vsim(VECTOR_SET_KEY, VECTOR1).collectList()).assertNext(similar -> {
            assertThat(similar).isNotEmpty();
            assertThat(similar.size()).isEqualTo(3);
            assertThat(similar.containsAll(Arrays.asList(ELEMENT1, ELEMENT2, ELEMENT3))).isTrue();
        }).verifyComplete();
    }

}
