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
package io.lettuce.core.internal;

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
    public static final <T> Deque<T> newConcurrentQueue() {
        return new ConcurrentLinkedDeque<T>();
    }

    /**
     * Creates a new {@link Queue} for single producer/single consumer.
     *
     * @param <T>
     * @return a new, empty {@link ArrayDeque}.
     */
    public static final <T> Deque<T> newSpScQueue() {
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
