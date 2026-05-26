/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.bf;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.bf.arguments.BfInsertArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
public class RedisBloomFilterIntegrationTests {

    private static final String MY_KEY = "books:name";

    private static final String MY_VALUE = "Dune";

    private static final String MY_VALUE_2 = "Dune Messiah";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    public RedisBloomFilterIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        client = RedisClient.create(redisURI);
        redis = client.connect().sync();
    }

    @BeforeEach
    public void prepare() throws IOException {
        redis.flushall();
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
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
    void bfInsertVararg() {
        redis.bfInsert(MY_KEY, MY_VALUE, MY_VALUE_2);

        assertThat(redis.bfExists(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.bfExists(MY_KEY, MY_VALUE_2)).isTrue();
    }

    @Test
    void bfInsertWithArgs() {
        BfInsertArgs insertArgs = BfInsertArgs.Builder.capacity(100).error(0.01);
        redis.bfInsert(MY_KEY, insertArgs, MY_VALUE, MY_VALUE_2);
        BfInfoValue result = redis.bfInfo(MY_KEY);

        assertThat(redis.bfExists(MY_KEY, MY_VALUE)).isTrue();
        assertThat(redis.bfExists(MY_KEY, MY_VALUE_2)).isTrue();
        assertThat(result.getCapacity()).isEqualTo(100L);
        assertThat(result.getItems()).isEqualTo(2L);
    }

    @Test
    void bfLoadChunk() {
        redis.bfAdd(MY_KEY, MY_VALUE);
        BfScanDumpValue header = redis.bfScanDump(MY_KEY, 0);
        BfScanDumpValue payload = redis.bfScanDump(MY_KEY, header.getIterator());

        assertThat(redis.bfLoadChunk("books:restored", header.getIterator(), header.getData())).isEqualTo("OK");
        assertThat(redis.bfLoadChunk("books:restored", payload.getIterator(), payload.getData())).isEqualTo("OK");
        assertThat(redis.bfExists("books:restored", MY_VALUE)).isTrue();
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
    void bfScanDump() {
        redis.bfReserve(MY_KEY, 0.01, 100);
        redis.bfAdd(MY_KEY, MY_VALUE);

        BfScanDumpValue result = redis.bfScanDump(MY_KEY, 0);

        assertThat(result.getIterator()).isEqualTo(1L);
    }

}
