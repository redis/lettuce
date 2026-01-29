// EXAMPLE: cmds_list
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.concurrent.CompletableFuture;
// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class CmdsListExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();
            // REMOVE_START
            asyncCommands.del("mylist").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START lpush
            CompletableFuture<Void> lpush = asyncCommands.lpush("mylist", "world").thenCompose(res1 -> {
                System.out.println(res1); // >>> 1
                // REMOVE_START
                assertThat(res1).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.lpush("mylist", "hello");
            }).thenCompose(res2 -> {
                System.out.println(res2); // >>> 2
                // REMOVE_START
                assertThat(res2).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.lrange("mylist", 0, -1);
            })
                    // REMOVE_START
                    .thenApply(res3 -> {
                        assertThat(res3.toString()).isEqualTo("[hello, world]");
                        return res3;
                    })
                    // REMOVE_END
                    .thenAccept(res3 -> System.out.println(res3)) // >>> [hello, world]
                    .toCompletableFuture();
            // STEP_END
            lpush.join();

            // REMOVE_START
            asyncCommands.del("mylist").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START lrange
            CompletableFuture<Void> lrange = asyncCommands.rpush("mylist", "one").thenCompose(res4 -> {
                System.out.println(res4); // >>> 1
                // REMOVE_START
                assertThat(res4).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.rpush("mylist", "two");
            }).thenCompose(res5 -> {
                System.out.println(res5); // >>> 2
                // REMOVE_START
                assertThat(res5).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.rpush("mylist", "three");
            }).thenCompose(res6 -> {
                System.out.println(res6); // >>> 3
                // REMOVE_START
                assertThat(res6).isEqualTo(3);
                // REMOVE_END

                return asyncCommands.lrange("mylist", 0, 0);
            }).thenCompose(res7 -> {
                System.out.println(res7); // >>> [one]
                // REMOVE_START
                assertThat(res7.toString()).isEqualTo("[one]");
                // REMOVE_END

                return asyncCommands.lrange("mylist", -3, 2);
            }).thenCompose(res8 -> {
                System.out.println(res8); // >>> [one, two, three]
                // REMOVE_START
                assertThat(res8.toString()).isEqualTo("[one, two, three]");
                // REMOVE_END

                return asyncCommands.lrange("mylist", -100, 100);
            }).thenCompose(res9 -> {
                System.out.println(res9); // >>> [one, two, three]
                // REMOVE_START
                assertThat(res9.toString()).isEqualTo("[one, two, three]");
                // REMOVE_END

                return asyncCommands.lrange("mylist", 5, 10);
            })
                    // REMOVE_START
                    .thenApply(res10 -> {
                        assertThat(res10.toString()).isEqualTo("[]");
                        return res10;
                    })
                    // REMOVE_END
                    .thenAccept(res10 -> System.out.println(res10)) // >>> []
                    .toCompletableFuture();
            // STEP_END
            lrange.join();

            // REMOVE_START
            asyncCommands.del("mylist").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START llen
            CompletableFuture<Void> llen = asyncCommands.lpush("mylist", "World").thenCompose(res11 -> {
                System.out.println(res11); // >>> 1
                // REMOVE_START
                assertThat(res11).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.lpush("mylist", "Hello");
            }).thenCompose(res12 -> {
                System.out.println(res12); // >>> 2
                // REMOVE_START
                assertThat(res12).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.llen("mylist");
            })
                    // REMOVE_START
                    .thenApply(res13 -> {
                        assertThat(res13).isEqualTo(2);
                        return res13;
                    })
                    // REMOVE_END
                    .thenAccept(res13 -> System.out.println(res13)) // >>> 2
                    .toCompletableFuture();
            // STEP_END
            llen.join();

            // REMOVE_START
            asyncCommands.del("mylist").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START rpush
            CompletableFuture<Void> rpush = asyncCommands.rpush("mylist", "hello").thenCompose(res14 -> {
                System.out.println(res14); // >>> 1
                // REMOVE_START
                assertThat(res14).isEqualTo(1);
                // REMOVE_END

                return asyncCommands.rpush("mylist", "world");
            }).thenCompose(res15 -> {
                System.out.println(res15); // >>> 2
                // REMOVE_START
                assertThat(res15).isEqualTo(2);
                // REMOVE_END

                return asyncCommands.lrange("mylist", 0, -1);
            })
                    // REMOVE_START
                    .thenApply(res16 -> {
                        assertThat(res16.toString()).isEqualTo("[hello, world]");
                        return res16;
                    })
                    // REMOVE_END
                    .thenAccept(res16 -> System.out.println(res16)) // >>> [hello, world]
                    .toCompletableFuture();
            // STEP_END
            rpush.join();

            // REMOVE_START
            asyncCommands.del("mylist").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START lpop
            CompletableFuture<Void> lpop = asyncCommands.rpush("mylist", "one", "two", "three", "four", "five")
                    .thenCompose(res17 -> {
                        System.out.println(res17); // >>> 5
                        // REMOVE_START
                        assertThat(res17).isEqualTo(5);
                        // REMOVE_END

                        return asyncCommands.lpop("mylist");
                    }).thenCompose(res18 -> {
                        System.out.println(res18); // >>> one
                        // REMOVE_START
                        assertThat(res18).isEqualTo("one");
                        // REMOVE_END

                        return asyncCommands.lpop("mylist", 2);
                    }).thenCompose(res19 -> {
                        System.out.println(res19); // >>> [two, three]
                        // REMOVE_START
                        assertThat(res19.toString()).isEqualTo("[two, three]");
                        // REMOVE_END

                        return asyncCommands.lrange("mylist", 0, -1);
                    })
                    // REMOVE_START
                    .thenApply(res17_final -> {
                        assertThat(res17_final.toString()).isEqualTo("[four, five]");
                        return res17_final;
                    })
                    // REMOVE_END
                    .thenAccept(res17_final -> System.out.println(res17_final)) // >>> [four, five]
                    .toCompletableFuture();
            // STEP_END
            lpop.join();

            // REMOVE_START
            asyncCommands.del("mylist").toCompletableFuture().join();
            // REMOVE_END

            // STEP_START rpop
            CompletableFuture<Void> rpop = asyncCommands.rpush("mylist", "one", "two", "three", "four", "five")
                    .thenCompose(res18 -> {
                        System.out.println(res18); // >>> 5
                        // REMOVE_START
                        assertThat(res18).isEqualTo(5);
                        // REMOVE_END

                        return asyncCommands.rpop("mylist");
                    }).thenCompose(res19 -> {
                        System.out.println(res19); // >>> five
                        // REMOVE_START
                        assertThat(res19).isEqualTo("five");
                        // REMOVE_END

                        return asyncCommands.rpop("mylist", 2);
                    }).thenCompose(res20 -> {
                        System.out.println(res20); // >>> [four, three]
                        // REMOVE_START
                        assertThat(res20.toString()).isEqualTo("[four, three]");
                        // REMOVE_END

                        return asyncCommands.lrange("mylist", 0, -1);
                    })
                    // REMOVE_START
                    .thenApply(res21 -> {
                        assertThat(res21.toString()).isEqualTo("[one, two]");
                        return res21;
                    })
                    // REMOVE_END
                    .thenAccept(res21 -> System.out.println(res21)) // >>> [one, two]
                    .toCompletableFuture();
            // STEP_END
            rpop.join();

            // REMOVE_START
            asyncCommands.del("mylist").toCompletableFuture().join();
            // REMOVE_END

            // HIDE_START
        } finally {
            redisClient.shutdown();
        }
    }

}
// HIDE_END
