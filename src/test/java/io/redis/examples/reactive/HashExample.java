// EXAMPLE: hash_tutorial
package io.redis.examples.reactive;

import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.StatefulRedisConnection;
// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

import reactor.core.publisher.Mono;

import java.util.*;

public class HashExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();
            // REMOVE_START
            // Clean up any existing data
            Mono<Void> cleanup = reactiveCommands.del("bike:1", "bike:1:stats").then();
            cleanup.block();
            // REMOVE_END

            // STEP_START set_get_all
            Map<String, String> bike1 = new HashMap<>();
            bike1.put("model", "Deimos");
            bike1.put("brand", "Ergonom");
            bike1.put("type", "Enduro bikes");
            bike1.put("price", "4972");

            Mono<Long> setGetAll = reactiveCommands.hset("bike:1", bike1).doOnNext(result -> {
                System.out.println(result); // >>> 4
                // REMOVE_START
                assertThat(result).isEqualTo(4L);
                // REMOVE_END
            });

            setGetAll.block();

            Mono<String> getModel = reactiveCommands.hget("bike:1", "model").doOnNext(result -> {
                System.out.println(result); // >>> Deimos
                // REMOVE_START
                assertThat(result).isEqualTo("Deimos");
                // REMOVE_END
            });

            Mono<String> getPrice = reactiveCommands.hget("bike:1", "price").doOnNext(result -> {
                System.out.println(result); // >>> 4972
                // REMOVE_START
                assertThat(result).isEqualTo("4972");
                // REMOVE_END
            });

            Mono<List<KeyValue<String, String>>> getAll = reactiveCommands.hgetall("bike:1").collectList().doOnNext(result -> {
                System.out.println(result);
                // >>> [KeyValue[type, Enduro bikes], KeyValue[brand, Ergonom],
                // KeyValue[price, 4972], KeyValue[model, Deimos]]
                // REMOVE_START
                List<KeyValue<String, String>> expected = new ArrayList<>(
                        Arrays.asList(KeyValue.just("price", "4972"), KeyValue.just("model", "Deimos"),
                                KeyValue.just("type", "Enduro bikes"), KeyValue.just("brand", "Ergonom")));
                assertThat(result).isEqualTo(expected);
                // REMOVE_END
            });
            // STEP_END

            // STEP_START hmget
            Mono<List<KeyValue<String, String>>> hmGet = reactiveCommands.hmget("bike:1", "model", "price").collectList()
                    .doOnNext(result -> {
                        System.out.println(result);
                        // >>> [KeyValue[model, Deimos], KeyValue[price, 4972]]
                        // REMOVE_START
                        List<KeyValue<String, String>> expected = new ArrayList<>(
                                Arrays.asList(KeyValue.just("model", "Deimos"), KeyValue.just("price", "4972")));
                        assertThat(result).isEqualTo(expected);
                        // REMOVE_END
                    });
            // STEP_END

            Mono.when(getModel, getPrice, getAll, hmGet).block();

            // STEP_START hincrby
            Mono<Void> hIncrBy = reactiveCommands.hincrby("bike:1", "price", 100).doOnNext(result -> {
                System.out.println(result); // >>> 5072
                // REMOVE_START
                assertThat(result).isEqualTo(5072L);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.hincrby("bike:1", "price", -100)).doOnNext(result -> {
                System.out.println(result); // >>> 4972
                // REMOVE_START
                assertThat(result).isEqualTo(4972L);
                // REMOVE_END
            }).then();
            // STEP_END
            hIncrBy.block();

            // STEP_START incrby_get_mget
            Mono<Void> incrByGetMget = reactiveCommands.hincrby("bike:1:stats", "rides", 1).doOnNext(result -> {
                System.out.println(result); // >>> 1
                // REMOVE_START
                assertThat(result).isEqualTo(1L);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.hincrby("bike:1:stats", "rides", 1)).doOnNext(result -> {
                System.out.println(result); // >>> 2
                // REMOVE_START
                assertThat(result).isEqualTo(2L);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.hincrby("bike:1:stats", "rides", 1)).doOnNext(result -> {
                System.out.println(result); // >>> 3
                // REMOVE_START
                assertThat(result).isEqualTo(3L);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.hincrby("bike:1:stats", "crashes", 1)).doOnNext(result -> {
                System.out.println(result); // >>> 1
                // REMOVE_START
                assertThat(result).isEqualTo(1L);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.hincrby("bike:1:stats", "owners", 1)).doOnNext(result -> {
                System.out.println(result); // >>> 1
                // REMOVE_START
                assertThat(result).isEqualTo(1L);
                // REMOVE_END
            }).then();

            incrByGetMget.block();

            Mono<String> getRides = reactiveCommands.hget("bike:1:stats", "rides").doOnNext(result -> {
                System.out.println(result); // >>> 3
                // REMOVE_START
                assertThat(result).isEqualTo("3");
                // REMOVE_END
            });

            Mono<List<KeyValue<String, String>>> getCrashesOwners = reactiveCommands.hmget("bike:1:stats", "crashes", "owners")
                    .collectList().doOnNext(result -> {
                        System.out.println(result);
                        // >>> [KeyValue[crashes, 1], KeyValue[owners, 1]]
                        // REMOVE_START
                        List<KeyValue<String, String>> expected = new ArrayList<>(
                                Arrays.asList(KeyValue.just("crashes", "1"), KeyValue.just("owners", "1")));

                        assertThat(result).isEqualTo(expected);
                        // REMOVE_END
                    });
            // STEP_END

            Mono.when(getRides, getCrashesOwners).block();
        } finally {
            redisClient.shutdown();
        }
    }

}
