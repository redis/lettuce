package com.lambdaworks.redis;

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
}
