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

/**
 * Geo element within a certain radius. Contains:
 * <ul>
 * <li>the member</li>
 * <li>the distance from the reference point (if requested)</li>
 * <li>the geohash (if requested)</li>
 * <li>the coordinates (if requested)</li>
 * </ul>
 *
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class GeoWithin<V> {

    private final V member;

    private final Double distance;

    private final Long geohash;

    private final GeoCoordinates coordinates;

    /**
     * Creates a new {@link GeoWithin}.
     *
     * @param member the member.
     * @param distance the distance, may be {@code null}.
     * @param geohash the geohash, may be {@code null}.
     * @param coordinates the coordinates, may be {@code null}.
     */
    public GeoWithin(V member, Double distance, Long geohash, GeoCoordinates coordinates) {

        this.member = member;
        this.distance = distance;
        this.geohash = geohash;
        this.coordinates = coordinates;
    }

    /**
     *
     * @return the member within the Geo set.
     */
    public V getMember() {
        return member;
    }

    /**
     *
     * @return distance if requested otherwise {@code null}.
     */
    public Double getDistance() {
        return distance;
    }

    /**
     *
     * @return geohash if requested otherwise {@code null}.
     */
    public Long getGeohash() {
        return geohash;
    }

    /**
     *
     * @return coordinates if requested otherwise {@code null}.
     */
    public GeoCoordinates getCoordinates() {
        return coordinates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GeoWithin))
            return false;

        GeoWithin<?> geoWithin = (GeoWithin<?>) o;

        if (member != null ? !member.equals(geoWithin.member) : geoWithin.member != null)
            return false;
        if (distance != null ? !distance.equals(geoWithin.distance) : geoWithin.distance != null)
            return false;
        if (geohash != null ? !geohash.equals(geoWithin.geohash) : geoWithin.geohash != null)
            return false;
        return !(coordinates != null ? !coordinates.equals(geoWithin.coordinates) : geoWithin.coordinates != null);
    }

    @Override
    public int hashCode() {
        int result = member != null ? member.hashCode() : 0;
        result = 31 * result + (distance != null ? distance.hashCode() : 0);
        result = 31 * result + (geohash != null ? geohash.hashCode() : 0);
        result = 31 * result + (coordinates != null ? coordinates.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [member=").append(member);
        sb.append(", distance=").append(distance);
        sb.append(", geohash=").append(geohash);
        sb.append(", coordinates=").append(coordinates);
        sb.append(']');
        return sb.toString();
    }

}
