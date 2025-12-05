/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisClient;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.test.LettuceExtension;

/**
 * Integration tests simulating reactive stream errors similar to the Spring Data example. This test reproduces the issue where
 * OutOfMemoryError thrown during reactive operations can cause out-of-order responses and result mismatches.
 *
 * @author Tihomir Mateev
 * @see <a href="https://github.com/nordnet/lettuce-out-of-order">Lettuce OutOfOrder repo</a>
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag(INTEGRATION_TEST)
class ReactiveStreamErrorIntegrationTests extends TestSupport {

    private final StatefulRedisConnection<String, String> connection;

    private static final SecureRandom random = new SecureRandom();

    private static final String KEY_TO_WORK_ON = "lettuce:session-1234-4567-7890";

    @Inject
    ReactiveStreamErrorIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    @BeforeEach
    void setUp() {
        this.connection.sync().flushall();
    }

    @Test
    void errorSimulation() throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        AtomicInteger messageOutOfOrder = new AtomicInteger(0);

        RedisReactiveCommands<String, String> reactive = connection.reactive();

        Runnable readTask = () -> IntStream.range(0, 100).forEach(i -> {
            reactive.get(KEY_TO_WORK_ON).doOnNext(result -> {
                if (result.equals("OK")) {
                    messageOutOfOrder.getAndIncrement();
                }
            }).block(Duration.ofMillis(100));
        });

        Runnable writeTask = () -> IntStream.range(0, 100).forEach(i -> {
            String id = "value:" + i;
            reactive.set(KEY_TO_WORK_ON, id).doOnNext(result -> {

                if (random.nextBoolean()) {
                    throw new OutOfMemoryError("Simulated error");
                }
            }).block(Duration.ofMillis(100));
        });

        IntStream.range(0, 1000).forEach(i -> executorService.submit(readTask));
        IntStream.range(0, 1000).forEach(i -> executorService.submit(writeTask));

        executorService.shutdown();
        assertThat(executorService.awaitTermination(1, TimeUnit.MINUTES)).isTrue();

        Assertions.assertEquals(0, messageOutOfOrder.get());
    }

}
