/*
 * Copyright 2011-2022 the original author or authors.
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

import static io.lettuce.core.protocol.CommandKeyword.*;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/geoadd">GEOADD</a> command starting from Redis 6.2.
 * Static import the methods from {@link Builder} and call the methods: {@code xx()} or {@code nx()} .
 * <p>
 * {@link GeoAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class GeoAddArgs implements CompositeArgument {

    private boolean nx = false;

    private boolean xx = false;

    private boolean ch = false;

    /**
     * Builder entry points for {@link ScanArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link GeoAddArgs} and enabling {@literal NX}.
         *
         * @return new {@link GeoAddArgs} with {@literal NX} enabled.
         * @see GeoAddArgs#nx()
         */
        public static GeoAddArgs nx() {
            return new GeoAddArgs().nx();
        }

        /**
         * Creates new {@link GeoAddArgs} and enabling {@literal XX}.
         *
         * @return new {@link GeoAddArgs} with {@literal XX} enabled.
         * @see GeoAddArgs#xx()
         */
        public static GeoAddArgs xx() {
            return new GeoAddArgs().xx();
        }

        /**
         * Creates new {@link GeoAddArgs} and enabling {@literal CH}.
         *
         * @return new {@link GeoAddArgs} with {@literal CH} enabled.
         * @see GeoAddArgs#ch()
         */
        public static GeoAddArgs ch() {
            return new GeoAddArgs().ch();
        }

    }

    /**
     * Don't update already existing elements. Always add new elements.
     *
     * @return {@code this} {@link GeoAddArgs}.
     */
    public GeoAddArgs nx() {

        this.nx = true;
        return this;
    }

    /**
     * Only update elements that already exist. Never add elements.
     *
     * @return {@code this} {@link GeoAddArgs}.
     */
    public GeoAddArgs xx() {

        this.xx = true;
        return this;
    }

    /**
     * Modify the return value from the number of new elements added, to the total number of elements changed.
     *
     * @return {@code this} {@link GeoAddArgs}.
     */
    public GeoAddArgs ch() {

        this.ch = true;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (nx) {
            args.add(NX);
        }

        if (xx) {
            args.add(XX);
        }

        if (ch) {
            args.add(CH);
        }
    }

}
