// EXAMPLE: cmds_set
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.StatefulRedisConnection;

// REMOVE_START
import org.junit.jupiter.api.Test;
// REMOVE_END
import java.util.concurrent.CompletableFuture;
import static java.util.stream.Collectors.toList;
// REMOVE_START
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class CmdsSetExample {

    @Test
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();
            // REMOVE_START
            CompletableFuture<Long> delResult = asyncCommands.del("myset").toCompletableFuture();
            delResult.join();
            // REMOVE_END

            // STEP_START sadd
            CompletableFuture<Void> sadd = asyncCommands.sadd("myset", "Hello").thenCompose(r -> {
                System.out.println(r); // >>> 1
                // REMOVE_START
                assertThat(r).isEqualTo(1);
                // REMOVE_END
                return asyncCommands.sadd("myset", "World");
            }).thenCompose(r -> {
                System.out.println(r); // >>> 1
                // REMOVE_START
                assertThat(r).isEqualTo(1);
                // REMOVE_END
                return asyncCommands.sadd("myset", "World");
            }).thenCompose(r -> {
                System.out.println(r); // >>> 0
                // REMOVE_START
                assertThat(r).isEqualTo(0);
                // REMOVE_END
                return asyncCommands.smembers("myset");
            })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.stream().sorted().collect(toList()).toString()).isEqualTo("[Hello, World]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [Hello, World]
                    .toCompletableFuture();
            // STEP_END
            // HIDE_START
            sadd.join();
            // HIDE_END
            // REMOVE_START
            CompletableFuture<Long> delSaddResult = asyncCommands.del("myset").toCompletableFuture();
            delSaddResult.join();
            // REMOVE_END

            // STEP_START smembers
            CompletableFuture<Void> smembers = asyncCommands.sadd("myset", "Hello", "World").thenCompose(r -> {
                return asyncCommands.smembers("myset");
            })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.stream().sorted().collect(toList()).toString()).isEqualTo("[Hello, World]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [Hello, World]
                    .toCompletableFuture();
            // STEP_END
            // HIDE_START
            smembers.join();
            // HIDE_END
        }
    }
}
