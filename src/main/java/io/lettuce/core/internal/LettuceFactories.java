package io.lettuce.core.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is part of the internal API and may change without further notice.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public class LettuceFactories {

    /**
     * Threshold used to determine queue implementation. A queue size above the size indicates usage of
     * {@link LinkedBlockingQueue} otherwise {@link ArrayBlockingQueue}.
     */
    private static final int ARRAY_QUEUE_THRESHOLD = Integer
            .getInteger("io.lettuce.core.LettuceFactories.array-queue-threshold", 200000);

    /**
     * Creates a new, optionally bounded, {@link Queue} that does not require external synchronization.
     *
     * @param maxSize queue size. If {@link Integer#MAX_VALUE}, then creates an {@link ConcurrentLinkedQueue unbounded queue}.
     * @return a new, empty {@link Queue}.
     */
    public static <T> Queue<T> newConcurrentQueue(int maxSize) {

        if (maxSize == Integer.MAX_VALUE) {
            return new ConcurrentLinkedQueue<>();
        }

        return maxSize > ARRAY_QUEUE_THRESHOLD ? new LinkedBlockingQueue<>(maxSize) : new ArrayBlockingQueue<>(maxSize);
    }

    /**
     * Creates a new {@link Queue} for single producer/single consumer.
     *
     * @return a new, empty {@link ArrayDeque}.
     */
    public static <T> Deque<T> newSpScQueue() {
        return new ArrayDeque<>();
    }

    /**
     * Creates a new {@link BlockingQueue}.
     *
     * @return a new, empty {@link BlockingQueue}.
     */
    public static <T> LinkedBlockingQueue<T> newBlockingQueue() {
        return new LinkedBlockingQueue<>();
    }

}
