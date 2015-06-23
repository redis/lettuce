package com.lambdaworks.redis;

import java.util.List;
import java.util.Set;

/**
 * Synchronous executed commands for Geo-Commands.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
public interface RedisGeoAsyncConnection<K, V> {

    /**
     * Single geo add.
     * 
     * @param key
     * @param latitude
     * @param longitude
     * @param member
     * @return 1 if member is new or 0 if member is updated
     */
    RedisFuture<Long> geoadd(K key, double latitude, double longitude, V member);

    /**
     * Multi geo add
     * 
     * @param key
     * @param latLonMember triplets of double latitude, double longitude and V member
     * @return count of members submitted
     */
    RedisFuture<Long> geoadd(K key, Object... latLonMember);

    /**
     * Retrieve members selected by distance with the center of {@code latitude} and {@code longitude}.
     * 
     * @param key
     * @param latitude
     * @param longitude
     * @param distance
     * @param unit
     * @return
     */
    RedisFuture<Set<V>> georadius(K key, double latitude, double longitude, double distance, GeoArgs.Unit unit);

    /**
     * Retrieve members selected by distance with the center of {@code latitude} and {@code longitude}.
     * 
     * @param key
     * @param latitude
     * @param longitude
     * @param distance
     * @param unit
     * @return
     */
    RedisFuture<List<Object>> georadius(K key, double latitude, double longitude, double distance, GeoArgs.Unit unit,
            GeoArgs geoArgs);

    /**
     * Retrieve members selected by distance with the center of {@code member}.
     * 
     * @param key
     * @param member
     * @param distance
     * @param unit
     * @return
     */
    RedisFuture<Set<V>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit);

    /**
     *
     * Retrieve members selected by distance with the center of {@code member}.
     * 
     * @param key
     * @param member
     * @param distance
     * @param unit
     * @return
     */
    RedisFuture<List<Object>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit, GeoArgs geoArgs);

    /**
     *
     * Encode latitude and longitude to highest geohash accuracy.
     *
     * @param latitude
     * @param longitude
     * @return nested multi-bulk reply with 1: the 52-bit geohash integer for your latitude longitude, 2: The minimum corner of
     *         your geohash, 3: The maximum corner of your geohash, 4: The averaged center of your geohash.
     */
    RedisFuture<List<Object>> geoencode(double latitude, double longitude);

    /**
     *
     * Encode latitude and longitude to highest geohash accuracy.
     *
     * @param latitude
     * @param longitude
     * @param distance
     * @param unit
     * @return nested multi-bulk reply with 1: the 52-bit geohash integer for your latitude longitude, 2: The minimum corner of
     *         your geohash, 3: The maximum corner of your geohash, 4: The averaged center of your geohash.
     */
    RedisFuture<List<Object>> geoencode(double latitude, double longitude, double distance, GeoArgs.Unit unit);

    /**
     *
     * Decode geohash.
     *
     * @param geohash
     * @return nested multi-bulk with 1: minimum decoded corner, 2: maximum decoded corner, 3: averaged center of bounding box.
     */
    RedisFuture<List<Object>> geodecode(long geohash);
}
