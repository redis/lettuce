// EXAMPLE: set_tutorial
package io.redis.examples.reactive;

import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.StatefulRedisConnection;
// REMOVE_START
import org.junit.Test;
// REMOVE_END
import reactor.core.publisher.Mono;

import java.util.*;

// REMOVE_START
import static org.junit.Assert.assertEquals;
// REMOVE_END

public class StringExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();

            // STEP_START set_get
            Mono<Void> setAndGet = reactiveCommands.set("bike:1", "Deimos").doOnNext(v -> {
                System.out.println(v); // OK
                // REMOVE_START
                assertEquals("OK", v);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.get("bike:1")).doOnNext(res -> {
                // REMOVE_START
                assertEquals("Deimos", res);
                // REMOVE_END
                System.out.println(res); // Deimos
            }).then();
            // STEP_END

            // STEP_START setnx_xx
            Mono<Void> setnx = reactiveCommands.setnx("bike:1", "bike").doOnNext(v -> {
                System.out.println(v); // false (because key already exists)
                // REMOVE_START
                assertEquals(false, v);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.get("bike:1")).doOnNext(res -> {
                // REMOVE_START
                assertEquals("Deimos", res);
                // REMOVE_END
                System.out.println(res); // Deimos (value is unchanged)
            }).then();

            Mono<Void> setxx = reactiveCommands.set("bike:1", "bike", SetArgs.Builder.xx()).doOnNext(res -> {
                // REMOVE_START
                assertEquals("OK", res);
                // REMOVE_END
                System.out.println(res); // OK
            }).then();
            // STEP_END

            // STEP_START mset
            Map<String, String> bikeMap = new HashMap<>();
            bikeMap.put("bike:1", "Deimos");
            bikeMap.put("bike:2", "Ares");
            bikeMap.put("bike:3", "Vanth");

            Mono<Void> mset = reactiveCommands.mset(bikeMap).doOnNext(System.out::println) // OK
                    .flatMap(v -> reactiveCommands.mget("bike:1", "bike:2", "bike:3").collectList()).doOnNext(res -> {
                        List<KeyValue<String, String>> expected = new ArrayList<>(
                                Arrays.asList(KeyValue.just("bike:1", "Deimos"), KeyValue.just("bike:2", "Ares"),
                                        KeyValue.just("bike:3", "Vanth")));
                        // REMOVE_START
                        assertEquals(expected, res);
                        // REMOVE_END
                        System.out.println(res); // [KeyValue[bike:1, Deimos], KeyValue[bike:2, Ares], KeyValue[bike:3, Vanth]]
                    }).then();
            // STEP_END

            // STEP_START incr
            Mono<Void> incrby = reactiveCommands.set("total_crashes", "0").flatMap(v -> reactiveCommands.incr("total_crashes"))
                    .doOnNext(v -> {
                        System.out.println(v); // 1
                        // REMOVE_START
                        assertEquals(1L, v.longValue());
                        // REMOVE_END
                    }).flatMap(v -> reactiveCommands.incrby("total_crashes", 10)).doOnNext(res -> {
                        // REMOVE_START
                        assertEquals(11L, res.longValue());
                        // REMOVE_END
                        System.out.println(res); // 11
                    }).then();
            // STEP_END

            Mono.when(setAndGet, setnx, setxx, mset, incrby).block();

        } finally {
            redisClient.shutdown();
        }
    }

}
