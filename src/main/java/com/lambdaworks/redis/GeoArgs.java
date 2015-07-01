package com.lambdaworks.redis;

import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;

/**
 * Args for {@literal GEORADIUS} and {@literal GEORADIUSBYMEMBER} commands.
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class GeoArgs {

    private boolean withdistance;
    private boolean withcoordinates;
    private boolean withhash;
    private Long count;
    private Sort sort = Sort.none;

    public GeoArgs withDistance() {
        withdistance = true;
        return this;
    }

    public GeoArgs withCoordinates() {
        withcoordinates = true;
        return this;
    }

    public GeoArgs withHash() {
        withhash = true;
        return this;
    }

    public GeoArgs withCount(long count) {
        this.count = count;
        return this;
    }

    /**
     * Sort results ascending.
     *
     * @return the current geo args.
     */
    public GeoArgs asc() {
        return sort(Sort.asc);
    }

    /**
     * Sort results descending.
     *
     * @return the current geo args.
     */
    public GeoArgs desc() {
        return sort(Sort.desc);
    }

    /**
     * Sort results.
     *
     * @param sort
     * @return the current geo args.
     */
    public GeoArgs sort(Sort sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Sort order.
     */
    public enum Sort {
        asc, desc, none;
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
