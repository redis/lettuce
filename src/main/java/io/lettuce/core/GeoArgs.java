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

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 *
 * Argument list builder for the Redis <a href="http://redis.io/commands/georadius">GEORADIUS</a> and
 * <a href="http://redis.io/commands/georadiusbymember">GEORADIUSBYMEMBER</a> commands.
 * <p>
 * {@link GeoArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 */
public class GeoArgs implements CompositeArgument {

    private boolean withdistance;

    private boolean withcoordinates;

    private boolean withhash;

    private Long count;

    private Sort sort = Sort.none;

    /**
     * Builder entry points for {@link GeoArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link GeoArgs} with {@literal WITHDIST} enabled.
         *
         * @return new {@link GeoArgs} with {@literal WITHDIST} enabled.
         * @see GeoArgs#withDistance()
         */
        public static GeoArgs distance() {
            return new GeoArgs().withDistance();
        }

        /**
         * Creates new {@link GeoArgs} with {@literal WITHCOORD} enabled.
         *
         * @return new {@link GeoArgs} with {@literal WITHCOORD} enabled.
         * @see GeoArgs#withCoordinates()
         */
        public static GeoArgs coordinates() {
            return new GeoArgs().withCoordinates();
        }

        /**
         * Creates new {@link GeoArgs} with {@literal WITHHASH} enabled.
         *
         * @return new {@link GeoArgs} with {@literal WITHHASH} enabled.
         * @see GeoArgs#withHash()
         */
        public static GeoArgs hash() {
            return new GeoArgs().withHash();
        }

        /**
         * Creates new {@link GeoArgs} with distance, coordinates and hash enabled.
         *
         * @return new {@link GeoArgs} with {@literal WITHDIST}, {@literal WITHCOORD}, {@literal WITHHASH} enabled.
         * @see GeoArgs#withDistance()
         * @see GeoArgs#withCoordinates()
         * @see GeoArgs#withHash()
         */
        public static GeoArgs full() {
            return new GeoArgs().withDistance().withCoordinates().withHash();
        }

        /**
         * Creates new {@link GeoArgs} with {@literal COUNT} set.
         *
         * @param count number greater 0.
         * @return new {@link GeoArgs} with {@literal COUNT} set.
         * @see GeoArgs#withCount(long)
         */
        public static GeoArgs count(long count) {
            return new GeoArgs().withCount(count);
        }

    }

    /**
     * Request distance for results.
     *
     * @return {@code this} {@link GeoArgs}.
     */
    public GeoArgs withDistance() {

        withdistance = true;
        return this;
    }

    /**
     * Request coordinates for results.
     *
     * @return {@code this} {@link GeoArgs}.
     */
    public GeoArgs withCoordinates() {

        withcoordinates = true;
        return this;
    }

    /**
     * Request geohash for results.
     *
     * @return {@code this} {@link GeoArgs}.
     */
    public GeoArgs withHash() {
        withhash = true;
        return this;
    }

    /**
     * Limit results to {@code count} entries.
     *
     * @param count number greater 0.
     * @return {@code this} {@link GeoArgs}.
     */
    public GeoArgs withCount(long count) {

        LettuceAssert.isTrue(count > 0, "Count must be greater 0");

        this.count = count;
        return this;
    }

    /**
     *
     * @return {@code true} if distance is requested.
     */
    public boolean isWithDistance() {
        return withdistance;
    }

    /**
     *
     * @return {@code true} if coordinates are requested.
     */
    public boolean isWithCoordinates() {
        return withcoordinates;
    }

    /**
     *
     * @return {@code true} if geohash is requested.
     */
    public boolean isWithHash() {
        return withhash;
    }

    /**
     * Sort results ascending.
     *
     * @return {@code this}
     */
    public GeoArgs asc() {
        return sort(Sort.asc);
    }

    /**
     * Sort results descending.
     *
     * @return {@code this}
     */
    public GeoArgs desc() {
        return sort(Sort.desc);
    }

    /**
     * Sort results.
     *
     * @param sort sort order, must not be {@code null}
     * @return {@code this}
     */
    public GeoArgs sort(Sort sort) {

        LettuceAssert.notNull(sort, "Sort must not be null");

        this.sort = sort;
        return this;
    }

    /**
     * Sort order.
     */
    public enum Sort {

        /**
         * ascending.
         */
        asc,

        /**
         * descending.
         */
        desc,

        /**
         * no sort order.
         */
        none;
    }

    /**
     * Supported geo unit.
     */
    public enum Unit {

        /**
         * meter.
         */
        m,

        /**
         * kilometer.
         */
        km,

        /**
         * feet.
         */
        ft,

        /**
         * mile.
         */
        mi;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (withdistance) {
            args.add("WITHDIST");
        }

        if (withhash) {
            args.add("WITHHASH");
        }

        if (withcoordinates) {
            args.add("WITHCOORD");
        }

        if (sort != null && sort != Sort.none) {
            args.add(sort.name());
        }

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }
    }

}
