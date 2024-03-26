package io.lettuce.test.resource;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/**
 * Client-Resources suitable for testing. Uses {@link TestEventLoopGroupProvider} to preserve the event loop
 * groups between tests. Every time a new {@link TestClientResources} instance is created, shutdown hook is added
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
