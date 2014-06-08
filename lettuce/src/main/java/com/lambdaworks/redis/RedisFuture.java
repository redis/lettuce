package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Redis Future, extends a Listenable Future (Notification on Complete). The execution of the notification happens either on
 * finish of the future execution or, if the future is completed already, immediately.
 * 
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 16.05.14 21:38
 */
public interface RedisFuture<V> extends ListenableFuture<V> {

    /**
     * 
     * @return error text, if any error occured.
     */
    String getError();

    /**
     * Wait up to the specified time for the command output to become available.
     *
     * @param timeout Maximum time to wait for a result.
     * @param unit Unit of time for the timeout.
     *
     * @return true if the output became available.
     */
    boolean await(long timeout, TimeUnit unit);
}
