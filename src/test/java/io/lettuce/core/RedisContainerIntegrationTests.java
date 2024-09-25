/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;

@Testcontainers
public class RedisContainerIntegrationTests {

    public static ComposeContainer CLUSTERED_STACK = new ComposeContainer(
            new File("src/test/resources/docker/docker-compose.yml")).withExposedService("clustered-stack", 36379)
                    .withExposedService("clustered-stack", 36380).withExposedService("clustered-stack", 36381)
                    .withExposedService("clustered-stack", 36382).withExposedService("clustered-stack", 36383)
                    .withExposedService("clustered-stack", 36384).withExposedService("standalone-stack", 6379);

    @BeforeAll
    public static void setup() throws IOException, InterruptedException {
        // In case you need to debug the container uncomment these lines to redirect the output
        CLUSTERED_STACK.withLogConsumer("clustered-stack", new SystemOutputConsumer("clustered"));
        CLUSTERED_STACK.withLogConsumer("standalone-stack", new SystemOutputConsumer("standalone"));

        CLUSTERED_STACK.waitingFor("clustered-stack",
                Wait.forLogMessage(".*Background RDB transfer terminated with success.*", 1));
        CLUSTERED_STACK.start();

    }

    static public class SystemOutputConsumer extends BaseConsumer<org.testcontainers.containers.output.ToStringConsumer> {

        String prefix;

        public SystemOutputConsumer(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void accept(OutputFrame outputFrame) {
            String output = String.format("%15s: %s", prefix, outputFrame.getUtf8String());
            System.out.print(output);
        }

    }

}
