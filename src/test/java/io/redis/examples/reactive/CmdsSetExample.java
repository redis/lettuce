// EXAMPLE: cmds_set
package io.redis.examples.reactive;

import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.StatefulRedisConnection;

import reactor.core.publisher.Mono;
// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class CmdsSetExample {

    @Test
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();
            // REMOVE_START
            // Clean up any existing data
            Mono<Void> cleanup = reactiveCommands
                    .del("myset").then();
            cleanup.block();
            // REMOVE_END

            // STEP_START sadd
            Mono<Void> sadd = reactiveCommands.sadd("myset", "Hello").doOnNext(r -> {
                System.out.println(r); // >>> 1
                // REMOVE_START
                assertThat(r).isEqualTo(1);
                // REMOVE_END
            }).flatMap(r -> reactiveCommands.sadd("myset", "World")).doOnNext(r -> {
                System.out.println(r); // >>> 1
                // REMOVE_START
                assertThat(r).isEqualTo(1);
                // REMOVE_END
            }).flatMap(r -> reactiveCommands.sadd("myset", "World")).doOnNext(r -> {
                System.out.println(r); // >>> 0
                // REMOVE_START
                assertThat(r).isEqualTo(0);
                // REMOVE_END
            }).flatMap(r -> reactiveCommands.smembers("myset").collectList()).doOnNext(r -> {
                System.out.println(r); // >>> [Hello, World]
                // REMOVE_START
                assertThat(r).containsExactlyInAnyOrder("Hello", "World");
                // REMOVE_END
            }).then();
            // STEP_END
            // HIDE_START
            sadd.block();
            // HIDE_END
            // REMOVE_START
            Mono<Long> delSaddResult = reactiveCommands.del("myset");
            delSaddResult.block();
            // REMOVE_END

            // STEP_START smembers
            Mono<Void> smembers = reactiveCommands.sadd("myset", "Hello", "World").doOnNext(r -> {
                System.out.println(r); // >>> 2
                // REMOVE_START
                assertThat(r).isEqualTo(2);
                // REMOVE_END
            }).flatMap(r -> reactiveCommands.smembers("myset").collectList()).doOnNext(r -> {
                System.out.println(r); // >>> [Hello, World]
                // REMOVE_START
                assertThat(r).containsExactlyInAnyOrder("Hello", "World");
                // REMOVE_END
            }).then();
            // STEP_END
            // HIDE_START
            smembers.block();
            // HIDE_END
        }
        finally {
            redisClient.shutdown();
        }
    }
}