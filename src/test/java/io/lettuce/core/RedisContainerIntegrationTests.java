/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.arguments.JsonSetArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Testcontainers
public class RedisContainerIntegrationTests {

    @Container
    public GenericContainer redisContainer = new GenericContainer(image).withExposedPorts(6379).withReuse(true);;

    private static DockerImageName image = DockerImageName.parse("redis/redis-stack:latest");

    private static RedisClient client;

    protected static RedisCommands<String, String> redis;

    @BeforeEach
    public void prepare() throws IOException {
        if (!redisContainer.isRunning()) {
            redisContainer.start();
        }

        String address = redisContainer.getHost();
        Integer port = redisContainer.getFirstMappedPort();
        RedisURI redisURI = RedisURI.Builder.redis(address).withPort(port).build();

        client = RedisClient.create(redisURI);
        redis = client.connect().sync();

        redis.flushall();

        Path path = Paths.get("src/test/resources/bike-inventory.json");
        String read = String.join("", Files.readAllLines(path));
        JsonValue<String, String> value = redis.getJsonParser().createJsonValue(read);

        redis.jsonSet("bikes:inventory", JsonPath.ROOT_PATH, value, JsonSetArgs.Builder.none());
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

}
