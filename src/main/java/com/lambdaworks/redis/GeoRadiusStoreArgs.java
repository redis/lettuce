package com.lambdaworks.redis;

import com.lambdaworks.redis.GeoArgs.Sort;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;

/**
 * Store Args for {@literal GEORADIUS} to store {@literal GEORADIUS} results or {@literal GEORADIUS} distances in a sorted set.
 *
 * @author Mark Paluch
 */
public class GeoRadiusStoreArgs<K> implements CompositeArgument {

    private K storeKey;
    private K storeDistKey;
    private Long count;
    private Sort sort = Sort.none;

    /**
     * Store the resulting members with their location in the new Geo set {@code storeKey}.
     * Cannot be used together with {@link #withStoreDist(Object)}.
     *
     * @param storeKey the destination key.
     * @return {@code this}
     */
    public GeoRadiusStoreArgs withStore(K storeKey) {
        LettuceAssert.notNull(storeKey, "StoreKey must not be null");
        this.storeKey = storeKey;
        return this;
    }

    /**
     * Store the resulting members with their distance in the sorted set {@code storeKey}.
     * Cannot be used together with {@link #withStore(Object)}.
     *
     * @param storeKey the destination key.
     * @return {@code this}
     */
    public GeoRadiusStoreArgs withStoreDist(K storeKey) {
        LettuceAssert.notNull(storeKey, "StoreKey must not be null");
        this.storeDistKey = storeKey;
        return this;
    }

    /**
     * Limit results to {@code count} entries.
     *
     * @param count number greater 0
     * @return {@code this}
     */
    public GeoRadiusStoreArgs withCount(long count) {
        LettuceAssert.isTrue(count > 0, "Count must be greater 0");
        this.count = count;
        return this;
    }

    /**
     * Sort results ascending.
     *
     * @return {@code this}
     */
    public GeoRadiusStoreArgs asc() {
        return sort(Sort.asc);
    }

    /**
     * Sort results descending.
     *
     * @return {@code this}
     */
    public GeoRadiusStoreArgs desc() {
        return sort(Sort.desc);
    }

    /**
     *
     * @return the key for storing results
     */
    public K getStoreKey() {
        return storeKey;
    }

    /**
     *
     * @return the key for storing distance results
     */
    public K getStoreDistKey() {
        return storeDistKey;
    }

    /**
     * Sort results.
     *
     * @param sort sort order, must not be {@literal null}
     * @return {@code this}
     */
    public GeoRadiusStoreArgs sort(Sort sort) {
        LettuceAssert.notNull(sort, "Sort must not be null");

        this.sort = sort;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (sort != null && sort != Sort.none) {
            args.add(sort.name());
        }

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }

        if (storeKey != null) {
            args.add("STORE").addKey((K) storeKey);
        }

        if (storeDistKey != null) {
            args.add("STOREDIST").addKey((K) storeDistKey);
        }
    }
}
