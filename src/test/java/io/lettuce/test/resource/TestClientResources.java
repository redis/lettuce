/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.test.resource;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/**
 * Client-Resources suitable for testing. Uses {@link TestEventLoopGroupProvider} to preserve the event loop groups between
 * tests. Every time a new {@link TestClientResources} instance is created, shutdown hook is added
 * {@link Runtime#addShutdownHook(Thread)}.
 *
 * @author Mark Paluch
 */
public class TestClientResources {

    private static final TestClientResources instance = new TestClientResources();

    private ClientResources clientResources = create();

    /**
     * @return the default {@link ClientResources} instance used across multiple tests. The returned instance must not be shut
     *         down.
     */
    public static ClientResources get() {
        return instance.clientResources;
    }

    /**
     * Creates a new {@link ClientResources} instance and registers a shutdown hook to de-allocate the instance upon JVM
     * shutdown.
     *
     * @return a new {@link ClientResources} instance.
     */
    public static ClientResources create() {

        final DefaultClientResources resources = DefaultClientResources.builder()
                .eventLoopGroupProvider(new TestEventLoopGroupProvider()).build();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    resources.shutdown(100, 100, TimeUnit.MILLISECONDS).get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        return resources;
    }

}
