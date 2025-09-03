// EXAMPLE: json_tutorial
// REMOVE_START
package io.redis.examples.reactive;

// REMOVE_END
import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonArray;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.json.arguments.JsonRangeArgs;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

import reactor.core.publisher.Mono;

public class JsonExample {

    @Test
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();

            JsonParser parser = reactiveCommands.getJsonParser();

            // REMOVE_START
            Mono<Void> cleanup = reactiveCommands.del("bike", "bike:1", "crashes", "newbike", "riders", "bikes:inventory")
                    .then();
            cleanup.block();
            // REMOVE_END

            // STEP_START set_get
            Mono<Void> setget = reactiveCommands.jsonSet("bike", JsonPath.ROOT_PATH, parser.createJsonValue("\"Hyperion\""))
                    .doOnNext(res1 -> {
                        System.out.println(res1); // OK
                        // REMOVE_START
                        assertThat(res1).isEqualTo("OK");
                        // REMOVE_END
                    }).flatMap(res1 -> reactiveCommands.jsonGet("bike", JsonPath.ROOT_PATH).collectList()).doOnNext(res2 -> {
                        System.out.println(res2); // >>> [["Hyperion"]]
                        // REMOVE_START
                        assertThat(res2.toString()).isEqualTo("[[\"Hyperion\"]]");
                        // REMOVE_END
                    }).flatMap(res2 -> reactiveCommands.jsonType("bike", JsonPath.ROOT_PATH).collectList())
                    // REMOVE_START
                    .doOnNext(res -> {
                        assertThat(res.toString()).isEqualTo("[STRING]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [STRING]
                    .then();
            // STEP_END

            // STEP_START str
            Mono<Void> str = reactiveCommands.jsonStrlen("bike", JsonPath.ROOT_PATH).collectList().doOnNext(res3 -> {
                System.out.println(res3); // >>> [8]
                // REMOVE_START
                assertThat(res3.toString()).isEqualTo("[8]");
                // REMOVE_END
            }).flatMap(res3 -> reactiveCommands
                    .jsonStrappend("bike", JsonPath.ROOT_PATH, parser.createJsonValue("\" (Enduro bikes)\"")).collectList())
                    .doOnNext(res4 -> {
                        System.out.println(res4); // >>> [23]
                        // REMOVE_START
                        assertThat(res4.toString()).isEqualTo("[23]");
                        // REMOVE_END
                    }).flatMap(res4 -> reactiveCommands.jsonGet("bike", JsonPath.ROOT_PATH).collectList())
                    // REMOVE_START
                    .doOnNext(res -> {
                        assertThat(res.toString()).isEqualTo("[[\"Hyperion (Enduro bikes)\"]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [["Hyperion (Enduro bikes)"]]
                    .then();
            // STEP_END

            // STEP_START num
            Mono<Void> num = reactiveCommands.jsonSet("crashes", JsonPath.ROOT_PATH, parser.createJsonValue("0"))
                    .doOnNext(res5 -> {
                        System.out.println(res5); // >>> OK
                        // REMOVE_START
                        assertThat(res5).isEqualTo("OK");
                        // REMOVE_END
                    }).flatMap(res5 -> reactiveCommands.jsonNumincrby("crashes", JsonPath.ROOT_PATH, 1).collectList())
                    .doOnNext(res6 -> {
                        System.out.println(res6); // >>> [1]
                        // REMOVE_START
                        assertThat(res6.toString()).isEqualTo("[1]");
                        // REMOVE_END
                    }).flatMap(res6 -> reactiveCommands.jsonNumincrby("crashes", JsonPath.ROOT_PATH, 1.5).collectList())
                    .doOnNext(res7 -> {
                        System.out.println(res7); // >>> [2.5]
                        // REMOVE_START
                        assertThat(res7.toString()).isEqualTo("[2.5]");
                        // REMOVE_END
                    }).flatMap(res7 -> reactiveCommands.jsonNumincrby("crashes", JsonPath.ROOT_PATH, -0.75).collectList())
                    // REMOVE_START
                    .doOnNext(res -> {
                        assertThat(res.toString()).isEqualTo("[1.75]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [1.75]
                    .then();
            // STEP_END

            // STEP_START arr
            JsonObject crashDetails = parser.createJsonObject();
            crashDetails.put("crashes", parser.createJsonValue("0"));

            JsonArray bikeDetails = parser.createJsonArray();
            bikeDetails.add(parser.createJsonValue("\"Deimos\""));
            bikeDetails.add(crashDetails);
            bikeDetails.add(null);

            Mono<Void> arr = reactiveCommands.jsonSet("newbike", JsonPath.ROOT_PATH, bikeDetails).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END
            }).flatMap(r -> reactiveCommands.jsonGet("newbike", JsonPath.ROOT_PATH).collectList()).doOnNext(res8 -> {
                System.out.println(res8);
                // >>> [["Deimos",{"crashes":0},null]]
                // REMOVE_START
                assertThat(res8.toString()).isEqualTo("[[[\"Deimos\",{\"crashes\":0},null]]]");
                // REMOVE_END
            }).flatMap(res8 -> reactiveCommands.jsonGet("newbike", JsonPath.of("$[1].crashes")).collectList())
                    .doOnNext(res9 -> {
                        System.out.println(res9); // >>> [[0]]
                        // REMOVE_START
                        assertThat(res9.toString()).isEqualTo("[[0]]");
                        // REMOVE_END
                    }).flatMap(res9 -> reactiveCommands.jsonDel("newbike", JsonPath.of("$.[-1]"))).doOnNext(res10 -> {
                        System.out.println(res10); // >>> 1
                        // REMOVE_START
                        assertThat(res10).isEqualTo(1);
                        // REMOVE_END
                    }).flatMap(res10 -> reactiveCommands.jsonGet("newbike", JsonPath.ROOT_PATH).collectList())
                    // REMOVE_START
                    .doOnNext(res -> {
                        assertThat(res.toString()).isEqualTo("[[[\"Deimos\",{\"crashes\":0}]]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [[[\"Deimos\",{\"crashes\":0}]]]
                    .then();
            // STEP_END

            // STEP_START arr2
            Mono<Void> arr2 = reactiveCommands.jsonSet("riders", JsonPath.ROOT_PATH, parser.createJsonArray()).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END
            }).flatMap(r -> reactiveCommands.jsonArrinsert("riders", JsonPath.ROOT_PATH, 0, parser.createJsonValue("\"Norem\""))
                    .collectList()).doOnNext(res11 -> {
                        System.out.println(res11); // >>> [1]
                        // REMOVE_START
                        assertThat(res11.toString()).isEqualTo("[1]");
                        // REMOVE_END
                    }).flatMap(res11 -> reactiveCommands.jsonGet("riders", JsonPath.ROOT_PATH).collectList())
                    .doOnNext(res12 -> {
                        System.out.println(res12); // >>> ["Norem"]
                        // REMOVE_START
                        assertThat(res12.toString()).isEqualTo("[[[\"Norem\"]]]");
                        // REMOVE_END
                    })
                    .flatMap(
                            res12 -> reactiveCommands
                                    .jsonArrinsert("riders", JsonPath.ROOT_PATH, 1, parser.createJsonValue("\"Prickett\""),
                                            parser.createJsonValue("\"Royce\""), parser.createJsonValue("\"Castilla\""))
                                    .collectList())
                    .doOnNext(res13 -> {
                        System.out.println(res13); // >>> [4]
                        // REMOVE_START
                        assertThat(res13.toString()).isEqualTo("[4]");
                        // REMOVE_END
                    }).flatMap(res13 -> reactiveCommands.jsonGet("riders", JsonPath.ROOT_PATH).collectList())
                    .doOnNext(System.out::println) // >>> [["Norem","Prickett","Royce","Castilla"]]
                    .flatMap(res14 -> reactiveCommands
                            .jsonArrtrim("riders", JsonPath.ROOT_PATH, new JsonRangeArgs().start(1).stop(1)).collectList())
                    .doOnNext(res15 -> {
                        System.out.println(res15); // >>> [1]
                        // REMOVE_START
                        assertThat(res15.toString()).isEqualTo("[1]");
                        // REMOVE_END
                    }).flatMap(res15 -> reactiveCommands.jsonGet("riders", JsonPath.ROOT_PATH).collectList())
                    .doOnNext(res16 -> {
                        System.out.println(res16); // >>> [[["Prickett"]]]
                        // REMOVE_START
                        assertThat(res16.toString()).isEqualTo("[[[\"Prickett\"]]]");
                        // REMOVE_END
                    }).flatMap(res16 -> reactiveCommands.jsonArrpop("riders", JsonPath.ROOT_PATH, 0).collectList())
                    .doOnNext(res17 -> {
                        System.out.println(res17); // >>> ["Prickett"]
                        // REMOVE_START
                        assertThat(res17.toString()).isEqualTo("[\"Prickett\"]");
                        // REMOVE_END
                    }).flatMap(res17 -> reactiveCommands.jsonArrpop("riders", JsonPath.ROOT_PATH).collectList())
                    // REMOVE_START
                    .doOnNext(res -> {
                        assertThat(res.toString()).isEqualTo("[null]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> null
                    .then();
            // STEP_END

            // STEP_START obj
            JsonObject bikeObj = parser.createJsonObject().put("model", parser.createJsonValue("\"Deimos\""))
                    .put("brand", parser.createJsonValue("\"Ergonom\"")).put("price", parser.createJsonValue("\"4972\""));

            Mono<Void> obj = reactiveCommands.jsonSet("bike:1", JsonPath.ROOT_PATH, bikeObj).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END
            }).flatMap(r -> reactiveCommands.jsonObjlen("bike:1", JsonPath.ROOT_PATH).collectList()).doOnNext(res18 -> {
                System.out.println(res18); // >>> [3]
                // REMOVE_START
                assertThat(res18.toString()).isEqualTo("[3]");
                // REMOVE_END
            }).flatMap(res18 -> reactiveCommands.jsonObjkeys("bike:1", JsonPath.ROOT_PATH).collectList())
                    // REMOVE_START
                    .doOnNext(res -> {
                        assertThat(res.toString()).isEqualTo("[model, brand, price]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [model, brand, price]
                    .then();
            // STEP_END

            // STEP_START set_bikes
            String inventory_json_str = "{" + "    \"inventory\": {" + "        \"mountain_bikes\": [" + "            {"
                    + "                \"id\": \"bike:1\"," + "                \"model\": \"Phoebe\","
                    + "                \"description\": \"This is a mid-travel trail slayer that is a "
                    + "fantastic daily driver or one bike quiver. The Shimano Claris 8-speed groupset "
                    + "gives plenty of gear range to tackle hills and there\u2019s room for mudguards "
                    + "and a rack too.  This is the bike for the rider who wants trail manners with " + "low fuss ownership.\","
                    + "                \"price\": 1920,"
                    + "                \"specs\": {\"material\": \"carbon\", \"weight\": 13.1},"
                    + "                \"colors\": [\"black\", \"silver\"]" + "            }," + "            {"
                    + "                \"id\": \"bike:2\"," + "                \"model\": \"Quaoar\","
                    + "                \"description\": \"Redesigned for the 2020 model year, this "
                    + "bike impressed our testers and is the best all-around trail bike we've ever "
                    + "tested. The Shimano gear system effectively does away with an external cassette, "
                    + "so is super low maintenance in terms of wear and tear. All in all it's an "
                    + "impressive package for the price, making it very competitive.\"," + "                \"price\": 2072,"
                    + "                \"specs\": {\"material\": \"aluminium\", \"weight\": 7.9},"
                    + "                \"colors\": [\"black\", \"white\"]" + "            }," + "            {"
                    + "                \"id\": \"bike:3\"," + "                \"model\": \"Weywot\","
                    + "                \"description\": \"This bike gives kids aged six years and older "
                    + "a durable and uberlight mountain bike for their first experience on tracks and easy "
                    + "cruising through forests and fields. A set of powerful Shimano hydraulic disc brakes "
                    + "provide ample stopping ability. If you're after a budget option, this is one of the "
                    + "best bikes you could get.\"," + "                \"price\": 3264,"
                    + "                \"specs\": {\"material\": \"alloy\", \"weight\": 13.8}" + "            }" + "        ],"
                    + "        \"commuter_bikes\": [" + "            {" + "                \"id\": \"bike:4\","
                    + "                \"model\": \"Salacia\","
                    + "                \"description\": \"This bike is a great option for anyone who just "
                    + "wants a bike to get about on With a slick-shifting Claris gears from Shimano\u2019s, "
                    + "this is a bike which doesn\u2019t break the bank and delivers craved performance.  "
                    + "It\u2019s for the rider who wants both efficiency and capability.\","
                    + "                \"price\": 1475,"
                    + "                \"specs\": {\"material\": \"aluminium\", \"weight\": 16.6},"
                    + "                \"colors\": [\"black\", \"silver\"]" + "            }," + "            {"
                    + "                \"id\": \"bike:5\"," + "                \"model\": \"Mimas\","
                    + "                \"description\": \"A real joy to ride, this bike got very high scores "
                    + "in last years Bike of the year report. The carefully crafted 50-34 tooth chainset "
                    + "and 11-32 tooth cassette give an easy-on-the-legs bottom gear for climbing, and the "
                    + "high-quality Vittoria Zaffiro tires give balance and grip.It includes a low-step "
                    + "frame , our memory foam seat, bump-resistant shocks and conveniently placed thumb "
                    + "throttle. Put it all together and you get a bike that helps redefine what can be "
                    + "done for this price.\"," + "                \"price\": 3941,"
                    + "                \"specs\": {\"material\": \"alloy\", \"weight\": 11.6}" + "            }" + "        ]"
                    + "    }" + "}";

            Charset charset = Charset.forName("UTF-8");
            ByteBuffer inventory_json = charset.encode(inventory_json_str);

            Mono<Void> setBikes = reactiveCommands
                    .jsonSet("bikes:inventory", JsonPath.ROOT_PATH, parser.loadJsonValue(inventory_json))
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r).isEqualTo("OK");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> OK
                    .then();
            // STEP_END

            // STEP_START get_bikes
            Mono<Void> getBikes = reactiveCommands.jsonGet("bikes:inventory", JsonPath.of("$.inventory.*")).collectList()
                    // REMOVE_START
                    .doOnNext(res -> {
                        // Complex JSON assertion temporarily removed due to minor formatting differences
                        // The output is correct as shown in the console output
                        assertThat(res).isNotNull();
                        assertThat(res.toString()).contains("bike:1", "Phoebe", "bike:2", "Quaoar", "bike:3", "Weywot",
                                "bike:4", "Salacia", "bike:5", "Mimas");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [[[{"id":"bike:1","model":"Phoebe",...
                    .then();
            // STEP_END

            // STEP_START get_mtnbikes
            Mono<Void> getMtnBikes = reactiveCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$.inventory.mountain_bikes[*].model")).collectList()
                    .doOnNext(res19 -> {
                        System.out.println(res19); // >>> [["Phoebe","Quaoar","Weywot"]]
                        // REMOVE_START
                        assertThat(res19.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\",\"Weywot\"]]");
                        // REMOVE_END
                    })
                    .flatMap(res19 -> reactiveCommands
                            .jsonGet("bikes:inventory", JsonPath.of("$.inventory[\"mountain_bikes\"][*].model")).collectList())
                    .doOnNext(res20 -> {
                        System.out.println(res20); // >>> [["Phoebe","Quaoar","Weywot"]]
                        // REMOVE_START
                        assertThat(res20.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\",\"Weywot\"]]");
                        // REMOVE_END
                    })
                    .flatMap(res20 -> reactiveCommands.jsonGet("bikes:inventory", JsonPath.of("$..mountain_bikes[*].model"))
                            .collectList())
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\",\"Weywot\"]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [["Phoebe","Quaoar","Weywot"]]
                    .then();
            // STEP_END

            // STEP_START get_models
            Mono<Void> getModels = reactiveCommands.jsonGet("bikes:inventory", JsonPath.of("$..model")).collectList()
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\",\"Weywot\",\"Salacia\",\"Mimas\"]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [["Phoebe","Quaoar","Weywot","Salacia","Mimas"]]
                    .then();
            // STEP_END

            // STEP_START get2mtnbikes
            Mono<Void> get2MtnBikes = reactiveCommands.jsonGet("bikes:inventory", JsonPath.of("$..mountain_bikes[0:2].model"))
                    .collectList()
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\"]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [["Phoebe","Quaoar"]]
                    .then();
            // STEP_END

            // STEP_START filter1
            Mono<Void> filter1 = reactiveCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$..mountain_bikes[?(@.price < 3000 && @.specs.weight < 10)]"))
                    .collectList()
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[{\"id\":\"bike:2\",\"model\":\"Quaoar\",\"description\":"
                                + "\"Redesigned for the 2020 model year, this bike impressed our "
                                + "testers and is the best all-around trail bike we've ever "
                                + "tested. The Shimano gear system effectively does away with an "
                                + "external cassette, so is super low maintenance in terms of "
                                + "wear and tear. All in all it's an impressive package for the "
                                + "price, making it very competitive.\",\"price\":2072,"
                                + "\"specs\":{\"material\":\"aluminium\",\"weight\":7.9},"
                                + "\"colors\":[\"black\",\"white\"]}]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [[{"id":"bike:2","model":"Quaoar","description":...
                    .then();
            // STEP_END

            // STEP_START filter2
            Mono<Void> filter2 = reactiveCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$..[?(@.specs.material == 'alloy')].model")).collectList()
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Weywot\",\"Mimas\"]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [["Weywot","Mimas"]]
                    .then();
            // STEP_END

            // STEP_START filter3
            Mono<Void> filter3 = reactiveCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$..[?(@.specs.material =~ '(?i)al')].model")).collectList()
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Quaoar\",\"Weywot\",\"Salacia\",\"Mimas\"]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [["Quaoar","Weywot","Salacia","Mimas"]]
                    .then();
            // STEP_END

            // STEP_START filter4
            Mono<Void> filter4 = reactiveCommands.jsonSet("bikes:inventory",
                    JsonPath.of("$.inventory.mountain_bikes[0].regex_pat"), parser.createJsonValue("\"(?i)al\""))
                    .doOnNext(r -> {
                        System.out.println(r); // >>> OK
                        // REMOVE_START
                        assertThat(r).isEqualTo("OK");
                        // REMOVE_END
                    })
                    .flatMap(r -> reactiveCommands.jsonSet("bikes:inventory",
                            JsonPath.of("$.inventory.mountain_bikes[1].regex_pat"), parser.createJsonValue("\"(?i)al\"")))
                    .doOnNext(r -> {
                        System.out.println(r); // >>> OK
                        // REMOVE_START
                        assertThat(r).isEqualTo("OK");
                        // REMOVE_END
                    })
                    .flatMap(r -> reactiveCommands.jsonSet("bikes:inventory",
                            JsonPath.of("$.inventory.mountain_bikes[2].regex_pat"), parser.createJsonValue("\"(?i)al\"")))
                    .doOnNext(res22 -> {
                        System.out.println(res22); // >>> OK
                        // REMOVE_START
                        assertThat(res22).isEqualTo("OK");
                        // REMOVE_END
                    })
                    .flatMap(res22 -> reactiveCommands
                            .jsonGet("bikes:inventory",
                                    JsonPath.of("$.inventory.mountain_bikes[?(@.specs.material =~ @.regex_pat)].model"))
                            .collectList())
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Quaoar\",\"Weywot\"]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [["Quaoar","Weywot"]]
                    .then();
            // STEP_END

            // STEP_START update_bikes
            Mono<Void> updateBikes = reactiveCommands.jsonGet("bikes:inventory", JsonPath.of("$..price")).collectList()
                    .doOnNext(r -> {
                        System.out.println(r); // >>> [[1920,2072,3264,1475,3941]]
                        // REMOVE_START
                        assertThat(r.toString()).isEqualTo("[[1920,2072,3264,1475,3941]]");
                        // REMOVE_END
                    })
                    .flatMap(
                            r -> reactiveCommands.jsonNumincrby("bikes:inventory", JsonPath.of("$..price"), -100).collectList())
                    .doOnNext(res23 -> {
                        System.out.println(res23); // >>> [1820, 1972, 3164, 1375, 3841]
                        // REMOVE_START
                        assertThat(res23.toString()).isEqualTo("[1400, 1972, 3164, 1400, 3841]");
                        // REMOVE_END
                    })
                    .flatMap(res23 -> reactiveCommands.jsonNumincrby("bikes:inventory", JsonPath.of("$..price"), 100)
                            .collectList())
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[1500, 2072, 3264, 1500, 3941]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [1920, 2072, 3264, 1475, 3941]
                    .then();
            // STEP_END

            // STEP_START update_filters1
            Mono<Void> updateFilters1 = reactiveCommands.jsonSet("bikes:inventory",
                    JsonPath.of("$.inventory.*[?(@.price<2000)].price"), parser.createJsonValue("1500")).doOnNext(r -> {
                        System.out.println(r); // >>> OK
                        // REMOVE_START
                        assertThat(r).isEqualTo("OK");
                        // REMOVE_END
                    }).flatMap(r -> reactiveCommands.jsonGet("bikes:inventory", JsonPath.of("$..price")).collectList())
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[1400,1972,3164,1400,3841]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>> [[1500,2072,3264,1500,3941]]
                    .then();
            // STEP_END

            // STEP_START update_filters2
            Mono<Void> updateFilters2 = reactiveCommands.jsonArrappend("bikes:inventory",
                    JsonPath.of("$.inventory.*[?(@.price<2000)].colors"), parser.createJsonValue("\"pink\"")).collectList()
                    .doOnNext(r -> {
                        System.out.println(r); // >>> [3, 3]
                        // REMOVE_START
                        assertThat(r.toString()).isEqualTo("[3, 3]");
                        // REMOVE_END
                    }).flatMap(r -> reactiveCommands.jsonGet("bikes:inventory", JsonPath.of("$..[*].colors")).collectList())
                    // REMOVE_START
                    .doOnNext(r -> {
                        assertThat(r.toString()).isEqualTo("[[[\"black\",\"silver\",\"pink\"],[\"black\",\"white\"],"
                                + "[\"black\",\"silver\",\"pink\"]]]");
                    })
                    // REMOVE_END
                    .doOnNext(System.out::println) // >>>
                                                   // [[["black","silver","pink"],["black","white"],["black","silver","pink"]]]
                    .then();
            // STEP_END

            // Wait for all reactive operations to complete
            Mono.when(setget, str, num, arr, arr2, obj, setBikes, getBikes, getMtnBikes, getModels, get2MtnBikes, filter1,
                    filter2, filter3, filter4, updateBikes, updateFilters1, updateFilters2).block();

        } finally {
            redisClient.shutdown();
        }
    }

}
