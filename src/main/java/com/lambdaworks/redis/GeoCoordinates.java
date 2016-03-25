package com.lambdaworks.redis;

import com.lambdaworks.redis.output.DoubleOutput;

/**
 * A tuple consisting of numerical geo data points to describe geo coordinates.
 * 
 * @author Mark Paluch
 */
public class GeoCoordinates {

    public final Number x;
    public final Number y;

    public GeoCoordinates(Number x, Number y) {

        this.x = x;
        this.y = y;
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

        return String.format("(%s, %s)", x, y);
    }

}
