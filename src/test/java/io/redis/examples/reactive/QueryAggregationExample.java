// EXAMPLE: query_agg
package io.redis.examples.reactive;

import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.AggregateArgs.GroupBy;
import io.lettuce.core.search.arguments.AggregateArgs.Reducer;
import io.lettuce.core.search.AggregationReply;

import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;

import java.util.Arrays;
import java.util.List;
// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END
import reactor.core.publisher.Mono;

public class QueryAggregationExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();
            JsonParser parser = reactiveCommands.getJsonParser();

            // REMOVE_START
            // Clean up any existing data
            Mono<Void> cleanup = reactiveCommands
                    .del("bicycle:0", "bicycle:1", "bicycle:2", "bicycle:3", "bicycle:4", "bicycle:5", "bicycle:6", "bicycle:7",
                            "bicycle:8", "bicycle:9")
                    .flatMap(v -> reactiveCommands.ftDropindex("idx:bicycle"))
                    .onErrorReturn("Index `idx:bicycle` does not exist.").then();
            cleanup.block();
            // REMOVE_END

            // create index
            List<FieldArgs<String>> schema = Arrays.asList(
                    TagFieldArgs.<String> builder().name("$.condition").as("condition").build(),
                    NumericFieldArgs.<String> builder().name("$.price").as("price").build());

            CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix("bicycle:")
                    .on(CreateArgs.TargetType.JSON).build();

            // load data using JsonParser
            List<JsonObject> bicycleJsons = Arrays.asList(parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                    "\"POLYGON((-74.0610 40.7578, -73.9510 40.7578, -73.9510 40.6678, -74.0610 40.6678, -74.0610 40.7578))\""))
                    .put("store_location", parser.createJsonValue("\"-74.0060,40.7128\""))
                    .put("brand", parser.createJsonValue("\"Velorim\"")).put("model", parser.createJsonValue("\"Jigger\""))
                    .put("price", parser.createJsonValue("270"))
                    .put("description", parser
                            .createJsonValue("\"Small and powerful, the Jigger is the best ride for the smallest of tikes! "
                                    + "This is the tiniest kids' pedal bike on the market available without a coaster brake, the Jigger "
                                    + "is the vehicle of choice for the rare tenacious little rider raring to go.\""))
                    .put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((-118.2887 34.0972, -118.1987 34.0972, -118.1987 33.9872, -118.2887 33.9872, -118.2887 34.0972))\""))
                            .put("store_location", parser.createJsonValue("\"-118.2437,34.0522\""))
                            .put("brand", parser.createJsonValue("\"Bicyk\""))
                            .put("model", parser.createJsonValue("\"Hillcraft\"")).put("price", parser.createJsonValue("1200"))
                            .put("description",
                                    parser.createJsonValue("\"Kids want to ride with as little weight as possible. Especially "
                                            + "on an incline! They may be at the age when a 27.5'' wheel bike is just too clumsy coming "
                                            + "off a 24'' bike. The Hillcraft 26 is just the solution they need!\""))
                            .put("condition", parser.createJsonValue("\"used\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((-87.6848 41.9331, -87.5748 41.9331, -87.5748 41.8231, -87.6848 41.8231, -87.6848 41.9331))\""))
                            .put("store_location", parser.createJsonValue("\"-87.6298,41.8781\""))
                            .put("brand", parser.createJsonValue("\"Nord\""))
                            .put("model", parser.createJsonValue("\"Chook air 5\"")).put("price", parser.createJsonValue("815"))
                            .put("description",
                                    parser.createJsonValue("\"The Chook Air 5  gives kids aged six years and older a durable "
                                            + "and uberlight mountain bike for their first experience on tracks and easy cruising through "
                                            + "forests and fields. The lower  top tube makes it easy to mount and dismount in any "
                                            + "situation, giving your kids greater safety on the trails.\""))
                            .put("condition", parser.createJsonValue("\"used\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((-80.2433 25.8067, -80.1333 25.8067, -80.1333 25.6967, -80.2433 25.6967, -80.2433 25.8067))\""))
                            .put("store_location", parser.createJsonValue("\"-80.1918,25.7617\""))
                            .put("brand", parser.createJsonValue("\"Eva\"")).put("model", parser.createJsonValue("\"Eva 291\""))
                            .put("price", parser.createJsonValue("3400"))
                            .put("description",
                                    parser.createJsonValue("\"The sister company to Nord, Eva launched in 2005 as the first "
                                            + "and only women-dedicated bicycle brand. Designed by women for women, allEva bikes "
                                            + "are optimized for the feminine physique using analytics from a body metrics database. "
                                            + "If you like 29ers, try the Eva 291. It's a brand new bike for 2022.. This "
                                            + "full-suspension, cross-country ride has been designed for velocity. The 291 has "
                                            + "100mm of front and rear travel, a superlight aluminum frame and fast-rolling "
                                            + "29-inch wheels. Yippee!\""))
                            .put("condition", parser.createJsonValue("\"used\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((-122.4644 37.8199, -122.3544 37.8199, -122.3544 37.7099, -122.4644 37.7099, -122.4644 37.8199))\""))
                            .put("store_location", parser.createJsonValue("\"-122.4194,37.7749\""))
                            .put("brand", parser.createJsonValue("\"Noka Bikes\""))
                            .put("model", parser.createJsonValue("\"Kahuna\"")).put("price", parser.createJsonValue("3200"))
                            .put("description",
                                    parser.createJsonValue("\"Whether you want to try your hand at XC racing or are looking "
                                            + "for a lively trail bike that's just as inspiring on the climbs as it is over rougher "
                                            + "ground, the Wilder is one heck of a bike built specifically for short women. Both the "
                                            + "frames and components have been tweaked to include a women's saddle, different bars "
                                            + "and unique colourway.\""))
                            .put("condition", parser.createJsonValue("\"used\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((-0.1778 51.5524, 0.0822 51.5524, 0.0822 51.4024, -0.1778 51.4024, -0.1778 51.5524))\""))
                            .put("store_location", parser.createJsonValue("\"-0.1278,51.5074\""))
                            .put("brand", parser.createJsonValue("\"Breakout\""))
                            .put("model", parser.createJsonValue("\"XBN 2.1 Alloy\""))
                            .put("price", parser.createJsonValue("810"))
                            .put("description",
                                    parser.createJsonValue("\"The XBN 2.1 Alloy is our entry-level road bike – but that's "
                                            + "not to say that it's a basic machine. With an internal weld aluminium frame, a full "
                                            + "carbon fork, and the slick-shifting Claris gears from Shimano's, this is a bike which "
                                            + "doesn't break the bank and delivers craved performance.\""))
                            .put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((2.1767 48.9016, 2.5267 48.9016, 2.5267 48.5516, 2.1767 48.5516, 2.1767 48.9016))\""))
                            .put("store_location", parser.createJsonValue("\"2.3522,48.8566\""))
                            .put("brand", parser.createJsonValue("\"ScramBikes\""))
                            .put("model", parser.createJsonValue("\"WattBike\"")).put("price", parser.createJsonValue("2300"))
                            .put("description",
                                    parser.createJsonValue("\"The WattBike is the best e-bike for people who still "
                                            + "feel young at heart. It has a Bafang 1000W mid-drive system and a 48V 17.5AH "
                                            + "Samsung Lithium-Ion battery, allowing you to ride for more than 60 miles on one "
                                            + "charge. It's great for tackling hilly terrain or if you just fancy a more "
                                            + "leisurely ride. With three working modes, you can choose between E-bike, "
                                            + "assisted bicycle, and normal bike modes.\""))
                            .put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((13.3260 52.5700, 13.6550 52.5700, 13.6550 52.2700, 13.3260 52.2700, 13.3260 52.5700))\""))
                            .put("store_location", parser.createJsonValue("\"13.4050,52.5200\""))
                            .put("brand", parser.createJsonValue("\"Peaknetic\""))
                            .put("model", parser.createJsonValue("\"Secto\"")).put("price", parser.createJsonValue("430"))
                            .put("description",
                                    parser.createJsonValue("\"If you struggle with stiff fingers or a kinked neck or "
                                            + "back after a few minutes on the road, this lightweight, aluminum bike alleviates "
                                            + "those issues and allows you to enjoy the ride. From the ergonomic grips to the "
                                            + "lumbar-supporting seat position, the Roll Low-Entry offers incredible comfort. "
                                            + "The rear-inclined seat tube facilitates stability by allowing you to put a foot "
                                            + "on the ground to balance at a stop, and the low step-over frame makes it "
                                            + "accessible for all ability and mobility levels. The saddle is very soft, with "
                                            + "a wide back to support your hip joints and a cutout in the center to redistribute "
                                            + "that pressure. Rim brakes deliver satisfactory braking control, and the wide tires "
                                            + "provide a smooth, stable ride on paved roads and gravel. Rack and fender mounts "
                                            + "facilitate setting up the Roll Low-Entry as your preferred commuter, and the "
                                            + "BMX-like handlebar offers space for mounting a flashlight, bell, or phone holder.\""))
                            .put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((1.9450 41.4301, 2.4018 41.4301, 2.4018 41.1987, 1.9450 41.1987, 1.9450 41.4301))\""))
                            .put("store_location", parser.createJsonValue("\"2.1734, 41.3851\""))
                            .put("brand", parser.createJsonValue("\"nHill\""))
                            .put("model", parser.createJsonValue("\"Summit\"")).put("price", parser.createJsonValue("1200"))
                            .put("description",
                                    parser.createJsonValue("\"This budget mountain bike from nHill performs well both "
                                            + "on bike paths and on the trail. The fork with 100mm of travel absorbs rough "
                                            + "terrain. Fat Kenda Booster tires give you grip in corners and on wet trails. "
                                            + "The Shimano Tourney drivetrain offered enough gears for finding a comfortable "
                                            + "pace to ride uphill, and the Tektro hydraulic disc brakes break smoothly. "
                                            + "Whether you want an affordable bike that you can take to work, but also take "
                                            + "trail in mountains on the weekends or you're just after a stable, comfortable "
                                            + "ride for the bike path, the Summit gives a good value for money.\""))
                            .put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("pickup_zone", parser.createJsonValue(
                            "\"POLYGON((12.4464 42.1028, 12.5464 42.1028, 12.5464 41.7028, 12.4464 41.7028, 12.4464 42.1028))\""))
                            .put("store_location", parser.createJsonValue("\"12.4964,41.9028\""))
                            .put("brand", parser.createJsonValue("\"BikeShind\""))
                            .put("model", parser.createJsonValue("\"ThrillCycle\"")).put("price", parser.createJsonValue("815"))
                            .put("description",
                                    parser.createJsonValue("\"An artsy,  retro-inspired bicycle that's as "
                                            + "functional as it is pretty: The ThrillCycle steel frame offers a smooth ride. "
                                            + "A 9-speed drivetrain has enough gears for coasting in the city, but we wouldn't "
                                            + "suggest taking it to the mountains. Fenders protect you from mud, and a rear "
                                            + "basket lets you transport groceries, flowers and books. The ThrillCycle comes "
                                            + "with a limited lifetime warranty, so this little guy will last you long "
                                            + "past graduation.\""))
                            .put("condition", parser.createJsonValue("\"refurbished\"")));

            Mono<String> setup = reactiveCommands.ftCreate("idx:bicycle", createArgs, schema).flatMap(result -> {
                // load data sequentially using reactive chains
                Mono<String> loadChain = Mono.just("OK");
                for (int i = 0; i < bicycleJsons.size(); i++) {
                    final int index = i;
                    loadChain = loadChain.flatMap(
                            v -> reactiveCommands.jsonSet("bicycle:" + index, JsonPath.ROOT_PATH, bicycleJsons.get(index)));
                }
                return loadChain;
            });

            setup.block();

            // STEP_START agg1
            AggregateArgs<String, String> agg1Args = AggregateArgs.<String, String> builder().load("__key").load("price")
                    .apply("@price - (@price * 0.1)", "discounted").build();

            Mono<AggregationReply<String, String>> agg1 = reactiveCommands
                    .ftAggregate("idx:bicycle", "@condition:{new}", agg1Args).doOnNext(result -> {
                        result.getReplies().get(0).getResults().stream()
                                .sorted((doc1, doc2) -> doc1.getFields().get("__key").compareTo(doc2.getFields().get("__key")))
                                .forEach(doc -> {
                                    System.out.printf("Key: %s, Price: %s, Discounted: %s\n", doc.getFields().get("__key"),
                                            doc.getFields().get("price"), doc.getFields().get("discounted"));
                                });
                        // >>> Key: bicycle:0, Price: 270, Discounted: 243
                        // >>> Key: bicycle:5, Price: 810, Discounted: 729
                        // >>> Key: bicycle:6, Price: 2300, Discounted: 2070
                        // >>> Key: bicycle:7, Price: 430, Discounted: 387
                        // >>> Key: bicycle:8, Price: 1200, Discounted: 1080
                        // REMOVE_START
                        assertThat(result.getReplies().get(0).getResults().size()).isEqualTo(5);
                        assertThat(result.getReplies().get(0).getResults().get(0).getFields().get("discounted"))
                                .isEqualTo("243");
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START agg2
            AggregateArgs<String, String> agg2Args = AggregateArgs.<String, String> builder().load("price")
                    .apply("@price<1000", "price_category").groupBy(GroupBy.<String, String> of("condition")
                            .reduce(Reducer.<String, String> sum("@price_category").as("num_affordable")))
                    .build();

            Mono<AggregationReply<String, String>> agg2 = reactiveCommands.ftAggregate("idx:bicycle", "*", agg2Args)
                    .doOnNext(result -> {
                        result.getReplies().get(0).getResults().stream().sorted(
                                (doc1, doc2) -> doc1.getFields().get("condition").compareTo(doc2.getFields().get("condition")))
                                .forEach(doc -> {
                                    System.out.printf("Condition: %s, Num Affordable: %s\n", doc.getFields().get("condition"),
                                            doc.getFields().get("num_affordable"));
                                });
                        // >>> Condition: new, Num Affordable: 3
                        // >>> Condition: refurbished, Num Affordable: 1
                        // >>> Condition: used, Num Affordable: 1
                        // REMOVE_START
                        assertThat(result.getReplies().get(0).getResults().size()).isEqualTo(3);
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START agg3
            AggregateArgs<String, String> agg3Args = AggregateArgs.<String, String> builder().apply("'bicycle'", "type")
                    .groupBy(GroupBy.<String, String> of("type").reduce(Reducer.<String, String> count().as("num_total")))
                    .build();

            Mono<AggregationReply<String, String>> agg3 = reactiveCommands.ftAggregate("idx:bicycle", "*", agg3Args)
                    .doOnNext(result -> {
                        result.getReplies().get(0).getResults().stream()
                                .sorted((doc1, doc2) -> doc1.getFields().get("type").compareTo(doc2.getFields().get("type")))
                                .forEach(doc -> {
                                    System.out.printf("Type: %s, Total Count: %s\n", doc.getFields().get("type"),
                                            doc.getFields().get("num_total"));
                                });
                        // >>> Type: bicycle, Total Count: 10
                        // REMOVE_START
                        assertThat(result.getReplies().get(0).getResults().size()).isEqualTo(1);
                        assertThat(result.getReplies().get(0).getResults().get(0).getFields().get("num_total")).isEqualTo("10");
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START agg4

            // The `TOLIST` reducer is not currently available in Lettuce.

            // STEP_END

            Mono.when(agg1, agg2, agg3).block();
        } finally {
            redisClient.shutdown();
        }
    }

}
