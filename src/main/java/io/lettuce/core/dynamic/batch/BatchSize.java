/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.dynamic.batch;

import java.lang.annotation.*;

/**
 * Redis command method annotation declaring a command interface to use batching with a specified {@code batchSize}.
 * <p>
 * Usage:
 *
 * <pre class="code">
 * &#64;BatchSize(50)
 * public interface MyCommands extends Commands {
 *
 *   public void set(String key, String value);
 *
 *   public RedisFuture&lt;String&gt; get(String key)
 * }
 * </pre>
 * <p>
 * Command batching executes commands in a deferred nature. This also means that at the time of invocation no result is
 * available. Batching can be only used with synchronous methods without a return value ({@code void}) or asynchronous methods
 * returning a {@link io.lettuce.core.RedisFuture}. Reactive command batching is not supported because reactive executed
 * commands maintain an own subscription lifecycle that is decoupled from command method batching.
 * <p>
 * Command methods participating in batching share a single batch queue. All method invocations are queued until reaching the
 * batch size. Command batching can be also specified using dynamic batching by providing a {@link CommandBatching} parameter on
 * each command invocation. {@link CommandBatching} parameters have precedence over {@link BatchSize} and can be used to enqueue
 * commands or force batch flushing of commands.
 * <p>
 * Alternatively, a command interface can implement {@link BatchExecutor} to {@link BatchExecutor#flush()} commands before the
 * batch size is reached. Commands remain in a batch queue until the batch size is reached or the queue is
 * {@link BatchExecutor#flush() flushed}. If the batch size is not reached, commands remain not executed.
 * <p>
 * Batching command interfaces are thread-safe and can be shared amongst multiple threads.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see CommandBatching
 * @see BatchExecutor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface BatchSize {

    /**
     * Declares the batch size for the command method.
     *
     * @return a positive, non-zero number of commands.
     */
    int value();
}
