/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core;

import io.lettuce.core.GeoArgs.Sort;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

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
     * Store the resulting members with their location in the new Geo set {@code storeKey}. Cannot be used together with
     * {@link #withStoreDist(Object)}.
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
     * Store the resulting members with their distance in the sorted set {@code storeKey}. Cannot be used together with
     * {@link #withStore(Object)}.
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

    @SuppressWarnings("unchecked")
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
