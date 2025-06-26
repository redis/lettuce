// EXAMPLE: sets_tutorial
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

public class SetExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();
            // REMOVE_START
            // Clean up any existing data
            Mono<Void> cleanup = reactiveCommands.del("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy").then();
            cleanup.block();
            // REMOVE_END

            // STEP_START sadd
            Mono<Void> sAdd = reactiveCommands.sadd("bikes:racing:france", "bike:1").doOnNext(result -> {
                System.out.println(result); // >>> 1
                // REMOVE_START
                assertThat(result).isEqualTo(1L);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.sadd("bikes:racing:france", "bike:1")).doOnNext(result -> {
                System.out.println(result); // >>> 0
                // REMOVE_START
                assertThat(result).isEqualTo(0L);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.sadd("bikes:racing:france", "bike:2", "bike:3")).doOnNext(result -> {
                System.out.println(result); // >>> 2
                // REMOVE_START
                assertThat(result).isEqualTo(2L);
                // REMOVE_END
            }).flatMap(v -> reactiveCommands.sadd("bikes:racing:usa", "bike:1", "bike:4")).doOnNext(result -> {
                System.out.println(result); // >>> 2
                // REMOVE_START
                assertThat(result).isEqualTo(2L);
                // REMOVE_END
            }).then();
            // STEP_END
            sAdd.block();

            // STEP_START sismember
            Mono<Boolean> sIsMember1 = reactiveCommands.sismember("bikes:racing:usa", "bike:1").doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<Boolean> sIsMember2 = reactiveCommands.sismember("bikes:racing:usa", "bike:2").doOnNext(result -> {
                System.out.println(result); // >>> false
                // REMOVE_START
                assertThat(result).isFalse();
                // REMOVE_END
            });
            // STEP_END

            // STEP_START sinter
            Mono<List<String>> sInter = reactiveCommands.sinter("bikes:racing:france", "bikes:racing:usa").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [bike:1]
                        // REMOVE_START
                        assertThat(result).containsExactly("bike:1");
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START scard
            Mono<Long> sCard = reactiveCommands.scard("bikes:racing:france").doOnNext(result -> {
                System.out.println(result); // >>> 3
                // REMOVE_START
                assertThat(result).isEqualTo(3L);
                // REMOVE_END
            });
            // STEP_END

            Mono.when(sIsMember1, sIsMember2, sInter, sCard).block();

            // STEP_START sadd_smembers
            Mono<Void> sAddSMembers = reactiveCommands.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3")
                    .doOnNext(result -> {
                        System.out.println(result); // >>> 3
                        // REMOVE_START
                        assertThat(result).isEqualTo(0L);
                        // REMOVE_END
                    }).flatMap(v -> reactiveCommands.smembers("bikes:racing:france").collectList()).doOnNext(result -> {
                        System.out.println(result); // >>> [bike:1, bike:2, bike:3]
                        // REMOVE_START
                        assertThat(result).containsExactlyInAnyOrder("bike:1", "bike:2", "bike:3");
                        // REMOVE_END
                    }).then();
            // STEP_END
            sAddSMembers.block();

            // STEP_START smismember
            Mono<Boolean> sIsMember3 = reactiveCommands.sismember("bikes:racing:france", "bike:1").doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<List<Boolean>> sMIsMember = reactiveCommands.smismember("bikes:racing:france", "bike:2", "bike:3", "bike:4")
                    .collectList().doOnNext(result -> {
                        System.out.println(result); // >>> [true, true, false]
                        // REMOVE_START
                        assertThat(result).containsExactly(true, true, false);
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START sdiff
            Mono<List<String>> sDiff = reactiveCommands.sdiff("bikes:racing:france", "bikes:racing:usa").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [bike:2, bike:3]
                        // REMOVE_START
                        assertThat(result).containsExactlyInAnyOrder("bike:2", "bike:3");
                        // REMOVE_END
                    });
            // STEP_END

            Mono.when(sIsMember3, sMIsMember, sDiff).block();

            // STEP_START multisets
            Mono<Void> add3sets = reactiveCommands.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3")
                    .doOnNext(result -> {
                        System.out.println(result); // >>> 0
                        // REMOVE_START
                        assertThat(result).isEqualTo(0L);
                        // REMOVE_END
                    }).flatMap(v -> reactiveCommands.sadd("bikes:racing:usa", "bike:1", "bike:4")).doOnNext(result -> {
                        System.out.println(result); // >>> 0
                        // REMOVE_START
                        assertThat(result).isEqualTo(0L);
                        // REMOVE_END
                    }).flatMap(v -> reactiveCommands.sadd("bikes:racing:italy", "bike:1", "bike:2", "bike:3", "bike:4"))
                    .doOnNext(result -> {
                        System.out.println(result); // >>> 4,
                        // REMOVE_START
                        assertThat(result).isEqualTo(4L);
                        // REMOVE_END
                    }).then();
            add3sets.block();

            Mono<List<String>> multisets1 = reactiveCommands
                    .sinter("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy").collectList().doOnNext(result -> {
                        System.out.println(result); // >>> [bike:1]
                        // REMOVE_START
                        assertThat(result).containsExactly("bike:1");
                        // REMOVE_END
                    });

            Mono<List<String>> multisets2 = reactiveCommands
                    .sunion("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy").collectList().doOnNext(result -> {
                        System.out.println(result); // >>> [bike:1, bike:2, bike:3, bike:4]
                        // REMOVE_START
                        assertThat(result).containsExactlyInAnyOrder("bike:1", "bike:2", "bike:3", "bike:4");
                        // REMOVE_END
                    });

            Mono<List<String>> multisets3 = reactiveCommands
                    .sdiff("bikes:racing:france", "bikes:racing:usa", "bikes:racing:italy").collectList().doOnNext(result -> {
                        System.out.println(result); // >>> []
                        // REMOVE_START
                        assertThat(result).isEmpty();
                        // REMOVE_END
                    });

            Mono<List<String>> multisets4 = reactiveCommands.sdiff("bikes:racing:usa", "bikes:racing:france").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [bike:4]
                        // REMOVE_START
                        assertThat(result).containsExactly("bike:4");
                        // REMOVE_END
                    });

            Mono<List<String>> multisets5 = reactiveCommands.sdiff("bikes:racing:france", "bikes:racing:usa").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [bike:2, bike:3]
                        // REMOVE_START
                        assertThat(result).containsExactlyInAnyOrder("bike:2", "bike:3");
                        // REMOVE_END
                    });
            // STEP_END

            Mono.when(multisets1, multisets2, multisets3, multisets4, multisets5).block();

            // STEP_START srem
            Mono<Void> sRem = reactiveCommands.sadd("bikes:racing:france", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5")
                    .doOnNext(result -> {
                        System.out.println(result); // >>> 2
                        // REMOVE_START
                        assertThat(result).isEqualTo(2L);
                        // REMOVE_END
                    }).flatMap(v -> reactiveCommands.srem("bikes:racing:france", "bike:1")).doOnNext(result -> {
                        System.out.println(result); // >>> 1
                        // REMOVE_START
                        assertThat(result).isEqualTo(1L);
                        // REMOVE_END
                    }).flatMap(v -> reactiveCommands.spop("bikes:racing:france")).doOnNext(result -> {
                        System.out.println(result); // >>> bike:3 (for example)
                    }).flatMap(v -> reactiveCommands.smembers("bikes:racing:france").collectList()).doOnNext(result -> {
                        System.out.println(result); // >>> [bike:2, bike:4, bike:5] (for example)
                    }).flatMap(v -> reactiveCommands.srandmember("bikes:racing:france")).doOnNext(result -> {
                        System.out.println(result); // >>> bike:4 (for example)
                    }).then();
            // STEP_END

            sRem.block();
        } finally {
            redisClient.shutdown();
        }
    }

}
