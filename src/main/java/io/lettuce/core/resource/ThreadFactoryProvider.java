package io.lettuce.core.resource;

import java.util.concurrent.ThreadFactory;

/**
 * Interface to provide a custom {@link java.util.concurrent.ThreadFactory}. Implementations are asked through
 * {@link #getThreadFactory(String)} to provide a thread factory for a given pool name.
 *
 * @since 6.1.1
 */
@FunctionalInterface
public interface ThreadFactoryProvider {

    /**
     * Return a {@link ThreadFactory} for the given {@code poolName}.
     *
     * @param poolName a descriptive pool name. Typically used as prefix for thread names.
     * @return the {@link ThreadFactory}.
     */
    ThreadFactory getThreadFactory(String poolName);

}
