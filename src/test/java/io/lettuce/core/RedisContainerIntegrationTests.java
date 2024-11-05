/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;

@Testcontainers
public class RedisContainerIntegrationTests {

    private static final Logger LOGGER = LogManager.getLogger(RedisContainerIntegrationTests.class);

    private static final String REDIS_STACK_STANDALONE = "standalone-stack";

    private static final String REDIS_STACK_CLUSTER = "clustered-stack";

    public static ComposeContainer CLUSTERED_STACK = new ComposeContainer(
            new File("src/test/resources/docker/docker-compose.yml")).withExposedService(REDIS_STACK_CLUSTER, 36379)
                    .withExposedService(REDIS_STACK_CLUSTER, 36380).withExposedService(REDIS_STACK_CLUSTER, 36381)
                    .withExposedService(REDIS_STACK_CLUSTER, 36382).withExposedService(REDIS_STACK_CLUSTER, 36383)
                    .withExposedService(REDIS_STACK_CLUSTER, 36384).withExposedService(REDIS_STACK_STANDALONE, 6379)
                    .withLocalCompose(true);

    @BeforeAll
    public static void setup() throws IOException, InterruptedException {
        // In case you need to debug the container uncomment these lines to redirect the output
        CLUSTERED_STACK.withLogConsumer(REDIS_STACK_CLUSTER, (OutputFrame frame) -> LOGGER.debug(frame.getUtf8String()));
        CLUSTERED_STACK.withLogConsumer(REDIS_STACK_STANDALONE, (OutputFrame frame) -> LOGGER.debug(frame.getUtf8String()));

        CLUSTERED_STACK.waitingFor(REDIS_STACK_CLUSTER,
                Wait.forLogMessage(".*Background RDB transfer terminated with success.*", 1));
        CLUSTERED_STACK.start();
    }

}
