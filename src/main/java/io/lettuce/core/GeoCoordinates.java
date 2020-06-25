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

/**
 * A tuple consisting of numerical geo data points to describe geo coordinates.
 *
 * @author Mark Paluch
 */
public class GeoCoordinates {

    private final Number x;

    private final Number y;

    /**
     * Creates new {@link GeoCoordinates}.
     *
     * @param x the longitude, must not be {@code null}.
     * @param y the latitude, must not be {@code null}.
     */
    public GeoCoordinates(Number x, Number y) {

        LettuceAssert.notNull(x, "X must not be null");
        LettuceAssert.notNull(y, "Y must not be null");

        this.x = x;
        this.y = y;
    }

    /**
     * Creates new {@link GeoCoordinates}.
     *
     * @param x the longitude, must not be {@code null}.
     * @param y the latitude, must not be {@code null}.
     * @return {@link GeoCoordinates}.
     */
    public static GeoCoordinates create(Number x, Number y) {
        return new GeoCoordinates(x, y);
    }

    /**
     *
     * @return the longitude.
     */
    public Number getX() {
        return x;
    }

    /**
     *
     * @return the latitude.
     */
    public Number getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GeoCoordinates))
            return false;

        GeoCoordinates geoCoords = (GeoCoordinates) o;

        if (x != null ? !x.equals(geoCoords.x) : geoCoords.x != null)
            return false;
        return !(y != null ? !y.equals(geoCoords.y) : geoCoords.y != null);
    }

    @Override
    public int hashCode() {
        int result = x != null ? x.hashCode() : 0;
        result = 31 * result + (y != null ? y.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", getX(), getY());
    }

}
