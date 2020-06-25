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
package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/set">SET</a> command starting from Redis 2.6.12. Static
 * import the methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
 * <p>
 * {@link SetArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Will Glozer
 * @author Vincent Rischmann
 * @author Mark Paluch
 */
public class SetArgs implements CompositeArgument {

    private Long ex;

    private Long px;

    private boolean nx = false;

    private boolean xx = false;

    private boolean keepttl = false;

    /**
     * Builder entry points for {@link SetArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SetArgs} and enabling {@literal EX}.
         *
         * @param timeout expire time in seconds.
         * @return new {@link SetArgs} with {@literal EX} enabled.
         * @see SetArgs#ex(long)
         */
        public static SetArgs ex(long timeout) {
            return new SetArgs().ex(timeout);
        }

        /**
         * Creates new {@link SetArgs} and enabling {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link SetArgs} with {@literal PX} enabled.
         * @see SetArgs#px(long)
         */
        public static SetArgs px(long timeout) {
            return new SetArgs().px(timeout);
        }

        /**
         * Creates new {@link SetArgs} and enabling {@literal NX}.
         *
         * @return new {@link SetArgs} with {@literal NX} enabled.
         * @see SetArgs#nx()
         */
        public static SetArgs nx() {
            return new SetArgs().nx();
        }

        /**
         * Creates new {@link SetArgs} and enabling {@literal XX}.
         *
         * @return new {@link SetArgs} with {@literal XX} enabled.
         * @see SetArgs#xx()
         */
        public static SetArgs xx() {
            return new SetArgs().xx();
        }

        /**
         * Creates new {@link SetArgs} and enabling {@literal KEEPTTL}.
         *
         * @return new {@link SetArgs} with {@literal KEEPTTL} enabled.
         * @see SetArgs#keepttl()
         * @since 5.3
         */
        public static SetArgs keepttl() {
            return new SetArgs().keepttl();
        }

    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs ex(long timeout) {

        this.ex = timeout;
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs px(long timeout) {

        this.px = timeout;
        return this;
    }

    /**
     * Only set the key if it does not already exist.
     *
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs nx() {

        this.nx = true;
        return this;
    }

    /**
     * Set the value and retain the existing TTL.
     *
     * @return {@code this} {@link SetArgs}.
     * @since 5.3
     */
    public SetArgs keepttl() {

        this.keepttl = true;
        return this;
    }

    /**
     * Only set the key if it already exists.
     *
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs xx() {

        this.xx = true;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (ex != null) {
            args.add("EX").add(ex);
        }

        if (px != null) {
            args.add("PX").add(px);
        }

        if (nx) {
            args.add("NX");
        }

        if (xx) {
            args.add("XX");
        }

        if (keepttl) {
            args.add("KEEPTTL");
        }
    }

}
