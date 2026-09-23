// EXAMPLE: query_range
// REMOVE_START
package io.redis.examples.async;

// REMOVE_END
import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;

import io.lettuce.core.search.arguments.*;
import io.lettuce.core.search.SearchReply;

import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class QueryRangeExample {

    @Test
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();
            // REMOVE_START
            asyncCommands.ftDropindex("idx:bicycle").exceptionally(ex -> null) // Ignore errors if the index doesn't exist.
                    .toCompletableFuture().join();
            asyncCommands.del("bicycle:0", "bicycle:1", "bicycle:2", "bicycle:3", "bicycle:4", "bicycle:5", "bicycle:6",
                    "bicycle:7", "bicycle:8", "bicycle:9").toCompletableFuture().join();
            // REMOVE_END

            List<FieldArgs<String>> bicycleSchema = Arrays.asList(
                    TextFieldArgs.<String> builder().name("$.brand").as("brand").build(),
                    TextFieldArgs.<String> builder().name("$.model").as("model").build(),
                    TextFieldArgs.<String> builder().name("$.description").as("description").build(),
                    NumericFieldArgs.<String> builder().name("$.price").as("price").build(),
                    TagFieldArgs.<String> builder().name("$.condition").as("condition").build());

            CreateArgs<String, String> bicycleCreateArgs = CreateArgs.<String, String> builder().on(CreateArgs.TargetType.JSON)
                    .withPrefix("bicycle:").build();

            asyncCommands.ftCreate("idx:bicycle", bicycleCreateArgs, bicycleSchema).toCompletableFuture().join();

            JsonParser parser = asyncCommands.getJsonParser();

            List<JsonObject> bicycleJsons = Arrays.asList(parser.createJsonObject()
                    .put("brand", parser.createJsonValue("\"Velorim\"")).put("model", parser.createJsonValue("\"Jigger\""))
                    .put("description", parser
                            .createJsonValue("\"Small and powerful, the Jigger is the best ride for the smallest of tikes! "
                                    + "This is the tiniest kids’ pedal bike on the market available without a coaster brake, the Jigger "
                                    + "is the vehicle of choice for the rare tenacious little rider raring to go.\""))
                    .put("price", parser.createJsonValue("270")).put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"Bicyk\""))
                            .put("model", parser.createJsonValue("\"Hillcraft\""))
                            .put("description",
                                    parser.createJsonValue("\"Kids want to ride with as little weight as possible. Especially "
                                            + "on an incline! They may be at the age when a 27.5'' wheel bike is just too clumsy coming "
                                            + "off a 24'' bike. The Hillcraft 26 is just the solution they need!\""))
                            .put("price", parser.createJsonValue("1200")).put("condition", parser.createJsonValue("\"used\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"Nord\""))
                            .put("model", parser.createJsonValue("\"Chook air 5\""))
                            .put("description",
                                    parser.createJsonValue("\"The Chook Air 5  gives kids aged six years and older a durable "
                                            + "and uberlight mountain bike for their first experience on tracks and easy cruising through "
                                            + "forests and fields. The lower  top tube makes it easy to mount and dismount in any "
                                            + "situation, giving your kids greater safety on the trails.\""))
                            .put("price", parser.createJsonValue("815")).put("condition", parser.createJsonValue("\"used\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"Eva\""))
                            .put("model", parser.createJsonValue("\"Eva 291\""))
                            .put("description",
                                    parser.createJsonValue("\"The sister company to Nord, Eva launched in 2005 as the first "
                                            + "and only women-dedicated bicycle brand. Designed by women for women, allEva bikes "
                                            + "are optimized for the feminine physique using analytics from a body metrics database. "
                                            + "If you like 29ers, try the Eva 291. It’s a brand new bike for 2022.. This "
                                            + "full-suspension, cross-country ride has been designed for velocity. The 291 has "
                                            + "100mm of front and rear travel, a superlight aluminum frame and fast-rolling "
                                            + "29-inch wheels. Yippee!\""))
                            .put("price", parser.createJsonValue("3400")).put("condition", parser.createJsonValue("\"used\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"Noka Bikes\""))
                            .put("model", parser.createJsonValue("\"Kahuna\""))
                            .put("description",
                                    parser.createJsonValue("\"Whether you want to try your hand at XC racing or are looking "
                                            + "for a lively trail bike that's just as inspiring on the climbs as it is over rougher "
                                            + "ground, the Wilder is one heck of a bike built specifically for short women. Both the "
                                            + "frames and components have been tweaked to include a women’s saddle, different bars "
                                            + "and unique colourway.\""))
                            .put("price", parser.createJsonValue("3200")).put("condition", parser.createJsonValue("\"used\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"Breakout\""))
                            .put("model", parser.createJsonValue("\"XBN 2.1 Alloy\""))
                            .put("description",
                                    parser.createJsonValue("\"The XBN 2.1 Alloy is our entry-level road bike – but that’s "
                                            + "not to say that it’s a basic machine. With an internal weld aluminium frame, a full "
                                            + "carbon fork, and the slick-shifting Claris gears from Shimano’s, this is a bike which "
                                            + "doesn’t break the bank and delivers craved performance.\""))
                            .put("price", parser.createJsonValue("810")).put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"ScramBikes\""))
                            .put("model", parser.createJsonValue("\"WattBike\""))
                            .put("description",
                                    parser.createJsonValue("\"The WattBike is the best e-bike for people who still "
                                            + "feel young at heart. It has a Bafang 1000W mid-drive system and a 48V 17.5AH "
                                            + "Samsung Lithium-Ion battery, allowing you to ride for more than 60 miles on one "
                                            + "charge. It’s great for tackling hilly terrain or if you just fancy a more "
                                            + "leisurely ride. With three working modes, you can choose between E-bike, "
                                            + "assisted bicycle, and normal bike modes.\""))
                            .put("price", parser.createJsonValue("2300")).put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"Peaknetic\""))
                            .put("model", parser.createJsonValue("\"Secto\""))
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
                            .put("price", parser.createJsonValue("430")).put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"nHill\""))
                            .put("model", parser.createJsonValue("\"Summit\""))
                            .put("description",
                                    parser.createJsonValue("\"This budget mountain bike from nHill performs well both "
                                            + "on bike paths and on the trail. The fork with 100mm of travel absorbs rough "
                                            + "terrain. Fat Kenda Booster tires give you grip in corners and on wet trails. "
                                            + "The Shimano Tourney drivetrain offered enough gears for finding a comfortable "
                                            + "pace to ride uphill, and the Tektro hydraulic disc brakes break smoothly. "
                                            + "Whether you want an affordable bike that you can take to work, but also take "
                                            + "trail in mountains on the weekends or you’re just after a stable, comfortable "
                                            + "ride for the bike path, the Summit gives a good value for money.\""))
                            .put("price", parser.createJsonValue("1200")).put("condition", parser.createJsonValue("\"new\"")),
                    parser.createJsonObject().put("brand", parser.createJsonValue("\"ThrillCycle\""))
                            .put("model", parser.createJsonValue("\"BikeShind\""))
                            .put("description",
                                    parser.createJsonValue("\"An artsy,  retro-inspired bicycle that’s as "
                                            + "functional as it is pretty: The ThrillCycle steel frame offers a smooth ride. "
                                            + "A 9-speed drivetrain has enough gears for coasting in the city, but we wouldn’t "
                                            + "suggest taking it to the mountains. Fenders protect you from mud, and a rear "
                                            + "basket lets you transport groceries, flowers and books. The ThrillCycle comes "
                                            + "with a limited lifetime warranty, so this little guy will last you long "
                                            + "past graduation.\""))
                            .put("price", parser.createJsonValue("815"))
                            .put("condition", parser.createJsonValue("\"refurbished\"")));

            CompletableFuture<?>[] bikeFutures = new CompletableFuture[bicycleJsons.size()];

            for (int i = 0; i < bicycleJsons.size(); i++) {
                bikeFutures[i] = asyncCommands.jsonSet("bicycle:" + i, JsonPath.ROOT_PATH, bicycleJsons.get(i))
                        .toCompletableFuture();
            }

            CompletableFuture.allOf(bikeFutures).join();

            // STEP_START range1
            CompletableFuture<SearchReply<String, String>> priceResults = asyncCommands
                    .ftSearch("idx:bicycle", "@price:[500 1000]").thenApply(res -> {
                        res.getResults().stream().sorted((doc1, doc2) -> doc1.getId().compareTo(doc2.getId())).forEach(doc -> {
                            System.out.printf("ID: %s\n", doc.getId());
                        });
                        // >>> ID: bicycle:2
                        // >>> ID: bicycle:5
                        // >>> ID: bicycle:9
                        // REMOVE_START
                        assertThat(res.getResults().size()).isEqualTo(3);
                        assertThat(res.getResults().stream().map(SearchReply.SearchResult<String, String>::getId).sorted()
                                .toArray()).containsExactly("bicycle:2", "bicycle:5", "bicycle:9");
                        // REMOVE_END
                        return res;
                    }).toCompletableFuture();
            // STEP_END

            // STEP_START range2

            // `Filter` is not supported.

            // STEP_END

            // STEP_START range3
            SearchArgs<String, String> priceSearchArgs2 = SearchArgs.<String, String> builder().returnField("price").build();

            CompletableFuture<SearchReply<String, String>> priceResults2 = asyncCommands
                    .ftSearch("idx:bicycle", "@price:[(1000 +inf]", priceSearchArgs2).thenApply(res -> {
                        res.getResults().stream().sorted((doc1, doc2) -> doc1.getId().compareTo(doc2.getId())).forEach(doc -> {
                            System.out.printf("ID: %s, price: %s\n", doc.getId(), doc.getFields().get("price"));
                        });
                        // >>> ID: bicycle:1, price: 1200
                        // >>> ID: bicycle:3, price: 3400
                        // >>> ID: bicycle:4, price: 3200
                        // >>> ID: bicycle:6, price: 2300
                        // >>> ID: bicycle:8, price: 1200
                        // REMOVE_START
                        assertThat(res.getResults().size()).isEqualTo(5);
                        assertThat(res.getResults().stream().map(SearchReply.SearchResult<String, String>::getId).sorted()
                                .toArray()).containsExactly("bicycle:1", "bicycle:3", "bicycle:4", "bicycle:6", "bicycle:8");
                        // REMOVE_END
                        return res;
                    }).toCompletableFuture();
            // STEP_END

            // STEP_START range4
            SearchArgs<String, String> priceSearchArgs3 = SearchArgs.<String, String> builder().returnField("price")
                    .sortBy(SortByArgs.<String> builder().attribute("price").build()).limit(0, 5).build();

            CompletableFuture<SearchReply<String, String>> priceResults3 = asyncCommands
                    .ftSearch("idx:bicycle", "@price:[-inf 2000]", priceSearchArgs3).thenApply(res -> {
                        res.getResults().stream().sorted((doc1, doc2) -> doc1.getId().compareTo(doc2.getId())).forEach(doc -> {
                            System.out.printf("ID: %s, price: %s\n", doc.getId(), doc.getFields().get("price"));
                        });
                        // >>> ID: bicycle:0, price: 270
                        // >>> ID: bicycle:2, price: 815
                        // >>> ID: bicycle:5, price: 810
                        // >>> ID: bicycle:7, price: 430
                        // >>> ID: bicycle:9, price: 815
                        // REMOVE_START
                        assertThat(res.getResults().size()).isEqualTo(5);
                        assertThat(res.getResults().stream().map(SearchReply.SearchResult<String, String>::getId).sorted()
                                .toArray()).containsExactly("bicycle:0", "bicycle:2", "bicycle:5", "bicycle:7", "bicycle:9");
                        // REMOVE_END
                        return res;
                    }).toCompletableFuture();
            // STEP_END

            CompletableFuture.allOf(priceResults, priceResults2, priceResults3).join();

        } finally {
            redisClient.shutdown();
        }
    }

}
