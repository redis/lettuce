/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisContainerIntegrationTests;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.search.Field;
import io.lettuce.core.search.Fields;
import io.lettuce.core.search.arguments.CreateArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
public class RediSearchIntegrationTests extends RedisContainerIntegrationTests {

    private static final String GENERIC_INDEX = "idx";

    private static final String FIELD1_NAME = "title";

    private static final String FIELD2_NAME = "published_at";

    private static final String FIELD3_NAME = "category";

    private static final String PREFIX = "blog:post:";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    public RediSearchIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        client = RedisClient.create(redisURI);
        redis = client.connect().sync();
    }

    @BeforeEach
    public void prepare() throws IOException {
        redis.flushall();

        Path path = Paths.get("src/test/resources/bike-inventory.json");
        String read = String.join("", Files.readAllLines(path));
        JsonValue value = redis.getJsonParser().createJsonValue(read);

        redis.jsonSet("bikes:inventory", JsonPath.ROOT_PATH, value);
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void ftCreateScenario1() {
        Field<String> field1 = Field.<String> builder().name(FIELD1_NAME).type(Field.Type.TEXT).sortable().build();
        Field<String> field2 = Field.<String> builder().name(FIELD2_NAME).type(Field.Type.NUMERIC).sortable().build();
        Field<String> field3 = Field.<String> builder().name(FIELD3_NAME).type(Field.Type.TAG).sortable().build();
        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().addPrefix(PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        String result = redis.ftCreate(GENERIC_INDEX, createArgs, Fields.from(field1, field2, field3));
        assertThat(result).isEqualTo("OK");
    }

}
