package com.lambdaworks.redis;

import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;

/**
 * Args for {@literal GEORADIUS} and {@literal GEORADIUSBYMEMBER} commands.
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
     * Request distance for results.
     * 
     * @return {@code this}
     */
    public GeoArgs withDistance() {
        withdistance = true;
        return this;
    }

    /**
     * Request coordinates for results.
     * 
     * @return {@code this}
     */
    public GeoArgs withCoordinates() {
        withcoordinates = true;
        return this;
    }

    /**
     * Request geohash for results.
     * 
     * @return {@code this}
     */
    public GeoArgs withHash() {
        withhash = true;
        return this;
    }

    /**
     * Limit results to {@code count} entries.
     * 
     * @param count number greater 0
     * @return {@code this}
     */
    public GeoArgs withCount(long count) {
        LettuceAssert.isTrue(count > 0, "Count must be greater 0");
        this.count = count;
        return this;
    }

    /**
     * 
     * @return {@literal true} if distance is requested.
     */
    public boolean isWithDistance() {
        return withdistance;
    }

    /**
     * 
     * @return {@literal true} if coordinates are requested.
     */
    public boolean isWithCoordinates() {
        return withcoordinates;
    }

    /**
     * 
     * @return {@literal true} if geohash is requested.
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
     * @param sort sort order, must not be {@literal null}
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
            args.add("withdist");
        }

        if (withhash) {
            args.add("withhash");
        }

        if (withcoordinates) {
            args.add("withcoord");
        }

        if (sort != null && sort != Sort.none) {
            args.add(sort.name());
        }

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }

    }
}
