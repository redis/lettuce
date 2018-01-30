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

/**
 * Programmatic command batching API.
 * <p>
 * {@link CommandBatching} is used to queue commands in a batch queue and flush the command queue on command invocation. Usage:
 *
 * <pre class="code">
 * public interface MyCommands extends Commands {
 *
 *   public void set(String key, String value, CommandBatching batching);
 *
 *   public RedisFuture&lt;String&gt; get(String key, CommandBatching batching)
 * }
 *
 * MyCommands commands = …
 *
 * commands.set("key", "value", CommandBatching.queue());
 * commands.get("key", CommandBatching.flush());
 * </pre>
 * <p>
 * Using {@link CommandBatching} in a method signature turns the command method into a batched command method.<br/>
 * Command batching executes commands in a deferred nature. This also means that at the time of invocation no result is
 * available. Batching can be only used with synchronous methods without a return value ({@code void}) or asynchronous methods
 * returning a {@link io.lettuce.core.RedisFuture}. Reactive command batching is not supported because reactive executed
 * commands maintain an own subscription lifecycle that is decoupled from command method batching.
 * <p>
 *
 * @author Mark Paluch
 * @since 5.0
 * @see BatchSize
 */
public abstract class CommandBatching {

    /**
     * Flush the command batch queue after adding a command to the batch queue.
     *
     * @return {@link CommandBatching} to flush the command batch queue after adding a command to the batch queue.
     */
    public static CommandBatching flush() {
        return FlushCommands.instance();
    }

    /**
     * Enqueue the command to the batch queue.
     *
     * @return {@link CommandBatching} to enqueue the command to the batch queue.
     */
    public static CommandBatching queue() {
        return QueueCommands.instance();
    }

    /**
     * {@link CommandBatching} to flush the command batch queue after adding a command to the batch queue.
     */
    static class FlushCommands extends CommandBatching {

        static final FlushCommands INSTANCE = new FlushCommands();

        private FlushCommands() {
        }

        /**
         * @return a static instance of {@link FlushCommands}.
         */
        public static CommandBatching instance() {
            return INSTANCE;
        }
    }

    /**
     * {@link CommandBatching} to enqueue the command to the batch queue.
     */
    static class QueueCommands extends CommandBatching {

        static final QueueCommands INSTANCE = new QueueCommands();

        private QueueCommands() {
        }

        /**
         * @return a static instance of {@link QueueCommands}.
         */
        public static QueueCommands instance() {
            return INSTANCE;
        }
    }
}
