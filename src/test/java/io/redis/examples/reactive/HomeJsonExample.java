// EXAMPLE: lettuce_home_json
package io.redis.examples.reactive;

// STEP_START import
import io.lettuce.core.*;

import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.reactive.RediSearchReactiveCommands;
import io.lettuce.core.search.arguments.*;
import io.lettuce.core.search.arguments.AggregateArgs.*;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.AggregationReply;

import io.lettuce.core.json.JsonParser;
import io.lettuce.core.json.JsonObject;
import io.lettuce.core.json.JsonPath;

import io.lettuce.core.api.StatefulRedisConnection;

import java.util.*;
import reactor.core.publisher.Mono;
// STEP_END
// REMOVE_START
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

public class HomeJsonExample {

    @Test
    public void run() {
        // STEP_START connect
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();
            RediSearchReactiveCommands<String, String> searchCommands = connection.reactive();
            // ...
            // STEP_END
            // REMOVE_START
            reactiveCommands.del("user:1", "user:2", "user:3", "huser:1", "huser:2", "huser:3").block();
            searchCommands.ftDropindex("idx:users").onErrorReturn("Index `idx:users` does not exist").block();
            searchCommands.ftDropindex("hash-idx:users").onErrorReturn("Index `hash-idx:users` does not exist").block();
            // REMOVE_END

            // STEP_START create_data
            JsonParser parser = reactiveCommands.getJsonParser();
            JsonObject user1 = parser.createJsonObject().put("name", parser.createJsonValue("\"Paul John\""))
                    .put("email", parser.createJsonValue("\"paul.john@example.com\"")).put("age", parser.createJsonValue("42"))
                    .put("city", parser.createJsonValue("\"London\""));

            JsonObject user2 = parser.createJsonObject().put("name", parser.createJsonValue("\"Eden Zamir\""))
                    .put("email", parser.createJsonValue("\"eden.zamir@example.com\"")).put("age", parser.createJsonValue("29"))
                    .put("city", parser.createJsonValue("\"Tel Aviv\""));

            JsonObject user3 = parser.createJsonObject().put("name", parser.createJsonValue("\"Paul Zamir\""))
                    .put("email", parser.createJsonValue("\"paul.zamir@example.com\"")).put("age", parser.createJsonValue("35"))
                    .put("city", parser.createJsonValue("\"Tel Aviv\""));
            // STEP_END

            // STEP_START make_index
            List<FieldArgs<String>> schema = Arrays.asList(TextFieldArgs.<String> builder().name("$.name").as("name").build(),
                    NumericFieldArgs.<String> builder().name("$.age").as("age").build(),
                    TagFieldArgs.<String> builder().name("$.city").as("city").build());

            CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().on(CreateArgs.TargetType.JSON)
                    .withPrefix("user:").build();

            Mono<Void> make_index = searchCommands.ftCreate("idx:users", createArgs, schema).doOnNext(res -> {
                // REMOVE_START
                assertThat(res).isEqualTo("OK");
                // REMOVE_END
                System.out.println(res); // >>> OK
            }).then();
            // STEP_END
            make_index.block();

            // STEP_START add_data
            Mono<String> addUser1 = reactiveCommands.jsonSet("user:1", JsonPath.ROOT_PATH, user1).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END
            });

            Mono<String> addUser2 = reactiveCommands.jsonSet("user:2", JsonPath.ROOT_PATH, user2).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END
            });

            Mono<String> addUser3 = reactiveCommands.jsonSet("user:3", JsonPath.ROOT_PATH, user3).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo("OK");
                // REMOVE_END
            });
            // STEP_END
            Mono.when(addUser1, addUser2, addUser3).block();

            // STEP_START query1
            Mono<SearchReply<String, String>> query1 = searchCommands.ftSearch("idx:users", "Paul @age:[30 40]")
                    .doOnNext(res -> {
                        List<SearchReply.SearchResult<String, String>> results = res.getResults();

                        results.forEach(result -> {
                            System.out.println(result.getId());
                        });
                        // >>> user:3
                        // REMOVE_START
                        assertThat(res.getCount()).isEqualTo(1);
                        assertThat(results.get(0).getId()).isEqualTo("user:3");
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START query2
            SearchArgs<String, String> query2Args = SearchArgs.<String, String> builder().returnField("city").build();

            Mono<SearchReply<String, String>> query2 = searchCommands.ftSearch("idx:users", "Paul", query2Args)
                    .doOnNext(res -> {
                        List<SearchReply.SearchResult<String, String>> results = res.getResults();

                        results.forEach(result -> {
                            System.out.printf("ID: %s, City: %s\n", result.getId(), result.getFields().get("city"));
                        });
                        // >>> ID: user:1, City: London
                        // >>> ID: user:3, City: Tel Aviv
                        // REMOVE_START
                        assertThat(res.getCount()).isEqualTo(2);
                        assertThat(results.stream().map(result -> {
                            return String.format("ID: %s, City: %s", result.getId(), result.getFields().get("city"));
                        }).sorted().toArray()).containsExactly("ID: user:1, City: London", "ID: user:3, City: Tel Aviv");
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START query3
            AggregateArgs<String, String> aggArgs = AggregateArgs.<String, String> builder()
                    .groupBy(GroupBy.<String, String> of("@city").reduce(Reducer.<String, String> count().as("count"))).build();

            Mono<AggregationReply<String, String>> query3 = searchCommands.ftAggregate("idx:users", "*", aggArgs)
                    .doOnNext(res -> {
                        List<SearchReply<String, String>> replies = res.getReplies();
                        replies.forEach(reply -> {
                            reply.getResults().forEach(result -> {
                                System.out.printf("City: %s, Count: %s\n", result.getFields().get("city"),
                                        result.getFields().get("count"));
                            });
                            // >>> City: London, Count: 1
                            // >>> City: Tel Aviv, Count: 2
                        });
                        // REMOVE_START
                        assertThat(replies.size()).isEqualTo(1);
                        assertThat(replies.get(0).getResults().size()).isEqualTo(2);
                        assertThat(replies.get(0).getResults().stream().map(result -> {
                            return String.format("City: %s, Count: %s", result.getFields().get("city"),
                                    result.getFields().get("count"));
                        }).sorted().toArray()).containsExactly("City: London, Count: 1", "City: Tel Aviv, Count: 2");
                        // REMOVE_END
                    });
            // STEP_END

            Mono.when(query1, query2, query3).block();

            // STEP_START make_hash_index
            List<FieldArgs<String>> hashSchema = Arrays.asList(TextFieldArgs.<String> builder().name("name").build(),
                    NumericFieldArgs.<String> builder().name("age").build(),
                    TagFieldArgs.<String> builder().name("city").build());

            CreateArgs<String, String> hashCreateArgs = CreateArgs.<String, String> builder().on(CreateArgs.TargetType.HASH)
                    .withPrefix("huser:").build();

            Mono<String> makeHashIndex = searchCommands.ftCreate("hash-idx:users", hashCreateArgs, hashSchema).doOnNext(res -> {
                // REMOVE_START
                assertThat(res).isEqualTo("OK");
                // REMOVE_END
                System.out.println(res); // >>> OK
            });
            // STEP_END
            makeHashIndex.block();

            // STEP_START add_hash_data
            Map<String, String> huser1 = new HashMap<>();
            huser1.put("name", "Paul John");
            huser1.put("email", "paul.john@example.com");
            huser1.put("age", "42");
            huser1.put("city", "London");

            Map<String, String> huser2 = new HashMap<>();
            huser2.put("name", "Eden Zamir");
            huser2.put("email", "eden.zamir@example.com");
            huser2.put("age", "29");
            huser2.put("city", "Tel Aviv");

            Map<String, String> huser3 = new HashMap<>();
            huser3.put("name", "Paul Zamir");
            huser3.put("email", "paul.zamir@example.com");
            huser3.put("age", "35");
            huser3.put("city", "Tel Aviv");

            Mono<Long> addHashUser1 = reactiveCommands.hset("huser:1", huser1).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo(4L);
                // REMOVE_END
            });

            Mono<Long> addHashUser2 = reactiveCommands.hset("huser:2", huser2).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo(4L);
                // REMOVE_END
            });

            Mono<Long> addHashUser3 = reactiveCommands.hset("huser:3", huser3).doOnNext(r -> {
                System.out.println(r); // >>> OK
                // REMOVE_START
                assertThat(r).isEqualTo(4L);
                // REMOVE_END
            });
            // STEP_END
            Mono.when(addHashUser1, addHashUser2, addHashUser3).block();

            // STEP_START query1_hash
            Mono<SearchReply<String, String>> query1Hash = searchCommands.ftSearch("hash-idx:users", "Paul @age:[30 40]")
                    .doOnNext(res -> {
                        List<SearchReply.SearchResult<String, String>> results = res.getResults();

                        results.forEach(result -> {
                            System.out.println(result.getId());
                        });
                        // >>> huser:3
                        // REMOVE_START
                        assertThat(res.getCount()).isEqualTo(1);
                        assertThat(results.get(0).getId()).isEqualTo("huser:3");
                        // REMOVE_END
                    });
            // STEP_END
            query1Hash.block();
        } finally {
            redisClient.shutdown();
        }
    }

}
