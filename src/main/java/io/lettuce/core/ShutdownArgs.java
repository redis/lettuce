/*
 * Copyright 2022 the original author or authors.
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
package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/shutdown">SHUTDOWN</a> command. Static import the
 * methods from {@link Builder} and call the methods: {@code now(…)} .
 * <p>
 * {@link ShutdownArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author dengliming
 * @since 6.2
 */
public class ShutdownArgs implements CompositeArgument {

    private boolean save;

    private boolean now;

    private boolean force;

    private boolean abort;

    /**
     * Builder entry points for {@link ShutdownArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ShutdownArgs} and setting {@literal SAVE}.
         *
         * @return new {@link ShutdownArgs} with {@literal SAVE} set.
         * @see ShutdownArgs#save(boolean)
         */
        public static ShutdownArgs save(boolean save) {
            return new ShutdownArgs().save(save);
        }

        /**
         * Creates new {@link ShutdownArgs} and setting {@literal NOW}.
         *
         * @return new {@link ShutdownArgs} with {@literal NOW} set.
         * @see ShutdownArgs#now()
         */
        public static ShutdownArgs now() {
            return new ShutdownArgs().now();
        }

        /**
         * Creates new {@link ShutdownArgs} and setting {@literal FORCE}.
         *
         * @return new {@link ShutdownArgs} with {@literal FORCE} set.
         * @see ShutdownArgs#force()
         */
        public static ShutdownArgs force() {
            return new ShutdownArgs().force();
        }

        /**
         * Creates new {@link ShutdownArgs} and setting {@literal ABORT}.
         *
         * @return new {@link ShutdownArgs} with {@literal ABORT} set.
         * @see ShutdownArgs#abort()
         */
        public static ShutdownArgs abort() {
            return new ShutdownArgs().abort();
        }
    }

    /**
     * Will force a DB saving operation even if no save points are configured.
     *
     * @param save {@code true} force save operation.
     * @return {@code this}
     */
    public ShutdownArgs save(boolean save) {
        this.save = save;
        return this;
    }

    /**
     * Skips waiting for lagging replicas, i.e. it bypasses the first step in the shutdown sequence.
     *
     * @return {@code this}
     */
    public ShutdownArgs now() {

        this.now = true;
        return this;
    }

    /**
     * Ignores any errors that would normally prevent the server from exiting.
     *
     * @return {@code this}
     */
    public ShutdownArgs force() {

        this.force = true;
        return this;
    }

    /**
     * Cancels an ongoing shutdown and cannot be combined with other flags.
     *
     * @return {@code this}
     */
    public ShutdownArgs abort() {

        this.abort = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (save) {
            args.add(CommandKeyword.SAVE);
        } else {
            args.add(CommandKeyword.NOSAVE);
        }

        if (now) {
            args.add("NOW");
        }
        if (force) {
            args.add(CommandKeyword.FORCE);
        }
        if (abort) {
            args.add("ABORT");
        }
    }

}
