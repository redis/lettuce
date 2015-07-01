package com.lambdaworks.redis;

/**
 * Geo-encoded object. Contains:
 * 
 * <ul>
 * <li>the 52-bit geohash integer for your longitude/latitude</li>
 * <li>the minimum corner of your geohash {@link GeoCoordinates}</li>
 * <li>the maximum corner of your geohash {@link GeoCoordinates}</li>
 * <li>4: The averaged center of your geohash {@link GeoCoordinates}</li>
 * </ul>
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class GeoEncoded {

    public final long geohash;
    public final GeoCoordinates min;
    public final GeoCoordinates max;
    public final GeoCoordinates avg;

    public GeoEncoded(long geohash, GeoCoordinates min, GeoCoordinates max, GeoCoordinates avg) {
        this.geohash = geohash;
        this.min = min;
        this.max = max;
        this.avg = avg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GeoEncoded))
            return false;

        GeoEncoded that = (GeoEncoded) o;

        if (geohash != that.geohash)
            return false;
        if (min != null ? !min.equals(that.min) : that.min != null)
            return false;
        if (max != null ? !max.equals(that.max) : that.max != null)
            return false;
        return !(avg != null ? !avg.equals(that.avg) : that.avg != null);
    }

    @Override
    public int hashCode() {
        int result = (int) (geohash ^ (geohash >>> 32));
        result = 31 * result + (min != null ? min.hashCode() : 0);
        result = 31 * result + (max != null ? max.hashCode() : 0);
        result = 31 * result + (avg != null ? avg.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [geohash=").append(geohash);
        sb.append(", min=").append(min);
        sb.append(", max=").append(max);
        sb.append(", avg=").append(avg);
        sb.append(']');
        return sb.toString();
    }
}
