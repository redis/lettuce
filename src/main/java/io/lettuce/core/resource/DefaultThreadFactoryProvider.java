package io.lettuce.core.resource;

import java.util.concurrent.ThreadFactory;

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Default {@link ThreadFactoryProvider} implementation.
 *
 * @author Mark Paluch
 */
enum DefaultThreadFactoryProvider implements ThreadFactoryProvider {

    INSTANCE;

    @Override
    public ThreadFactory getThreadFactory(String poolName) {
        return new DefaultThreadFactory(poolName, true);
    }

}
