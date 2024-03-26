package biz.paluch.redis.extensibility;

import java.util.List;
import java.util.Set;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;

public class LettuceGeoDemo {

    public static void main(String[] args) {

        RedisClient redisClient = RedisClient.create(RedisURI.Builder.redis("localhost", 6379).build());
        RedisCommands<String, String> redis = redisClient.connect().sync();
        String key = "my-geo-set";

        redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim", 8.3796281, 48.9978127, "Office tower", 8.665351, 49.553302,
                "Train station");

        Set<String> georadius = redis.georadius(key, 8.6582861, 49.5285695, 5, GeoArgs.Unit.km);
        System.out.println("Geo Radius: " + georadius);

        // georadius contains "Weinheim" and "Train station"

        Double distance = redis.geodist(key, "Weinheim", "Train station", GeoArgs.Unit.km);
        System.out.println("Distance: " + distance + " km");

        // distance ≈ 2.78km

        GeoArgs geoArgs = new GeoArgs().withHash().withCoordinates().withDistance().withCount(2).asc();

        List<GeoWithin<String>> georadiusWithArgs = redis.georadius(key, 8.665351, 49.5285695, 5, GeoArgs.Unit.km, geoArgs);

        // georadiusWithArgs contains "Weinheim" and "Train station"
        // ordered descending by distance and containing distance/coordinates
        GeoWithin<String> weinheim = georadiusWithArgs.get(0);

        System.out.println("Member: " + weinheim.getMember());
        System.out.println("Geo hash: " + weinheim.getGeohash());
        System.out.println("Distance: " + weinheim.getDistance());
        System.out.println("Coordinates: " + weinheim.getCoordinates().getX() + "/" + weinheim.getCoordinates().getY());

        List<GeoCoordinates> geopos = redis.geopos(key, "Weinheim", "Train station");
        GeoCoordinates weinheimGeopos = geopos.get(0);
        System.out.println("Coordinates: " + weinheimGeopos.getX() + "/" + weinheimGeopos.getY());

        redis.getStatefulConnection().close();
        redisClient.shutdown();
    }
}
