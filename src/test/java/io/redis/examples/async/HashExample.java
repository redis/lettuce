// EXAMPLE: hash_tutorial
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.StatefulRedisConnection;

// REMOVE_START
import org.junit.jupiter.api.Test;
// REMOVE_END
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.s;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// REMOVE_START
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class HashExample {

    @Test
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();
            // REMOVE_START
            CompletableFuture<Long> delResult = asyncCommands.del("bike:1")
                    .toCompletableFuture();

            // REMOVE_END

            //STEP_START set_get_all
            Map<String, String> bike1 = new HashMap<>();
            bike1.put("model", "Deimos");
            bike1.put("brand", "Ergonom");
            bike1.put("type", "Enduro bikes");
            bike1.put("price", "4972");

            CompletableFuture<Void> setGetAll = asyncCommands.hset(
                "bike:1", bike1
            )
                    .thenCompose(res1 -> {
                        System.out.println(res1); // >>> 4
                        // REMOVE_START
                        assertThat(res1).isEqualTo(4);
                        // REMOVE_END
                        return asyncCommands.hget("bike:1", "model");
                    })
                    .thenCompose(res2 -> {
                        System.out.println(res2); // >>> Deimos
                        // REMOVE_START
                        assertThat(res2).isEqualTo("Deimos");
                        // REMOVE_END
                        return asyncCommands.hget("bike:1", "price");
                    })
                    .thenCompose(res3 -> {
                        System.out.println(res3); // >>> 4972
                        // REMOVE_START
                        assertThat(res3).isEqualTo("4972");
                        // REMOVE_END
                        return asyncCommands.hgetall("bike:1");
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.get("type")).isEqualTo("Enduro bikes");
                        assertThat(res.get("brand")).isEqualTo("Ergonom");
                        assertThat(res.get("price")).isEqualTo("4972");
                        assertThat(res.get("model")).isEqualTo("Deimos");
                    
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> {type=Enduro bikes, brand=Ergonom, price=4972, model=Deimos}
                    .toCompletableFuture();
            //STEP_END

            // STEP_START hmget
            CompletableFuture<Void> hmGet = setGetAll
                    .thenApply(res4 -> {
                        return asyncCommands.hmget(
                            "bike:1","model", "price"
                        );
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[KeyValue[model, Deimos], KeyValue[price, 4972]]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // [KeyValue[model, Deimos], KeyValue[price, 4972]]
                    .toCompletableFuture();
            // STEP_END

            // STEP_START hincrby
            CompletableFuture<Void> hIncrBy = hmGet
                    .thenCompose(res5 -> {
                        return asyncCommands.hincrby(
                            "bike:1", "price", 100
                        );
                    })
                    .thenCompose(res6 -> {
                        System.out.println(res6); // >>> 5072
                        // REMOVE_START
                        assertThat(res6).isEqualTo(5072);
                        // REMOVE_END
                        return asyncCommands.hincrby("bike:1", "price", -100);
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo(4972);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> 4972
                    .toCompletableFuture();
            // STEP_END
            
            CompletableFuture.allOf(
                // REMOVE_START
                delResult,
                // REMOVE_END
                hmGet
            );
        } finally {
            redisClient.shutdown();
        }
    }
}