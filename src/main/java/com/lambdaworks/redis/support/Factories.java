package com.lambdaworks.redis.support;

import io.netty.util.internal.PlatformDependent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.2
 */
public class Factories {

    /**
     * Creates a new {@link Queue} that does not require external synchronization.
     * 
     * @param <T>
     * @return
     */
    public final static <T> Queue<T> newConcurrentQueue() {
        return PlatformDependent.newConcurrentDeque();
    }
}
