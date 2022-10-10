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
 * Argument list builder for the Redis <a href="https://redis.io/commands/expire">EXPIRE</a> commands (PEXPIRE, EXPIREAT,
 * PEXPIREAT). Static import the methods from {@link ExpireArgs.Builder} and call the methods: {@code xx(…)} {@code gt(…)}.
 *
 * {@link ExpireArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 6.2
 */
public class ExpireArgs implements CompositeArgument {

    private boolean nx;

    private boolean xx;

    private boolean gt;

    private boolean lt;

    /**
     * Builder entry points for {@link ExpireArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ExpireArgs} and sets {@literal NX}.
         *
         * @return new {@link ExpireArgs} with {@literal NX} set.
         */
        public static ExpireArgs nx() {
            return new ExpireArgs().nx();
        }

        /**
         * Creates new {@link ExpireArgs} and sets {@literal XX}.
         *
         * @return new {@link ExpireArgs} with {@literal XX} set.
         */
        public static ExpireArgs xx() {
            return new ExpireArgs().xx();
        }

        /**
         * Creates new {@link ExpireArgs} and sets {@literal GT}.
         *
         * @return new {@link ExpireArgs} with {@literal GT} set.
         */
        public static ExpireArgs gt() {
            return new ExpireArgs().gt();
        }

        /**
         * Creates new {@link ExpireArgs} and sets {@literal LT}.
         *
         * @return new {@link ExpireArgs} with {@literal LT} set.
         */
        public static ExpireArgs lt() {
            return new ExpireArgs().lt();
        }

    }

    /**
     * Set expiry only when the key has no expiry.
     *
     * @return {@code this}.
     */
    public ExpireArgs nx() {

        this.nx = true;
        return this;
    }

    /**
     * Set expiry only when the key has an existing expiry.
     *
     * @return {@code this}.
     */
    public ExpireArgs xx() {

        this.xx = true;
        return this;
    }

    /**
     * Set expiry only when the new expiry is greater than current one.
     *
     * @return {@code this}.
     */
    public ExpireArgs gt() {

        this.gt = true;
        return this;
    }

    /**
     * Set expiry only when the new expiry is less than current one.
     *
     * @return {@code this}.
     */
    public ExpireArgs lt() {

        this.lt = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (xx) {
            args.add(CommandKeyword.XX);
        } else if (nx) {
            args.add(CommandKeyword.NX);
        }

        if (lt) {
            args.add("LT");
        } else if (gt) {
            args.add("GT");
        }
    }

}
