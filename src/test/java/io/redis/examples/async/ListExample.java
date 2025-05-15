// EXAMPLE: list_tutorial
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.concurrent.CompletableFuture;
// REMOVE_START
import org.junit.jupiter.api.Test;
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
            asyncCommands.del("bikes:repairs", "bikes:finished", "new_bikes", "new_bikes_string").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START queue
            CompletableFuture<Void> queue = asyncCommands.lpush("bikes:repairs", "bike:1").thenCompose(res1 -> {
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
            queue.join();

            // STEP_START stack
            CompletableFuture<Void> stack = asyncCommands.lpush("bikes:repairs", "bike:1").thenCompose(res4 -> {
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
            stack.join();

            // STEP_START llen
            CompletableFuture<Void> llen = asyncCommands.llen("bikes:repairs")
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isZero();
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> 0
                    .toCompletableFuture();
            // STEP_END
            llen.join();

            // STEP_START lmove_lrange
            CompletableFuture<Void> lmovelrange = asyncCommands.lpush("bikes:repairs", "bike:1").thenCompose(res7 -> {
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

                return asyncCommands.lmove("bikes:repairs", "bikes:finished", LMoveArgs.Builder.leftLeft());
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
            lmovelrange.join();

            // REMOVE_START
            asyncCommands.del("bikes:repairs").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START lpush_rpush
            CompletableFuture<Void> lpushrpush = asyncCommands.rpush("bikes:repairs", "bike:1").thenCompose(res11 -> {
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
            lpushrpush.join();

            // REMOVE_START
            asyncCommands.del("bikes:repairs").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START variadic
            CompletableFuture<Void> variadic = asyncCommands.rpush("bikes:repairs", "bike:1", "bike:2", "bike:3")
                    .thenCompose(res14 -> {
                        System.out.println(res14); // >>> 3
                        // REMOVE_START
                        assertThat(res14).isEqualTo(3);
                        // REMOVE_END

                        return asyncCommands.lpush("bikes:repairs", "bike:important_bike", "bike:very_important_bike");
                    }).thenCompose(res15 -> {
                        System.out.println(res15); // >>> 5
                        // REMOVE_START
                        assertThat(res15).isEqualTo(5);
                        // REMOVE_END

                        return asyncCommands.lrange("bikes:repairs", 0, -1);
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString())
                                .isEqualTo("[bike:very_important_bike, bike:important_bike, bike:1, bike:2, bike:3]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println)
                    // >>> [bike:very_important_bike, bike:important_bike, bike:1, bike:2, bike:3]
                    .toCompletableFuture();
            // STEP_END
            variadic.join();

            // REMOVE_START
            asyncCommands.del("bikes:repairs").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START lpop_rpop
            CompletableFuture<Void> lpoprpop = asyncCommands.rpush("bikes:repairs", "bike:1", "bike:2", "bike:3")
                    .thenCompose(res16 -> {
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

                        return asyncCommands.rpop("bikes:repairs");
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
            lpoprpop.join();

            // STEP_START ltrim
            CompletableFuture<Void> ltrim = asyncCommands
                    .lpush("bikes:repairs", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5").thenCompose(res20 -> {
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
            ltrim.join();

            // REMOVE_START
            asyncCommands.del("bikes:repairs");
            // REMOVE_END

            // STEP_START ltrim_end_of_list
            CompletableFuture<Void> ltrimendoflist = asyncCommands
                    .rpush("bikes:repairs", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5").thenCompose(res22 -> {
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
            ltrimendoflist.join();

            // REMOVE_START
            asyncCommands.del("bikes:repairs");
            // REMOVE_END

            // STEP_START brpop
            CompletableFuture<Void> brpop = asyncCommands.rpush("bikes:repairs", "bike:1", "bike:2").thenCompose(res24 -> {
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
            brpop.join();

            // STEP_START rule_1
            CompletableFuture<Void> rule1 = asyncCommands.del("new_bikes").thenCompose(res27 -> {
                System.out.println(res27); // >>> 0

                return asyncCommands.lpush("new_bikes", "bike:1", "bike:2", "bike:3");
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo(3);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> 3
                    .toCompletableFuture();
            // STEP_END
            rule1.join();

            // STEP_START rule_1.1
            CompletableFuture<Void> rule11 = asyncCommands.set("new_bikes_string", "bike:1").thenCompose(res28 -> {
                System.out.println(res28); // >>> OK
                // REMOVE_START
                assertThat(res28).isEqualTo("OK");
                // REMOVE_END

                return asyncCommands.type("new_bikes_string");
            }).thenCompose(res29 -> {
                System.out.println(res29); // >>> string
                // REMOVE_START
                assertThat(res29).isEqualTo("string");
                // REMOVE_END

                return asyncCommands.lpush("new_bikes_string", "bike:2", "bike:3");
            }).handle((res, ex) -> {
                if (ex == null) {
                    return res;
                } else {
                    System.out.println(ex);
                    // >>> java.util.concurrent.CompletionException:
                    // >>> io.lettuce.core.RedisCommandExecutionException:
                    // >>> WRONGTYPE Operation against a key holding the wrong
                    // >>> kind of value
                    // REMOVE_START
                    assertThat(ex.toString()).isEqualTo(
                            "java.util.concurrent.CompletionException: io.lettuce.core.RedisCommandExecutionException: WRONGTYPE Operation against a key holding the wrong kind of value");
                    // REMOVE_END

                    return -1L;
                }
            })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res).isEqualTo(-1L);
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> -1
                    .toCompletableFuture();
            // STEP_END
            rule11.join();

            // REMOVE_START
            asyncCommands.del("bikes:repairs");
            // REMOVE_END

            // STEP_START rule_2
            CompletableFuture<Void> rule2 = asyncCommands.lpush("bikes:repairs", "bike:1", "bike:2", "bike:3")
                    .thenCompose(res30 -> {
                        System.out.println(res30); // >>> 3
                        // REMOVE_START
                        assertThat(res30).isEqualTo(3);
                        // REMOVE_END

                        return asyncCommands.exists("bikes:repairs");
                    }).thenCompose(res31 -> {
                        System.out.println(res31); // >>> 1
                        // REMOVE_START
                        assertThat(res31).isEqualTo(1);
                        // REMOVE_END

                        return asyncCommands.lpop("bikes:repairs");
                    }).thenCompose(res32 -> {
                        System.out.println(res32); // >>> bike:3
                        // REMOVE_START
                        assertThat(res32).isEqualTo("bike:3");
                        // REMOVE_END

                        return asyncCommands.lpop("bikes:repairs");
                    }).thenCompose(res33 -> {
                        System.out.println(res33); // >>> bike:2
                        // REMOVE_START
                        assertThat(res33).isEqualTo("bike:2");
                        // REMOVE_END

                        return asyncCommands.lpop("bikes:repairs");
                    }).thenCompose(res34 -> {
                        System.out.println(res34); // >>> bike:1
                        // REMOVE_START
                        assertThat(res34).isEqualTo("bike:1");
                        // REMOVE_END

                        return asyncCommands.exists("bikes:repairs");
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
            rule2.join();

            // STEP_START rule_3
            CompletableFuture<Void> rule3 = asyncCommands.del("bikes:repairs").thenCompose(res35 -> {
                System.out.println(res35); // >>> 0

                return asyncCommands.llen("bikes:repairs");
            }).thenCompose(res36 -> {
                System.out.println(res36); // >>> 0
                // REMOVE_START
                assertThat(res36).isZero();
                // REMOVE_END

                return asyncCommands.lpop("bikes:repairs");
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
            rule3.join();

            // STEP_START ltrim.1
            CompletableFuture<Void> ltrim1 = asyncCommands
                    .lpush("bikes:repairs", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5").thenCompose(res37 -> {
                        System.out.println(res37); // >>> 5
                        // REMOVE_START
                        assertThat(res37).isEqualTo(5);
                        // REMOVE_END

                        return asyncCommands.ltrim("bikes:repairs", 0, 2);
                    }).thenCompose(res38 -> {
                        System.out.println(res38); // >>> OK
                        // REMOVE_START
                        assertThat(res38).isEqualTo("OK");
                        // REMOVE_END

                        return asyncCommands.lrange("bikes:repairs", 0, -1);
                    })
                    // REMOVE_START
                    .thenApply(res -> {
                        assertThat(res.toString()).isEqualTo("[bike:5, bike:4, bike:3]");
                        return res;
                    })
                    // REMOVE_END
                    .thenAccept(System.out::println) // >>> [bike:5, bike:4, bike:3]
                    .toCompletableFuture();
            // STEP_END
            ltrim1.join();
            // HIDE_START
        } finally {
            redisClient.shutdown();
        }
    }

}
// HIDE_END
