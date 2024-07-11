package biz.paluch.redis.extensibility;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.async.RedisJsonAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.json.JsonElement;
import io.lettuce.core.json.JsonPath;

public class LettuceGeoDemo {

    String bike = "{\n" + "        \"id\": \"bike:6\",\n" + "        \"model\": \"Mimas\",\n"
            + "        \"description\": \"A real joy to ride, this bike got very high scores in last years Bike of the year report. The carefully crafted 50-34 tooth chainset and 11-32 tooth cassette give an easy-on-the-legs bottom gear for climbing, and the high-quality Vittoria Zaffiro tires give balance and grip.It includes a low-step frame , our memory foam seat, bump-resistant shocks and conveniently placed thumb throttle. Put it all together and you get a bike that helps redefine what can be done for this price.\",\n"
            + "        \"price\": 3941,\n" + "        \"specs\": {\n" + "          \"material\": \"alloy\",\n"
            + "          \"weight\": 11.6\n" + "        }";



    public static void main(String[] args) throws ExecutionException, InterruptedException {
        RedisURI redisURI = RedisURI.Builder
                .redis("redis-19897.c55.eu-central-1-1.ec2.redns.redis-cloud.com")
                .withPort(19897)
                .withPassword("9CH6niJKjHFzAiPtp9jvoI9OvErZ7urh")
                .withTimeout(Duration.ofSeconds(30))
                .build();
        RedisClient redisClient = RedisClient.create(redisURI);
        RedisAsyncCommands<String, String> redis = redisClient.connect().async();
        JsonElement element = new JsonElement() {
            @Override
            public String toString() {
                return "";
            }

            @Override
            public JsonElement fromString(String json) {
                return null;
            }
        };
        redis.jsonArrappend("bikes:inventory", JsonPath.of("$..commuter_bikes"), element).get();


//        String key = "my-geo-set";
//
//        redis.geoadd(key, 8.6638775, 49.5282537, "Weinheim", 8.3796281, 48.9978127, "Office tower", 8.665351, 49.553302,
//                "Train station");
//
//        Set<String> georadius = redis.georadius(key, 8.6582861, 49.5285695, 5, GeoArgs.Unit.km);
//        System.out.println("Geo Radius: " + georadius);
//
//        // georadius contains "Weinheim" and "Train station"
//
//        Double distance = redis.geodist(key, "Weinheim", "Train station", GeoArgs.Unit.km);
//        System.out.println("Distance: " + distance + " km");
//
//        // distance ≈ 2.78km
//
//        GeoArgs geoArgs = new GeoArgs().withHash().withCoordinates().withDistance().withCount(2).asc();
//
//        List<GeoWithin<String>> georadiusWithArgs = redis.georadius(key, 8.665351, 49.5285695, 5, GeoArgs.Unit.km, geoArgs);
//
//        // georadiusWithArgs contains "Weinheim" and "Train station"
//        // ordered descending by distance and containing distance/coordinates
//        GeoWithin<String> weinheim = georadiusWithArgs.get(0);
//
//        System.out.println("Member: " + weinheim.getMember());
//        System.out.println("Geo hash: " + weinheim.getGeohash());
//        System.out.println("Distance: " + weinheim.getDistance());
//        System.out.println("Coordinates: " + weinheim.getCoordinates().getX() + "/" + weinheim.getCoordinates().getY());
//
//        List<GeoCoordinates> geopos = redis.geopos(key, "Weinheim", "Train station");
//        GeoCoordinates weinheimGeopos = geopos.get(0);
//        System.out.println("Coordinates: " + weinheimGeopos.getX() + "/" + weinheimGeopos.getY());

        redis.getStatefulConnection().close();
        redisClient.shutdown();
    }

}
