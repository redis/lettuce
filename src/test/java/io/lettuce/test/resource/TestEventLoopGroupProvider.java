package io.lettuce.test.resource;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.resource.DefaultEventLoopGroupProvider;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * A {@link io.lettuce.core.resource.EventLoopGroupProvider} suitable for testing. Preserves the event loop groups between
 * tests. Every time a new {@link TestEventLoopGroupProvider} instance is created, shutdown hook is added
 * {@link Runtime#addShutdownHook(Thread)}.
 *
 * @author Mark Paluch
 */
class TestEventLoopGroupProvider extends DefaultEventLoopGroupProvider {

    public TestEventLoopGroupProvider() {
        super(10);
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    TestEventLoopGroupProvider.this.shutdown(100, 100, TimeUnit.MILLISECONDS).get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }

    @Override
    public Promise<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {
        DefaultPromise<Boolean> result = new DefaultPromise<>(ImmediateEventExecutor.INSTANCE);
        result.setSuccess(true);

        return result;
    }

}
