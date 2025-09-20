// EXAMPLE: cmds_list
package io.redis.examples.reactive;

import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.StatefulRedisConnection;

// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

import reactor.core.publisher.Mono;

public class CmdsListExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();
            // REMOVE_START
            reactiveCommands.del("mylist").block();
            // REMOVE_END

            // STEP_START lpush
            Mono<Void> lpush = reactiveCommands.lpush("mylist", "world").doOnNext(res1 -> {
                System.out.println(res1); // >>> 1
                // REMOVE_START
                assertThat(res1).isEqualTo(1);
                // REMOVE_END
            }).flatMap(res1 -> reactiveCommands.lpush("mylist", "hello")).doOnNext(res2 -> {
                System.out.println(res2); // >>> 2
                // REMOVE_START
                assertThat(res2).isEqualTo(2);
                // REMOVE_END
            }).flatMap(res2 -> reactiveCommands.lrange("mylist", 0, -1).collectList()).doOnNext(res3 -> {
                System.out.println(res3); // >>> [hello, world]
                // REMOVE_START
                assertThat(res3.toArray()).isEqualTo(new String[] { "hello", "world" });
                // REMOVE_END
            }).then();
            // STEP_END
            lpush.block();
            reactiveCommands.del("mylist").block();

            // STEP_START lrange
            Mono<Void> lrange = reactiveCommands.rpush("mylist", "one").doOnNext(res4 -> {
                System.out.println(res4); // >>> 1
                // REMOVE_START
                assertThat(res4).isEqualTo(1);
                // REMOVE_END
            }).flatMap(res4 -> reactiveCommands.rpush("mylist", "two")).doOnNext(res5 -> {
                System.out.println(res5); // >>> 2
                // REMOVE_START
                assertThat(res5).isEqualTo(2);
                // REMOVE_END
            }).flatMap(res5 -> reactiveCommands.rpush("mylist", "three")).doOnNext(res6 -> {
                System.out.println(res6); // >>> 3
                // REMOVE_START
                assertThat(res6).isEqualTo(3);
                // REMOVE_END
            }).flatMap(res6 -> reactiveCommands.lrange("mylist", 0, 0).collectList()).doOnNext(res7 -> {
                System.out.println(res7); // >>> [one]
                // REMOVE_START
                assertThat(res7.toArray()).isEqualTo(new String[] { "one" });
                // REMOVE_END
            }).flatMap(res7 -> reactiveCommands.lrange("mylist", -3, 2).collectList()).doOnNext(res8 -> {
                System.out.println(res8); // >>> [one, two, three]
                // REMOVE_START
                assertThat(res8.toArray()).isEqualTo(new String[] { "one", "two", "three" });
                // REMOVE_END
            }).flatMap(res8 -> reactiveCommands.lrange("mylist", -100, 100).collectList()).doOnNext(res9 -> {
                System.out.println(res9); // >>> [one, two, three]
                // REMOVE_START
                assertThat(res9.toArray()).isEqualTo(new String[] { "one", "two", "three" });
                // REMOVE_END
            }).flatMap(res9 -> reactiveCommands.lrange("mylist", 5, 10).collectList()).doOnNext(res10 -> {
                System.out.println(res10); // >>> []
                // REMOVE_START
                assertThat(res10.toArray()).isEqualTo(new String[] {});
                // REMOVE_END
            }).then();
            // STEP_END
            lrange.block();
            reactiveCommands.del("mylist").block();

            // STEP_START llen
            Mono<Void> llen = reactiveCommands.lpush("mylist", "World").doOnNext(res11 -> {
                System.out.println(res11); // >>> 1
                // REMOVE_START
                assertThat(res11).isEqualTo(1);
                // REMOVE_END
            }).flatMap(res11 -> reactiveCommands.lpush("mylist", "Hello")).doOnNext(res12 -> {
                System.out.println(res12); // >>> 2
                // REMOVE_START
                assertThat(res12).isEqualTo(2);
                // REMOVE_END
            }).flatMap(res12 -> reactiveCommands.llen("mylist")).doOnNext(res13 -> {
                System.out.println(res13); // >>> 2
                // REMOVE_START
                assertThat(res13).isEqualTo(2);
                // REMOVE_END
            }).then();
            // STEP_END
            llen.block();
            reactiveCommands.del("mylist").block();

            // STEP_START rpush
            Mono<Void> rpush = reactiveCommands.rpush("mylist", "hello").doOnNext(res14 -> {
                System.out.println(res14); // >>> 1
                // REMOVE_START
                assertThat(res14).isEqualTo(1);
                // REMOVE_END
            }).flatMap(res14 -> reactiveCommands.rpush("mylist", "world")).doOnNext(res15 -> {
                System.out.println(res15); // >>> 2
                // REMOVE_START
                assertThat(res15).isEqualTo(2);
                // REMOVE_END
            }).flatMap(res15 -> reactiveCommands.lrange("mylist", 0, -1).collectList()).doOnNext(res16 -> {
                System.out.println(res16); // >>> [hello, world]
                // REMOVE_START
                assertThat(res16.toArray()).isEqualTo(new String[] { "hello", "world" });
                // REMOVE_END
            }).then();
            // STEP_END
            rpush.block();
            reactiveCommands.del("mylist").block();

            // STEP_START lpop
            Mono<Void> lpop = reactiveCommands.rpush("mylist", "one", "two", "three", "four", "five").doOnNext(res17 -> {
                System.out.println(res17); // >>> 5
                // REMOVE_START
                assertThat(res17).isEqualTo(5);
                // REMOVE_END
            }).flatMap(res17 -> reactiveCommands.lpop("mylist")).doOnNext(res18 -> {
                System.out.println(res18); // >>> one
                // REMOVE_START
                assertThat(res18).isEqualTo("one");
                // REMOVE_END
            }).flatMap(res18 -> reactiveCommands.lpop("mylist", 2).collectList()).doOnNext(res19 -> {
                System.out.println(res19); // >>> [two, three]
                // REMOVE_START
                assertThat(res19.toArray()).isEqualTo(new String[] { "two", "three" });
                // REMOVE_END
            }).flatMap(res19 -> reactiveCommands.lrange("mylist", 0, -1).collectList()).doOnNext(res17_final -> {
                System.out.println(res17_final); // >>> [four, five]
                // REMOVE_START
                assertThat(res17_final.toArray()).isEqualTo(new String[] { "four", "five" });
                // REMOVE_END
            }).then();
            // STEP_END
            lpop.block();
            reactiveCommands.del("mylist").block();

            // STEP_START rpop
            Mono<Void> rpop = reactiveCommands.rpush("mylist", "one", "two", "three", "four", "five").doOnNext(res18 -> {
                System.out.println(res18); // >>> 5
                // REMOVE_START
                assertThat(res18).isEqualTo(5);
                // REMOVE_END
            }).flatMap(res18 -> reactiveCommands.rpop("mylist")).doOnNext(res19 -> {
                System.out.println(res19); // >>> five
                // REMOVE_START
                assertThat(res19).isEqualTo("five");
                // REMOVE_END
            }).flatMap(res19 -> reactiveCommands.rpop("mylist", 2).collectList()).doOnNext(res20 -> {
                System.out.println(res20); // >>> [four, three]
                // REMOVE_START
                assertThat(res20.toArray()).isEqualTo(new String[] { "four", "three" });
                // REMOVE_END
            }).flatMap(res20 -> reactiveCommands.lrange("mylist", 0, -1).collectList()).doOnNext(res21 -> {
                System.out.println(res21); // >>> [one, two]
                // REMOVE_START
                assertThat(res21.toArray()).isEqualTo(new String[] { "one", "two" });
                // REMOVE_END
            }).then();
            // STEP_END
            rpop.block();
            reactiveCommands.del("mylist").block();

        } finally {
            redisClient.shutdown();
        }
    }

}
