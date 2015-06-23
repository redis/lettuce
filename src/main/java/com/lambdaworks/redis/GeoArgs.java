package com.lambdaworks.redis;

import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * Args for {@literal GEORADIUS} and {@literal GEORADIUSBYMEMBER} commands.
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class GeoArgs {

    private boolean withdistance;
    private boolean withcoordinates;
    private boolean withhash;
    private boolean noproperties;
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

    public GeoArgs noProperties() {
        noproperties = true;
        return this;
    }

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
        meter, kilometer, feet, mile;
    }

    public <K, V> void build(CommandArgs<K, V> args) {
        if (withdistance) {
            args.add("withdistance");
        }

        if (withcoordinates) {
            args.add("withcoordinates");
        }

        if (withhash) {
            args.add("withhash");
        }

        if (noproperties) {
            args.add("noproperties");
        }

        if (sort != null && sort != Sort.none) {
            args.add(sort.name());
        }

    }
}
