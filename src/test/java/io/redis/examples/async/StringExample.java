// EXAMPLE: set_tutorial
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.StatefulRedisConnection;

// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StringExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();

            // STEP_START set_get
            CompletableFuture<Void> setAndGet = asyncCommands.set("bike:1", "Deimos").thenCompose(v -> {
                System.out.println(v); // >>> OK
                // REMOVE_START
                assertThat(v).isEqualTo("OK");
                // REMOVE_END
                return asyncCommands.get("bike:1");
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo("Deimos");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> Deimos
                    .toCompletableFuture();
            // STEP_END
            // HIDE_START
            setAndGet.join();
            // HIDE_END

            // STEP_START setnx_xx
            CompletableFuture<Void> setnx = asyncCommands.setnx("bike:1", "bike").thenCompose(v -> {
                System.out.println(v); // >>> false (because key already exists)
                // REMOVE_START
                assertThat(v).isFalse();
                // REMOVE_END
                return asyncCommands.get("bike:1");
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo("Deimos");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> Deimos (value is unchanged)
                    .toCompletableFuture();
            // HIDE_START
            setnx.join();
            // HIDE_END

            // set the value to "bike" if it already exists
            CompletableFuture<Void> setxx = asyncCommands.set("bike:1", "bike", SetArgs.Builder.xx())
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo("OK");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> OK
                    .toCompletableFuture();
            // HIDE_START
            setxx.join();
            // HIDE_END
            // STEP_END

            // STEP_START mset
            Map<String, String> bikeMap = new HashMap<>();
            bikeMap.put("bike:1", "Deimos");
            bikeMap.put("bike:2", "Ares");
            bikeMap.put("bike:3", "Vanth");

            CompletableFuture<Void> mset = asyncCommands.mset(bikeMap).thenCompose(v -> {
                System.out.println(v); // >>> OK
                return asyncCommands.mget("bike:1", "bike:2", "bike:3");
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        List<KeyValue<String, String>> expected = new ArrayList<>(
                                Arrays.asList(KeyValue.just("bike:1", "Deimos"), KeyValue.just("bike:2", "Ares"),
                                        KeyValue.just("bike:3", "Vanth")));
                        assertThat(res).isEqualTo(expected);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [KeyValue[bike:1, Deimos], KeyValue[bike:2, Ares], KeyValue[bike:3,
                    // Vanth]]
                    .toCompletableFuture();
            // STEP_END
            // HIDE_START
            mset.join();
            // HIDE_END

            // STEP_START incr
            CompletableFuture<Void> incrby = asyncCommands.set("total_crashes", "0")
                    .thenCompose(v -> asyncCommands.incr("total_crashes")).thenCompose(v -> {
                        System.out.println(v); // >>> 1
                        // REMOVE_START
                        assertThat(v).isEqualTo(1L);
                        // REMOVE_END
                        return asyncCommands.incrby("total_crashes", 10);
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo(11L);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> 11
                    .toCompletableFuture();
            // STEP_END
            // HIDE_START
            incrby.join();
            // HIDE_END
        } finally {
            redisClient.shutdown();
        }
    }

}
