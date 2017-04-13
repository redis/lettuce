/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.internal;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;
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
     * @return a new, empty {@link LinkedBlockingDeque}.
     */
    public static <T> Deque<T> newConcurrentQueue() {
        return new FastCountingDeque<>(new ConcurrentLinkedDeque<>());
    }

    /**
     * Creates a new {@link Collection} that does not require external synchronization optimized for adding and removing
     * elements.
     *
     * @param <T>
     * @return a new, empty {@link LinkedBlockingDeque}.
     */
    public static <T> Collection<T> newConcurrentCollection() {
        return new ConcurrentLinkedDeque<>();
    }

    /**
     * Creates a new {@link Queue} for single producer/single consumer.
     *
     * @param <T>
     * @return a new, empty {@link ArrayDeque}.
     */
    public static <T> Deque<T> newSpScQueue() {
        return new ArrayDeque<>();
    }

    /**
     * Creates a new {@link BlockingQueue}.
     *
     * @param <T>
     * @return a new, empty {@link BlockingQueue}.
     */
    public static <T> LinkedBlockingQueue<T> newBlockingQueue() {
        return new LinkedBlockingQueue<>();
    }
}
