package com.lambdaworks.redis.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is part of the internal API and may change without further notice.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
public class LettuceFactories {

    /**
     * Creates a new {@link Queue} that does not require external synchronization.
     * 
     * @param <T>
     * @return a new, empty {@link ConcurrentLinkedDeque}.
     */
    public final static <T> Queue<T> newConcurrentQueue() {
        return new ConcurrentLinkedDeque<T>();
    }

    /**
     * Creates a new {@link Queue} for single producer/single consumer.
     * 
     * @param <T>
     * @return a new, empty {@link ArrayDeque}.
     */
    public final static <T> Deque<T> newSpScQueue() {
        return new ArrayDeque<>();
    }

    /**
     * Creates a new {@link BlockingQueue}.
     * 
     * @param <E>
     * @return a new, empty {@link BlockingQueue}.
     */
    public static <E> BlockingQueue<E> newBlockingQueue() {
        return new LinkedBlockingQueue<>();
    }
}
