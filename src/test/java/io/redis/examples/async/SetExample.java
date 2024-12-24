// EXAMPLE: sets_tutorial
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.StatefulRedisConnection;

// REMOVE_START
import org.junit.jupiter.api.Test;

// REMOVE_END
import java.util.*;
import static java.util.stream.Collectors.*;
import java.util.concurrent.CompletableFuture;

// REMOVE_START
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class SetExample {

    @Test
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();
            // REMOVE_START
            CompletableFuture<Long> delResult = asyncCommands
                    .del("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy").toCompletableFuture();
            // REMOVE_END

            // STEP_START sadd
            CompletableFuture<Void> sAdd = asyncCommands.sadd("bikes:racing:france", "bike:1").thenCompose(res1 -> {
                System.out.println(res1); // >>> 1

                // REMOVE_START
                assertThat(res1).isEqualTo(1);
                // REMOVE_END
                return asyncCommands.sadd("bikes:racing:france", "bike:1");
            }).thenCompose(res2 -> {
                System.out.println(res2); // >>> 0

                // REMOVE_START
                assertThat(res2).isEqualTo(0);
                // REMOVE_END
                return asyncCommands.sadd("bikes:racing:france", "bike:2", "bike:3");
            }).thenCompose(res3 -> {
                System.out.println(res3); // >>> 2

                // REMOVE_START
                assertThat(res3).isEqualTo(2);
                // REMOVE_END
                return asyncCommands.sadd("bikes:racing:usa", "bike:1", "bike:4");
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo(2);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> 2
                    .toCompletableFuture();
            // STEP_END

            // STEP_START sismember
            CompletableFuture<Void> sIsMember = sAdd.thenCompose(r -> {
                return asyncCommands.sismember("bikes:racing:usa", "bike:1");
            }).thenCompose(res4 -> {
                System.out.println(res4); // >>> true

                // REMOVE_START
                assertThat(res4).isTrue();
                // REMOVE_END
                return asyncCommands.sismember("bikes:racing:usa", "bike:2");
            })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r).isFalse();
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> false
                    .toCompletableFuture();
            // STEP_END

            // STEP_START sinter
            CompletableFuture<Void> sInter = sIsMember.thenCompose(r -> {
                return asyncCommands.sinter("bikes:racing:france", "bikes:racing:usa");
            })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.toString()).isEqualTo("[bike:1]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> ["bike:1"]
                    .toCompletableFuture();
            // STEP_END

            // STEP_START scard
            CompletableFuture<Void> sCard = sInter.thenCompose(r -> {
                return asyncCommands.scard("bikes:racing:france");
            })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r).isEqualTo(3);
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> 3
                    .toCompletableFuture();
            // STEP_END

            // STEP_START sadd_smembers
            CompletableFuture<Void> sAddSMembers = sCard.thenCompose(r -> {
                return asyncCommands.del("bikes:racing:france");
            }).thenCompose(r -> {
                return asyncCommands.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");
            }).thenCompose(res5 -> {
                System.out.println(res5); // >>> 3
                return asyncCommands.smembers("bikes:racing:france");
            })
                    // REMOVE_START
                    .thenApply((Set<String> r) -> {
                        assertThat(r.stream().sorted().collect(toList()).toString()).isEqualTo("[bike:1, bike:2, bike:3]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [bike:1, bike:2, bike:3]
                    .toCompletableFuture();
            // STEP_END

            // STEP_START smismember
            CompletableFuture<Void> sMIsMember = sAddSMembers.thenCompose(r -> {
                return asyncCommands.sismember("bikes:racing:france", "bike:1");
            }).thenCompose(res6 -> {
                System.out.println(res6); // >>> True
                // REMOVE_START
                assertThat(res6).isTrue();
                // REMOVE_END
                return asyncCommands.smismember("bikes:racing:france", "bike:2", "bike:3", "bike:4");
            })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r).containsSequence(true, true, false);
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> [true, true, false]
                    .toCompletableFuture();
            // STEP_END

            // STEP_START sdiff
            CompletableFuture<Void> sDiff = sMIsMember.thenCompose(r -> {
                return asyncCommands.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");
            }).thenCompose(r -> {
                return asyncCommands.sadd("bikes:racing:usa", "bike:1", "bike:4");
            }).thenCompose(r -> {
                return asyncCommands.sdiff("bikes:racing:france", "bikes:racing:usa");
            })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.stream().sorted().collect(toList()).toString()).isEqualTo("[bike:2, bike:3]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> [bike:2, bike:3]
                    .toCompletableFuture();
            // STEP_END

            // STEP_START multisets
            CompletableFuture<Void> multisets = sDiff.thenCompose(r -> {
                return asyncCommands.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3");
            }).thenCompose(r -> {
                return asyncCommands.sadd("bikes:racing:usa", "bike:1", "bike:4");
            }).thenCompose(r -> {
                return asyncCommands.sadd("bikes:racing:italy", "bike:1", "bike:2", "bike:3", "bike:4");
            }).thenCompose(r -> {
                return asyncCommands.sinter("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy");
            }).thenCompose(res7 -> {
                System.out.println(res7); // >>> [bike:1]
                // REMOVE_START
                assertThat(res7.toString()).isEqualTo("[bike:1]");
                // REMOVE_END
                return asyncCommands.sunion("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy");
            }).thenCompose(res8 -> {
                System.out.println(res8);
                // >>> [bike:1, bike:2, bike:3, bike:4]
                // REMOVE_START
                assertThat(res8.stream().sorted().collect(toList()).toString()).isEqualTo("[bike:1, bike:2, bike:3, bike:4]");
                // REMOVE_END
                return asyncCommands.sdiff("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy");
            }).thenCompose(res9 -> {
                System.out.println(res9); // >>> []
                // REMOVE_START
                assertThat(res9.toString()).isEqualTo("[]");
                // REMOVE_END
                return asyncCommands.sdiff("bikes:racing:usa", "bikes:racing:france");
            }).thenCompose(res10 -> {
                System.out.println(res10); // >>> [bike:4]
                // REMOVE_START
                assertThat(res10.toString()).isEqualTo("[bike:4]");
                // REMOVE_END
                return asyncCommands.sdiff("bikes:racing:france", "bikes:racing:usa");
            })
                    // REMOVE_START
                    .thenApply(r -> {
                        assertThat(r.stream().sorted().collect(toList()).toString()).isEqualTo("[bike:2, bike:3]");
                        return r;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> [bike:2, bike:3]
                    .toCompletableFuture();
            // STEP_END

            // STEP_START srem
            CompletableFuture<Void> sRem = multisets.thenCompose(r -> {
                return asyncCommands.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5");
            }).thenCompose(r -> {
                return asyncCommands.srem("bikes:racing:france", "bike:1");
            }).thenCompose(res11 -> {
                System.out.println(res11); // >>> 1
                // REMOVE_START
                assertThat(res11).isEqualTo(1);
                // REMOVE_END
                return asyncCommands.spop("bikes:racing:france");
            }).thenCompose(res12 -> {
                System.out.println(res12); // >>> bike:3 (for example)
                return asyncCommands.smembers("bikes:racing:france");
            }).thenCompose(res13 -> {
                System.out.println(res13); // >>> [bike:2, bike:4, bike:5]
                return asyncCommands.srandmember("bikes:racing:france");
            }).thenAccept(System.out::println) // >>> bike:4
                    .toCompletableFuture();
            // STEP_END

            CompletableFuture.allOf(
                    // REMOVE_START
                    delResult,
                    // REMOVE_END,
                    sRem).join();
        } finally {
            redisClient.shutdown();
        }
    }

}
