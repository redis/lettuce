// EXAMPLE: list_tutorial
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.api.StatefulRedisConnection;

// REMOVE_START
import org.junit.jupiter.api.Test;
// REMOVE_END

import java.util.*;
import java.util.concurrent.CompletableFuture;

// REMOVE_START
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class ListExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();
            // REMOVE_START
            CompletableFuture<Long> delResult = asyncCommands
                    .del("bikes:repairs", "bikes:finished").toCompletableFuture();
            // REMOVE_END

            // STEP_START queue
            CompletableFuture<Void> queue = asyncCommands.lpush("bikes:repairs", "bike:1")
            .thenCompose(res1 -> {
                System.out.println(res1); // >>> 1
                // REMOVE_START
                assertThat(res1).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.lpush("bikes:repairs", "bike:2");
            }).thenCompose(res2 -> {
                System.out.println(res2); // >>> 2
                // REMOVE_START
                assertThat(res2).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.rpop("bikes:repairs");
            }).thenCompose(res3 -> {
                System.out.println(res3); // >>> bike:1
                // REMOVE_START
                assertThat(res3).isEqualTo("bike:1");
                // REMOVE_END

                return asyncCommands.rpop("bikes:repairs");
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res).isEqualTo("bike:2");
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println) // >>> bike:2
            .toCompletableFuture();
            // STEP_END
            
            // STEP_START stack
            CompletableFuture<Void> stack = queue.thenCompose(r -> {
                return asyncCommands.lpush("bikes:repairs", "bike:1");

            }).thenCompose(res4 -> {
                System.out.println(res4); // >>> 1
                // REMOVE_START
                assertThat(res4).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.lpush("bikes:repairs", "bike:2");
            }).thenCompose(res5 -> {
                System.out.println(res5); // >>> 2
                // REMOVE_START
                assertThat(res5).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.lpop("bikes:repairs");
            }).thenCompose(res6 -> {
                System.out.println(res6); // >>> bike:2
                // REMOVE_START
                assertThat(res6).isEqualTo("bike:2");
                // REMOVE_END

                return asyncCommands.lpop("bikes:repairs");
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res).isEqualTo("bike:1");
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println) // >>> bike:1
            .toCompletableFuture();
            // STEP_END

            // STEP_START llen
            CompletableFuture<Void> llen = stack.thenCompose(r -> {
                return asyncCommands.llen("bikes:repairs");
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res).isZero();
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println) // >>> 0
            .toCompletableFuture();
            // STEP_END

            // STEP_START lmove_lrange
            CompletableFuture<Void> lmovelrange = llen.thenCompose(r -> {
                return asyncCommands.lpush("bikes:repairs", "bike:1");
            }).thenCompose(res7 -> {
                System.out.println(res7); // >>> 1
                // REMOVE_START
                assertThat(res7).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.lpush("bikes:repairs", "bike:2");
            }).thenCompose(res8 -> {
                System.out.println(res8); // >>> 2
                // REMOVE_START
                assertThat(res8).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.lmove(
                    "bikes:repairs",
                    "bikes:finished",
                    LMoveArgs.Builder.leftLeft()
                );
            }).thenCompose(res9 -> {
                System.out.println(res9); // >>> bike:2
                // REMOVE_START
                assertThat(res9).isEqualTo("bike:2");
                // REMOVE_END

                return asyncCommands.lrange("bikes:repairs", 0, -1);
            }).thenCompose(res10 -> {
                System.out.println(res10); // >>> [bike:1]
                // REMOVE_START
                assertThat(res10.toString()).isEqualTo("[bike:1]");
                // REMOVE_END

                return asyncCommands.lrange("bikes:finished", 0, -1);
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res.toString()).isEqualTo("[bike:2]");
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println) // >>> [bike:2]
            .toCompletableFuture();
            // STEP_END
            
            // STEP_START lpush_rpush
            CompletableFuture<Void> lpushrpush = lmovelrange
            // REMOVE_START
            .thenCompose(r -> {
                return asyncCommands.del("bikes:repairs");
            })
            // REMOVE_END
            .thenCompose(r -> {
                return asyncCommands.rpush("bikes:repairs", "bike:1");
            }).thenCompose(res11 -> {
                System.out.println(res11); // >>> 1
                // REMOVE_START
                assertThat(res11).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.rpush("bikes:repairs", "bike:2");
            }).thenCompose(res12 -> {
                System.out.println(res12); // >>> 2
                // REMOVE_START
                assertThat(res12).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.lpush("bikes:repairs", "bike:important_bike");
            }).thenCompose(res13 -> {
                System.out.println(res13); // >>> 3
                // REMOVE_START
                assertThat(res13).isEqualTo(3);
                // REMOVE_END

                return asyncCommands.lrange("bikes:repairs", 0, -1);
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res.toString()).isEqualTo("[bike:important_bike, bike:1, bike:2]");
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println)
            // >>> [bike:important_bike, bike:1, bike:2]
            .toCompletableFuture();
            // STEP_END
            
            // STEP_START variadic
            CompletableFuture<Void> variadic = lpushrpush
            // REMOVE_START
            .thenCompose(r -> {
                return asyncCommands.del("bikes:repairs");
            })
            // REMOVE_END
            .thenCompose(r -> {
                return asyncCommands.rpush("bikes:repairs", "bike:1", "bike:2", "bike:3");
            }).thenCompose(res14 -> {
                System.out.println(res14); // >>> 3
                // REMOVE_START
                assertThat(res14).isEqualTo(3);
                // REMOVE_END

                return asyncCommands.lpush(
                    "bikes:repairs", "bike:important_bike", "bike:very_important_bike"
                );
            }).thenCompose(res15 -> {
                System.out.println(res15); // >>> 5
                // REMOVE_START
                assertThat(res15).isEqualTo(5);
                // REMOVE_END

                return asyncCommands.lrange("bikes:repairs", 0, -1);
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res.toString()).isEqualTo("[bike:very_important_bike, bike:important_bike, bike:1, bike:2, bike:3]");
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println)
            // >>> [bike:very_important_bike, bike:important_bike, bike:1, bike:2, bike:3]
            .toCompletableFuture();
            // STEP_END
            
            // STEP_START lpop_rpop
            CompletableFuture<Void> lpoprpop = variadic
            // REMOVE_START
            .thenCompose(r -> {
                return asyncCommands.del("bikes:repairs");
            })
            // REMOVE_END
            .thenCompose(r -> {
                return asyncCommands.rpush("bikes:repairs", "bike:1", "bike:2", "bike:3");
            }).thenCompose(res16 -> {
                System.out.println(res16); // >>> 3
                // REMOVE_START
                assertThat(res16).isEqualTo(3);
                // REMOVE_END

                return asyncCommands.rpop("bikes:repairs");
            }).thenCompose(res17 -> {
                System.out.println(res17); // >>> bike:3
                // REMOVE_START
                assertThat(res17).isEqualTo("bike:3");
                // REMOVE_END

                return asyncCommands.lpop("bikes:repairs");
            }).thenCompose(res18 -> {
                System.out.println(res18); // >>> bike:1
                // REMOVE_START
                assertThat(res18).isEqualTo("bike:1");
                // REMOVE_END

                return asyncCommands.rpop("bikes:repairs");
            }).thenCompose(res19 -> {
                System.out.println(res19); // >>> bike:2
                // REMOVE_START
                assertThat(res19).isEqualTo("bike:2");
                // REMOVE_END

                return asyncCommands.rpop(res19);
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res).isNull();
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println) // >>> null
            .toCompletableFuture();
            // STEP_END
            
            // STEP_START ltrim
            CompletableFuture<Void> ltrim = lpoprpop.thenCompose(r -> {
                return asyncCommands.lpush(
                    "bikes:repairs", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5"
                );
            }).thenCompose(res20 -> {
                System.out.println(res20); // >>> 5
                // REMOVE_START
                assertThat(res20).isEqualTo(5);
                // REMOVE_END

                return asyncCommands.ltrim("bikes:repairs", 0, 2);
            }).thenCompose(res21 -> {
                System.out.println(res21); // >>> OK
                // REMOVE_START
                assertThat(res21).isEqualTo("OK");
                // REMOVE_END

                return asyncCommands.lrange("bikes:repairs", 0, -1);
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res.toString()).isEqualTo("[bike:5, bike:4, bike:3]");
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println)
            // >>> [bike:5, bike:4, bike:3]
            .toCompletableFuture();
            // STEP_END
            
            // STEP_START ltrim_end_of_list
            CompletableFuture<Void> ltrimendoflist = ltrim
            // REMOVE_START
            .thenCompose(r -> {
                return asyncCommands.del("bikes:repairs");
            })
            // REMOVE_END
            .thenCompose(r -> {
                return asyncCommands.rpush(
                    "bikes:repairs", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5"
                );
            }).thenCompose(res22 -> {
                System.out.println(res22); // >>> 5
                // REMOVE_START
                assertThat(res22).isEqualTo(5);
                // REMOVE_END

                return asyncCommands.ltrim("bikes:repairs", -3, -1);
            }).thenCompose(res23 -> {
                System.out.println(res23); // >>> OK
                // REMOVE_START
                assertThat(res23).isEqualTo("OK");
                // REMOVE_END

                return asyncCommands.lrange("bikes:repairs", 0, -1);
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res.toString()).isEqualTo("[bike:3, bike:4, bike:5]");
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println)
            // >>> [bike:3, bike:4, bike:5]
            .toCompletableFuture();
            // STEP_END
            
            // STEP_START brpop
            CompletableFuture<Void> brpop = ltrimendoflist
            // REMOVE_START
            .thenCompose(r -> {
                return asyncCommands.del("bikes:repairs");
            })
            // REMOVE_END
            .thenCompose(r -> {
                return asyncCommands.rpush("bikes:repairs", "bike:1", "bike:2");
            }).thenCompose(res24 -> {
                System.out.println(res24); // >>> 2
                // REMOVE_START
                assertThat(res24).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.brpop(1, "bikes:repairs");
            }).thenCompose(res25 -> {
                System.out.println(res25);
                // >>> KeyValue[bikes:repairs, bike:2]
                // REMOVE_START
                assertThat(res25.toString()).isEqualTo("KeyValue[bikes:repairs, bike:2]");
                // REMOVE_END

                return asyncCommands.brpop(1, "bikes:repairs");
            }).thenCompose(res26 -> {
                System.out.println(res26);
                // >>> KeyValue[bikes:repairs, bike:1]
                // REMOVE_START
                assertThat(res26.toString()).isEqualTo("KeyValue[bikes:repairs, bike:1]");
                // REMOVE_END

                return asyncCommands.brpop(1, "bikes:repairs");
            })
            // REMOVE_START
            .thenApply(res -> {
                assertThat(res).isNull();
                return res;
            })
            // REMOVE_END
            .thenAccept(System.out::println) // >>> null
            .toCompletableFuture();
            // STEP_END
            
            // HIDE_START
            CompletableFuture.allOf(
                    // REMOVE_START
                    delResult,
                    // REMOVE_END,
                    brpop
            ).join();
        } finally {
            redisClient.shutdown();
        }
    }
}
// HIDE_END
