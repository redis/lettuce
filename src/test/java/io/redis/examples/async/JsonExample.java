// EXAMPLE: json_tutorial
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonArray;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.json.JsonValue;
import io.lettuce.core.json.arguments.JsonRangeArgs;

// REMOVE_START
import org.junit.jupiter.api.Test;
// REMOVE_END
import java.util.concurrent.CompletableFuture;
import java.util.List;
// REMOVE_START
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END
import static org.junit.Assert.assertThat;

public class JsonExample {

    @Test
    public void  run() {
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
                        System.out.println(res2); // >>>
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
                    .thenAccept(System.out::println).toCompletableFuture();
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
                    .thenAccept(System.out::println).toCompletableFuture();
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
                        System.out.println(res6);
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
                    .thenAccept(System.out::println).toCompletableFuture();
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

            CompletableFuture<Void> arr = asyncCommands.jsonSet(
                "newbike", JsonPath.ROOT_PATH,
                bikeDetails)
            .thenCompose(r -> {
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
                System.out.println(res9); // >>> [0]
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
            // >>> [[\"Deimos\",{\"crashes\":0}]]
            .toCompletableFuture();
            // STEP_END
            arr.join();

            /*
            // STEP_START arr2
            CompletableFuture<Void> arr2 = asyncCommands.jsonSet("riders", JsonPath.ROOT_PATH, parser.createJsonArray())
            .thenCompose(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END

                return asyncCommands.jsonArrappend("riders", JsonPath.ROOT_PATH, parser.createJsonValue("\"Norem\""));
            }).thenCompose(res11 -> {
                System.out.println(res11); // >>> [4]
                // REMOVE_START
                assertThat(res11.toString()).isEqualTo("[4]");
                // REMOVE_END

                return asyncCommands.jsonGet("riders", JsonPath.ROOT_PATH);
            })
            
            .thenCompose(res12 -> {
                System.out.println(res12); // >>> ["Norem"]
                // REMOVE_START
                assertThat(res12.toString()).isEqualTo("[[\"Norem\"]]");
                // REMOVE_END

                return asyncCommands.jsonArrinsert("riders", JsonPath.ROOT_PATH, 1, parser.createJsonValue("Prickett"), parser.createJsonValue("Royce"), parser.createJsonValue("Castilla"));
            })
            .thenCompose(res13 -> {
                System.out.println(res13); // >>> [4]
                // REMOVE_START
                assertThat(res13.toString()).isEqualTo("[4]");
                // REMOVE_END

                return asyncCommands.jsonGet("riders", JsonPath.ROOT_PATH);
            })
            .thenCompose(res14 -> {
                System.out.println(res14); // >>> ["Norem","Prickett","Royce","Castilla"]
                //
                return asyncCommands.jsonArrtrim("riders", JsonPath.ROOT_PATH, new JsonRangeArgs().start(1).stop(1));
            })
            .thenCompose(res15 -> {
                System.out.println(res15); // >>> [1]
                // REMOVE_START
                assertThat(res15.toString()).isEqualTo("[1]");
                // REMOVE_END

                return asyncCommands.jsonGet("riders", JsonPath.ROOT_PATH);
            })
            .thenCompose(res16 -> {
                System.out.println(res16); // >>> ["Prickett"]
                // REMOVE_START
                assertThat(res16.toString()).isEqualTo("[[\"Prickett\"]]");
                // REMOVE_END
                return asyncCommands.jsonArrpop("riders", JsonPath.ROOT_PATH, 0);
            })
            .thenCompose(res17 -> {
                System.out.println(res17); // >>> "Prickett"
                // REMOVE_START
                assertThat(res17).isEqualTo("Prickett");
                // REMOVE_END
                return asyncCommands.jsonArrpop("riders", JsonPath.ROOT_PATH);
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res).isEqualTo(null);
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println)
            // >>> null
            .toCompletableFuture();
            // STEP_END
            arr2.join();
            */

            // STEP_START obj
            JsonObject bikeObj = parser.createJsonObject()
                .put("model", parser.createJsonValue("\"Deimos\""))
                .put("brand", parser.createJsonValue("\"Ergonom\""))
                .put("price", parser.createJsonValue("\"4972\""));
                
            CompletableFuture<Void> obj = asyncCommands.jsonSet("bike:1", JsonPath.ROOT_PATH, bikeObj)
            .thenCompose(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END

                return asyncCommands.jsonObjlen("bike:1", JsonPath.ROOT_PATH);
            })
            .thenCompose(res18 -> {
                System.out.println(res18); // >>> 3
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
        } finally {
            redisClient.shutdown();
        }

    }

}
