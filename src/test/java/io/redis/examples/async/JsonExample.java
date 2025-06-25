// EXAMPLE: json_tutorial
// REMOVE_START
package io.redis.examples.async;

// REMOVE_END
import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonArray;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.json.arguments.JsonRangeArgs;

import java.util.concurrent.CompletableFuture;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class JsonExample {

    @Test
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();

            JsonParser parser = asyncCommands.getJsonParser();

            // REMOVE_START
            asyncCommands.del("bike", "bike:1", "crashes", "newbike", "riders", "bikes:inventory").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START set_get
            CompletableFuture<Void> setget = asyncCommands
                    .jsonSet("bike", JsonPath.ROOT_PATH, parser.createJsonValue("\"Hyperion\"")).thenCompose(res1 -> {
                        System.out.println(res1); // OK
                        // REMOVE_START
                        assertThat(res1).isEqualTo("OK");
                        // REMOVE_END

                        return asyncCommands.jsonGet("bike", JsonPath.ROOT_PATH);
                    }).thenCompose(res2 -> {
                        System.out.println(res2); // >>> [["Hyperion"]]
                        // REMOVE_START
                        assertThat(res2.toString()).isEqualTo("[[\"Hyperion\"]]");
                        // REMOVE_END

                        return asyncCommands.jsonType("bike", JsonPath.ROOT_PATH);
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[STRING]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [STRING]
                    .toCompletableFuture();
            // STEP_END
            // HIDE_START
            setget.join();
            // HIDE_END

            // STEP_START str
            CompletableFuture<Void> str = asyncCommands.jsonStrlen("bike", JsonPath.ROOT_PATH).thenCompose(res3 -> {
                System.out.println(res3); // >>> [8]
                // REMOVE_START
                assertThat(res3.toString()).isEqualTo("[8]");
                // REMOVE_END

                return asyncCommands.jsonStrappend("bike", JsonPath.ROOT_PATH, parser.createJsonValue("\" (Enduro bikes)\""));
            }).thenCompose(res4 -> {
                System.out.println(res4); // >>> [23]
                // REMOVE_START
                assertThat(res4.toString()).isEqualTo("[23]");
                // REMOVE_END

                return asyncCommands.jsonGet("bike", JsonPath.ROOT_PATH);
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[[\"Hyperion (Enduro bikes)\"]]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [["Hyperion (Enduro bikes)"]]
                    .toCompletableFuture();
            // STEP_END
            // HIDE_START
            str.join();
            // HIDE_END

            // STEP_START num
            CompletableFuture<Void> num = asyncCommands.jsonSet("crashes", JsonPath.ROOT_PATH, parser.createJsonValue("0"))
                    .thenCompose(res5 -> {
                        System.out.println(res5); // >>> OK
                        // REMOVE_START
                        assertThat(res5).isEqualTo("OK");
                        // REMOVE_END

                        return asyncCommands.jsonNumincrby("crashes", JsonPath.ROOT_PATH, 1);
                    }).thenCompose(res6 -> {
                        System.out.println(res6); // >>> [1]
                        // REMOVE_START
                        assertThat(res6.toString()).isEqualTo("[1]");
                        // REMOVE_END

                        return asyncCommands.jsonNumincrby("crashes", JsonPath.ROOT_PATH, 1.5);
                    }).thenCompose(res7 -> {
                        System.out.println(res7); // >>> [2.5]
                        // REMOVE_START
                        assertThat(res7.toString()).isEqualTo("[2.5]");
                        // REMOVE_END

                        return asyncCommands.jsonNumincrby("crashes", JsonPath.ROOT_PATH, -0.75);
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[1.75]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> [1.75]
                    .toCompletableFuture();
            // STEP_END
            // HIDE_START
            num.join();
            // HIDE_END

            // STEP_START arr
            JsonObject crashDetails = parser.createJsonObject();
            crashDetails.put("crashes", parser.createJsonValue("0"));

            JsonArray bikeDetails = parser.createJsonArray();
            bikeDetails.add(parser.createJsonValue("\"Deimos\""));
            bikeDetails.add(crashDetails);
            bikeDetails.add(null);

            CompletableFuture<Void> arr = asyncCommands.jsonSet("newbike", JsonPath.ROOT_PATH, bikeDetails).thenCompose(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END

                return asyncCommands.jsonGet("newbike", JsonPath.ROOT_PATH);
            }).thenCompose(res8 -> {
                System.out.println(res8);
                // >>> [["Deimos",{"crashes":0},null]]
                // REMOVE_START
                assertThat(res8.toString()).isEqualTo("[[[\"Deimos\",{\"crashes\":0},null]]]");
                // REMOVE_END

                return asyncCommands.jsonGet("newbike", JsonPath.of("$[1].crashes"));
            }).thenCompose(res9 -> {
                System.out.println(res9); // >>> [[0]]
                // REMOVE_START
                assertThat(res9.toString()).isEqualTo("[[0]]");
                // REMOVE_END

                return asyncCommands.jsonDel("newbike", JsonPath.of("$.[-1]"));
            }).thenCompose(res10 -> {
                System.out.println(res10); // >>> 1
                // REMOVE_START
                assertThat(res10).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.jsonGet("newbike", JsonPath.ROOT_PATH);
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[[[\"Deimos\",{\"crashes\":0}]]]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [[[\"Deimos\",{\"crashes\":0}]]]
                    .toCompletableFuture();
            // STEP_END
            arr.join();

            // STEP_START arr2
            CompletableFuture<Void> arr2 = asyncCommands.jsonSet("riders", JsonPath.ROOT_PATH, parser.createJsonArray())
                    .thenCompose(r -> {
                        System.out.println(r); // >>> OK
                        // REMOVE_START
                        assertThat(r).isEqualTo("OK");
                        // REMOVE_END

                        return asyncCommands.jsonArrinsert("riders", JsonPath.ROOT_PATH, 0,
                                parser.createJsonValue("\"Norem\""));
                    }).thenCompose(res11 -> {
                        System.out.println(res11); // >>> [1]
                        // REMOVE_START
                        assertThat(res11.toString()).isEqualTo("[1]");
                        // REMOVE_END

                        return asyncCommands.jsonGet("riders", JsonPath.ROOT_PATH);
                    })

                    .thenCompose(res12 -> {
                        System.out.println(res12); // >>> ["Norem"]
                        // REMOVE_START
                        assertThat(res12.toString()).isEqualTo("[[[\"Norem\"]]]");
                        // REMOVE_END

                        return asyncCommands.jsonArrinsert("riders", JsonPath.ROOT_PATH, 1,
                                parser.createJsonValue("\"Prickett\""), parser.createJsonValue("\"Royce\""),
                                parser.createJsonValue("\"Castilla\""));
                    }).thenCompose(res13 -> {
                        System.out.println(res13); // >>> [4]
                        // REMOVE_START
                        assertThat(res13.toString()).isEqualTo("[4]");
                        // REMOVE_END

                        return asyncCommands.jsonGet("riders", JsonPath.ROOT_PATH);
                    }).thenCompose(res14 -> {
                        System.out.println(res14); // >>> ["Norem","Prickett","Royce","Castilla"]
                        //
                        return asyncCommands.jsonArrtrim("riders", JsonPath.ROOT_PATH, new JsonRangeArgs().start(1).stop(1));
                    }).thenCompose(res15 -> {
                        System.out.println(res15); // >>> [1]
                        // REMOVE_START
                        assertThat(res15.toString()).isEqualTo("[1]");
                        // REMOVE_END

                        return asyncCommands.jsonGet("riders", JsonPath.ROOT_PATH);
                    }).thenCompose(res16 -> {
                        System.out.println(res16); // >>> [[["Prickett"]]]
                        // REMOVE_START
                        assertThat(res16.toString()).isEqualTo("[[[\"Prickett\"]]]");
                        // REMOVE_END
                        return asyncCommands.jsonArrpop("riders", JsonPath.ROOT_PATH, 0);
                    }).thenCompose(res17 -> {
                        System.out.println(res17); // >>> ["Prickett"]
                        // REMOVE_START
                        assertThat(res17.toString()).isEqualTo("[\"Prickett\"]");
                        // REMOVE_END
                        return asyncCommands.jsonArrpop("riders", JsonPath.ROOT_PATH);
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[null]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> null
                    .toCompletableFuture();
            // STEP_END
            arr2.join();

            // STEP_START obj
            JsonObject bikeObj = parser.createJsonObject().put("model", parser.createJsonValue("\"Deimos\""))
                    .put("brand", parser.createJsonValue("\"Ergonom\"")).put("price", parser.createJsonValue("\"4972\""));

            CompletableFuture<Void> obj = asyncCommands.jsonSet("bike:1", JsonPath.ROOT_PATH, bikeObj).thenCompose(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END

                return asyncCommands.jsonObjlen("bike:1", JsonPath.ROOT_PATH);
            }).thenCompose(res18 -> {
                System.out.println(res18); // >>> [3]
                // REMOVE_START
                assertThat(res18.toString()).isEqualTo("[3]");
                // REMOVE_END

                return asyncCommands.jsonObjkeys("bike:1", JsonPath.ROOT_PATH);
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[model, brand, price]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [model, brand, price]
                    .toCompletableFuture();
            // STEP_END
            obj.join();

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

            CompletableFuture<Void> setBikes = asyncCommands
                    .jsonSet("bikes:inventory", JsonPath.ROOT_PATH, parser.loadJsonValue(inventory_json))
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r).isEqualTo("OK");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> OK
                    .toCompletableFuture();
            // STEP_END
            setBikes.join();

            // STEP_START get_bikes
            CompletableFuture<Void> getBikes = asyncCommands.jsonGet("bikes:inventory", JsonPath.of("$.inventory.*"))
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[[[{\"id\":\"bike:1\",\"model\":\"Phoebe\","
                                + "\"description\":\"This is a mid-travel trail slayer that is a "
                                + "fantastic daily driver or one bike quiver. The Shimano Claris "
                                + "8-speed groupset gives plenty of gear range to tackle hills "
                                + "and there’s room for mudguards and a rack too.  This is the bike "
                                + "for the rider who wants trail manners with low fuss ownership.\","
                                + "\"price\":1920,\"specs\":{\"material\":\"carbon\",\"weight\":13.1},"
                                + "\"colors\":[\"black\",\"silver\"]},{\"id\":\"bike:2\","
                                + "\"model\":\"Quaoar\",\"description\":\"Redesigned for the 2020 "
                                + "model year, this bike impressed our testers and is the best "
                                + "all-around trail bike we've ever tested. The Shimano gear system "
                                + "effectively does away with an external cassette, so is super low "
                                + "maintenance in terms of wear and tear. All in all it's an "
                                + "impressive package for the price, making it very competitive.\","
                                + "\"price\":2072,\"specs\":{\"material\":\"aluminium\","
                                + "\"weight\":7.9},\"colors\":[\"black\",\"white\"]},"
                                + "{\"id\":\"bike:3\",\"model\":\"Weywot\",\"description\":"
                                + "\"This bike gives kids aged six years and older a durable "
                                + "and uberlight mountain bike for their first experience on tracks "
                                + "and easy cruising through forests and fields. A set of powerful "
                                + "Shimano hydraulic disc brakes provide ample stopping ability. If "
                                + "you're after a budget option, this is one of the best bikes you "
                                + "could get.\",\"price\":3264,\"specs\":{\"material\":\"alloy\","
                                + "\"weight\":13.8}}],[{\"id\":\"bike:4\",\"model\":\"Salacia\","
                                + "\"description\":\"This bike is a great option for anyone who "
                                + "just wants a bike to get about on With a slick-shifting Claris "
                                + "gears from Shimano’s, this is a bike which doesn’t break the "
                                + "bank and delivers craved performance.  It’s for the rider who "
                                + "wants both efficiency and capability.\",\"price\":1475,"
                                + "\"specs\":{\"material\":\"aluminium\",\"weight\":16.6},"
                                + "\"colors\":[\"black\",\"silver\"]},{\"id\":\"bike:5\","
                                + "\"model\":\"Mimas\",\"description\":\"A real joy to ride, "
                                + "this bike got very high scores in last years Bike of the year "
                                + "report. The carefully crafted 50-34 tooth chainset and 11-32 "
                                + "tooth cassette give an easy-on-the-legs bottom gear for "
                                + "climbing, and the high-quality Vittoria Zaffiro tires give "
                                + "balance and grip.It includes a low-step frame , our memory "
                                + "foam seat, bump-resistant shocks and conveniently placed thumb "
                                + "throttle. Put it all together and you get a bike that helps "
                                + "redefine what can be done for this price.\",\"price\":3941,"
                                + "\"specs\":{\"material\":\"alloy\",\"weight\":11.6}}]]]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [[[{"id":"bike:1","model":"Phoebe",...
                    .toCompletableFuture();
            // STEP_END
            getBikes.join();

            // STEP_START get_mtnbikes
            CompletableFuture<Void> getMtnBikes = asyncCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$.inventory.mountain_bikes[*].model")).thenCompose(res19 -> {
                        System.out.println(res19); // >>> [["Phoebe","Quaoar","Weywot"]]
                        // REMOVE_START
                        assertThat(res19.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\",\"Weywot\"]]");
                        // REMOVE_END
                        return asyncCommands.jsonGet("bikes:inventory",
                                JsonPath.of("$.inventory[\"mountain_bikes\"][*].model"));
                    }).thenCompose(res20 -> {
                        System.out.println(res20); // >>> [["Phoebe","Quaoar","Weywot"]]
                        // REMOVE_START
                        assertThat(res20.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\",\"Weywot\"]]");
                        // REMOVE_END
                        return asyncCommands.jsonGet("bikes:inventory", JsonPath.of("$..mountain_bikes[*].model"));
                    })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\",\"Weywot\"]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [["Phoebe","Quaoar","Weywot"]]
                    .toCompletableFuture();
            // STEP_END
            getMtnBikes.join();

            // STEP_START get_models
            CompletableFuture<Void> getModels = asyncCommands.jsonGet("bikes:inventory", JsonPath.of("$..model"))
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\",\"Weywot\",\"Salacia\",\"Mimas\"]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [["Phoebe","Quaoar","Weywot","Salacia","Mimas"]]
                    .toCompletableFuture();
            // STEP_END
            getModels.join();

            // STEP_START get2mtnbikes
            CompletableFuture<Void> get2MtnBikes = asyncCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$..mountain_bikes[0:2].model"))
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Phoebe\",\"Quaoar\"]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [["Phoebe","Quaoar"]]
                    .toCompletableFuture();
            // STEP_END
            get2MtnBikes.join();

            // STEP_START filter1
            CompletableFuture<Void> filter1 = asyncCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$..mountain_bikes[?(@.price < 3000 && @.specs.weight < 10)]"))
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[{\"id\":\"bike:2\",\"model\":\"Quaoar\",\"description\":"
                                + "\"Redesigned for the 2020 model year, this bike impressed our "
                                + "testers and is the best all-around trail bike we've ever "
                                + "tested. The Shimano gear system effectively does away with an "
                                + "external cassette, so is super low maintenance in terms of "
                                + "wear and tear. All in all it's an impressive package for the "
                                + "price, making it very competitive.\",\"price\":2072,"
                                + "\"specs\":{\"material\":\"aluminium\",\"weight\":7.9},"
                                + "\"colors\":[\"black\",\"white\"]}]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [[{"id":"bike:2","model":"Quaoar","description":...
                    .toCompletableFuture();
            // STEP_END
            filter1.join();

            // STEP_START filter2
            CompletableFuture<Void> filter2 = asyncCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$..[?(@.specs.material == 'alloy')].model"))
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Weywot\",\"Mimas\"]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [["Weywot","Mimas"]]
                    .toCompletableFuture();
            // STEP_END
            filter2.join();

            // STEP_START filter3
            CompletableFuture<Void> filter3 = asyncCommands
                    .jsonGet("bikes:inventory", JsonPath.of("$..[?(@.specs.material =~ '(?i)al')].model"))
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Quaoar\",\"Weywot\",\"Salacia\",\"Mimas\"]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [["Quaoar","Weywot","Salacia","Mimas"]]
                    .toCompletableFuture();
            // STEP_END
            filter3.join();

            // STEP_START filter4
            CompletableFuture<Void> filter4 = asyncCommands.jsonSet("bikes:inventory",
                    JsonPath.of("$.inventory.mountain_bikes[0].regex_pat"), parser.createJsonValue("\"(?i)al\""))
                    .thenCompose(r -> {
                        System.out.println(r); // >>> OK
                        // REMOVE_START
                        assertThat(r).isEqualTo("OK");
                        // REMOVE_END

                        return asyncCommands.jsonSet("bikes:inventory", JsonPath.of("$.inventory.mountain_bikes[1].regex_pat"),
                                parser.createJsonValue("\"(?i)al\""));
                    }).thenCompose(r -> {
                        System.out.println(r); // >>> OK
                        // REMOVE_START
                        assertThat(r).isEqualTo("OK");
                        // REMOVE_END

                        return asyncCommands.jsonSet("bikes:inventory", JsonPath.of("$.inventory.mountain_bikes[2].regex_pat"),
                                parser.createJsonValue("\"(?i)al\""));
                    }).thenCompose(res22 -> {
                        System.out.println(res22); // >>> OK
                        // REMOVE_START
                        assertThat(res22).isEqualTo("OK");
                        // REMOVE_END

                        return asyncCommands.jsonGet("bikes:inventory",
                                JsonPath.of("$.inventory.mountain_bikes[?(@.specs.material =~ @.regex_pat)].model"));
                    })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[\"Quaoar\",\"Weywot\"]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [["Quaoar","Weywot"]]
                    .toCompletableFuture();
            // STEP_END
            filter4.join();

            // STEP_START update_bikes
            CompletableFuture<Void> updateBikes = asyncCommands.jsonGet("bikes:inventory", JsonPath.of("$..price"))
                    .thenCompose(r -> {
                        System.out.println(r); // >>> [[1920,2072,3264,1475,3941]]
                        // REMOVE_START
                        assertThat(r.toString()).isEqualTo("[[1920,2072,3264,1475,3941]]");
                        // REMOVE_END

                        return asyncCommands.jsonNumincrby("bikes:inventory", JsonPath.of("$..price"), -100);
                    }).thenCompose(res23 -> {
                        System.out.println(res23); // >>> [1820, 1972, 3164, 1375, 3841]
                        // REMOVE_START
                        assertThat(res23.toString()).isEqualTo("[1820, 1972, 3164, 1375, 3841]");
                        // REMOVE_END

                        return asyncCommands.jsonNumincrby("bikes:inventory", JsonPath.of("$..price"), 100);
                    })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[1920, 2072, 3264, 1475, 3941]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [1920, 2072, 3264, 1475, 3941]
                    .toCompletableFuture();
            // STEP_END
            updateBikes.join();

            // STEP_START update_filters1
            CompletableFuture<Void> updateFilters1 = asyncCommands.jsonSet("bikes:inventory",
                    JsonPath.of("$.inventory.*[?(@.price<2000)].price"), parser.createJsonValue("1500")).thenCompose(r -> {
                        System.out.println(r); // >>> OK
                        // REMOVE_START
                        assertThat(r).isEqualTo("OK");
                        // REMOVE_END

                        return asyncCommands.jsonGet("bikes:inventory", JsonPath.of("$..price"));
                    })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[1500,2072,3264,1500,3941]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [[1500,2072,3264,1500,3941]]
                    .toCompletableFuture();
            // STEP_END
            updateFilters1.join();

            // STEP_START update_filters2
            CompletableFuture<Void> updateFilters2 = asyncCommands.jsonArrappend("bikes:inventory",
                    JsonPath.of("$.inventory.*[?(@.price<2000)].colors"), parser.createJsonValue("\"pink\"")).thenCompose(r -> {
                        System.out.println(r); // >>> [3, 3]
                        // REMOVE_START
                        assertThat(r.toString()).isEqualTo("[3, 3]");
                        // REMOVE_END

                        return asyncCommands.jsonGet("bikes:inventory", JsonPath.of("$..[*].colors"));
                    })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[[[\"black\",\"silver\",\"pink\"],[\"black\",\"white\"],"
                                + "[\"black\",\"silver\",\"pink\"]]]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [[["black","silver","pink"],["black","white"],["black","silver","pink"]]]
                    .toCompletableFuture();
            // STEP_END
            updateFilters2.join();

        } finally {
            redisClient.shutdown();
        }
    }

}
