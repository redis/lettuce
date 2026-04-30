/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Field arguments for GEO fields in a RediSearch index.
 * <p>
 * Geo fields are used to store geographical coordinates such as longitude and latitude. They enable geospatial radius queries,
 * which allow you to implement location-based search functionality in your applications such as finding nearby restaurants,
 * stores, or any other points of interest.
 *
 * @param <K> Key type
 * @see <a href=
 *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#geo-fields">Geo
 *      Fields</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
public class GeoFieldArgs<K> extends FieldArgs<K> {

    /**
     * Create a new {@link GeoFieldArgs} using the builder pattern.
     * 
     * @param <K> Key type
     * @return a new {@link Builder}
     */
    public static <K> Builder<K> builder() {
        return new Builder<>();
    }

    @Override
    public String getFieldType() {
        return "GEO";
    }

    @Override
    protected void buildTypeSpecificArgs(CommandArgs<K, ?> args) {
        // Geo fields have no type-specific arguments beyond the common ones
    }

    /**
     * Builder for {@link GeoFieldArgs}.
     * 
     * @param <K> Key type
     */
    public static class Builder<K> extends FieldArgs.Builder<K, GeoFieldArgs<K>, Builder<K>> {

        public Builder() {
            super(new GeoFieldArgs<>());
        }

    }

}
