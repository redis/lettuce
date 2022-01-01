/*
 * Copyright 2021-2022 the original author or authors.
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

/**
 * Utility to create {@link GeoPredicate} and {@link GeoRef} objects to be used with {@code GEOSEARCH}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public final class GeoSearch {

    // TODO: Should be V
    /**
     * Create a {@link GeoRef} from a Geo set {@code member}.
     *
     * @param member the Geo set member to use as search reference starting point.
     * @return the {@link GeoRef}.
     */
    public static <K> GeoRef<K> fromMember(K member) {
        LettuceAssert.notNull(member, "Reference member must not be null");
        return new FromMember<>(member);
    }

    /**
     * Create a {@link GeoRef} from WGS84 coordinates {@code longitude} and {@code latitude}.
     *
     * @param longitude the longitude coordinate according to WGS84.
     * @param latitude the latitude coordinate according to WGS84.
     * @return the {@link GeoRef}.
     */
    public static <K> GeoRef<K> fromCoordinates(double longitude, double latitude) {
        return (GeoRef<K>) new FromCoordinates(longitude, latitude);
    }

    /**
     * Create a {@link GeoPredicate} by specifying a radius {@code distance} and {@link GeoArgs.Unit}.
     *
     * @param distance the radius.
     * @param unit size unit.
     * @return the {@link GeoPredicate} for the specified radius.
     */
    public static GeoPredicate byRadius(double distance, GeoArgs.Unit unit) {
        return new Radius(distance, unit);
    }

    /**
     * Create a {@link GeoPredicate} by specifying a box of the size {@code width}, {@code height} and {@link GeoArgs.Unit}.
     *
     * @param width box width.
     * @param height box height.
     * @param unit size unit.
     * @return the {@link GeoPredicate} for the specified box.
     */
    public static GeoPredicate byBox(double width, double height, GeoArgs.Unit unit) {
        return new Box(width, height, unit);
    }

    /**
     * Geo reference specifying a search starting point.
     *
     * @param <K>
     */
    public interface GeoRef<K> extends CompositeArgument {

    }

    static class FromMember<K> implements GeoRef<K> {

        final K member;

        public FromMember(K member) {
            this.member = member;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add("FROMMEMBER").addKey((K) member);
        }

    }

    static class FromCoordinates implements GeoRef<Object> {

        final double longitude, latitude;

        public FromCoordinates(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

        @Override
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add("FROMLONLAT").add(longitude).add(latitude);
        }

    }

    /**
     * Geo predicate specifying a search scope.
     */
    public interface GeoPredicate extends CompositeArgument {

    }

    static class Radius implements GeoPredicate {

        final double distance;

        final GeoArgs.Unit unit;

        public Radius(double distance, GeoArgs.Unit unit) {
            this.distance = distance;
            this.unit = unit;
        }

        @Override
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add("BYRADIUS").add(distance).add(unit);
        }

    }

    static class Box implements GeoPredicate {

        final double width, height;

        final GeoArgs.Unit unit;

        public Box(double width, double height, GeoArgs.Unit unit) {
            this.width = width;
            this.height = height;
            this.unit = unit;
        }

        @Override
        public <K, V> void build(CommandArgs<K, V> args) {
            args.add("BYBOX").add(width).add(height).add(unit);
        }

    }

}
