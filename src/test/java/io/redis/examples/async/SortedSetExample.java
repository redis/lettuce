// EXAMPLE: ss_tutorial
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.StatefulRedisConnection;

// REMOVE_START
import org.junit.jupiter.api.Test;
// REMOVE_END
import java.util.*;
import java.util.concurrent.CompletableFuture;
// REMOVE_START
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class SortedSetExample {

    @Test
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();
            // REMOVE_START
            asyncCommands.del("racer_scores").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START zadd
            CompletableFuture<Void> zadd = asyncCommands.zadd("racer_scores", ScoredValue.just(10d, "Norem"))
                    .thenCompose(res1 -> {
                        System.out.println(res1); // >>> 1
                        // REMOVE_START
                        assertThat(res1).isEqualTo(1);
                        // REMOVE_END

                        return asyncCommands.zadd("racer_scores", ScoredValue.just(12d, "Castilla"));
                    }).thenCompose(res2 -> {
                        System.out.println(res2); // >>> 1
                        // REMOVE_START
                        assertThat(res2).isEqualTo(1);
                        // REMOVE_END

                        return asyncCommands.zadd("racer_scores", ScoredValue.just(8d, "Sam-Bodden"),
                                ScoredValue.just(10d, "Royce"), ScoredValue.just(6d, "Ford"),
                                ScoredValue.just(14d, "Prickett"));
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo(4);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> 4
                    .toCompletableFuture();
            // STEP_END
            zadd.join();

            // STEP_START zrange
            CompletableFuture<Void> zrange = asyncCommands.zrange("racer_scores", 0, -1).thenCompose(res3 -> {
                System.out.println(res3);
                // >>> [Ford, Sam-Bodden, Norem, Royce, Castilla, Prickett]
                // REMOVE_START
                assertThat(res3.toString()).isEqualTo("[Ford, Sam-Bodden, Norem, Royce, Castilla, Prickett]");
                // REMOVE_END

                return asyncCommands.zrevrange("racer_scores", 0, -1);
            })
                    // REMOVE_START
                    .thenApply((List<String> res) -> {
                        assertThat(res.toString()).isEqualTo("[Prickett, Castilla, Royce, Norem, Sam-Bodden, Ford]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [Prickett, Castilla, Royce, Norem, Sam-Bodden, Ford]
                    .toCompletableFuture();
            // STEP_END
            zrange.join();

            // STEP_START zrange_withscores
            CompletableFuture<Void> zrangeWithScores = asyncCommands.zrangeWithScores("racer_scores", 0, -1)
                    // REMOVE_START
                    .thenApply((List<ScoredValue<String>> res) -> {
                        assertThat(res.toString()).isEqualTo("[ScoredValue[6.000000, Ford], ScoredValue[8.000000, Sam-Bodden],"
                                + " ScoredValue[10.000000, Norem], ScoredValue[10.000000, Royce],"
                                + " ScoredValue[12.000000, Castilla], ScoredValue[14.000000, Prickett]]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [ScoredValue[6.000000, Ford], ScoredValue[8.000000, Sam-Bodden]...
                    .toCompletableFuture();
            // STEP_END
            zrangeWithScores.join();

            // STEP_START zrangebyscore
            CompletableFuture<Void> zrangebyscore = asyncCommands
                    .zrangebyscore("racer_scores", Range.create(Double.MIN_VALUE, 10))
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[Ford, Sam-Bodden, Norem, Royce]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [Ford, Sam-Bodden, Norem, Royce]
                    .toCompletableFuture();
            // STEP_END
            zrangebyscore.join();

            // STEP_START zremrangebyscore
            CompletableFuture<Void> zremrangebyscore = asyncCommands.zrem("racer_scores", "Castilla").thenCompose(res4 -> {
                System.out.println(res4); // >>> 1
                // REMOVE_START
                assertThat(res4).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.zremrangebyscore("racer_scores", Range.create(Double.MIN_VALUE, 9));
            }).thenCompose(res5 -> {
                System.out.println(res5); // >>> 2
                // REMOVE_START
                assertThat(res5).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.zrange("racer_scores", 0, -1);
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[Norem, Royce, Prickett]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [Norem, Royce, Prickett]
                    .toCompletableFuture();
            // STEP_END
            zremrangebyscore.join();

            // STEP_START zrank
            CompletableFuture<Void> zrank = asyncCommands.zrank("racer_scores", "Norem").thenCompose(res6 -> {
                System.out.println(res6); // >>> 0
                // REMOVE_START
                assertThat(res6).isZero();
                // REMOVE_END

                return asyncCommands.zrevrank("racer_scores", "Norem");
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo(2);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> 2
                    .toCompletableFuture();
            // STEP_END
            zrank.join();

            // STEP_START zadd_lex
            CompletableFuture<Void> zaddLex = asyncCommands.zadd("racer_scores", ScoredValue.just(0d, "Norem"),
                    ScoredValue.just(0d, "Sam-Bodden"), ScoredValue.just(0d, "Royce"), ScoredValue.just(0d, "Castilla"),
                    ScoredValue.just(0d, "Prickett"), ScoredValue.just(0d, "Ford")).thenCompose(res7 -> {
                        System.out.println(res7); // >>> 3
                        // REMOVE_START
                        assertThat(res7).isEqualTo(3);
                        // REMOVE_END

                        return asyncCommands.zrange("racer_scores", 0, -1);
                    }).thenCompose(res8 -> {
                        System.out.println(res8);
                        // >>> [Castilla, Ford, Norem, Prickett, Royce, Sam-Bodden]
                        // REMOVE_START
                        assertThat(res8.toString()).isEqualTo("[Castilla, Ford, Norem, Prickett, Royce, Sam-Bodden]");
                        // REMOVE_END

                        return asyncCommands.zrangebylex("racer_scores", Range.create("A", "L"));
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[Castilla, Ford]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [Castilla, Ford]
                    .toCompletableFuture();
            // STEP_END
            zaddLex.join();

            // STEP_START leaderboard
            CompletableFuture<Void> leaderboard = asyncCommands.zadd("racer_scores", ScoredValue.just(100, "Wood"))
                    .thenCompose(res9 -> {
                        System.out.println(res9); // >>> 1
                        // REMOVE_START
                        assertThat(res9).isEqualTo(1);
                        // REMOVE_END

                        return asyncCommands.zadd("racer_scores", ScoredValue.just(100, "Henshaw"));
                    }).thenCompose(res10 -> {
                        System.out.println(res10); // >>> 1
                        // REMOVE_START
                        assertThat(res10).isEqualTo(1);
                        // REMOVE_END

                        return asyncCommands.zadd("racer_scores", ScoredValue.just(150, "Henshaw"));
                    }).thenCompose(res11 -> {
                        System.out.println(res11); // >>> 0
                        // REMOVE_START
                        assertThat(res11).isZero();
                        // REMOVE_END

                        return asyncCommands.zincrby("racer_scores", 50, "Wood");
                    }).thenCompose(res12 -> {
                        System.out.println(res12); // >>> 150
                        // REMOVE_START
                        assertThat(res12).isEqualTo(150);
                        // REMOVE_END

                        return asyncCommands.zincrby("racer_scores", 50, "Henshaw");
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo(200);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> 200
                    .toCompletableFuture();
            // STEP_END
            leaderboard.join();
            // HIDE_START
        } finally {
            redisClient.shutdown();
        }
    }

}
// HIDE_END
