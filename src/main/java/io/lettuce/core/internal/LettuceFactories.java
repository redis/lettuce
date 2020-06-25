/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
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
     * Threshold used to determine queue implementation. A queue size above the size indicates usage of
     * {@link LinkedBlockingQueue} otherwise {@link ArrayBlockingQueue}.
     */
    private static final int ARRAY_QUEUE_THRESHOLD = Integer
            .getInteger("io.lettuce.core.LettuceFactories.array-queue-threshold", 200000);

    /**
     * Creates a new, optionally bounded, {@link Queue} that does not require external synchronization.
     *
     * @param maxSize queue size. If {@link Integer#MAX_VALUE}, then creates an {@link ConcurrentLinkedDeque unbounded queue}.
     * @return a new, empty {@link Queue}.
     */
    public static <T> Queue<T> newConcurrentQueue(int maxSize) {

        if (maxSize == Integer.MAX_VALUE) {
            return new ConcurrentLinkedDeque<>();
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
