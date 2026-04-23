/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
public class RediSearchCodecSafetyIntegrationTests {

    private static final String INDEX = "codec-safety-idx";

    private static final String CODEC_PREFIX = "tenant1:";

    private static RedisClient client;

    private static StatefulRedisConnection<String, String> connection;

    private static RedisCommands<String, String> redis;

    public RediSearchCodecSafetyIntegrationTests() {
        RedisURI uri = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(uri);
        connection = client.connect(new PrefixingStringCodec(CODEC_PREFIX));
        redis = connection.sync();
    }

    @BeforeEach
    void prepare() {
        redis.flushall();

        CreateArgs createArgs = CreateArgs.builder().on(CreateArgs.TargetType.HASH).build();
        FieldArgs title = TextFieldArgs.builder().name("title").build();
        FieldArgs body = TextFieldArgs.builder().name("body").build();
        FieldArgs category = TagFieldArgs.builder().name("category").build();
        redis.ftCreate(INDEX, createArgs, Arrays.asList(title, body, category));

        Map<String, String> doc = new HashMap<>();
        doc.put("title", "Redis search guide");
        doc.put("body",
                "A long introduction to Redis that mentions search only deep into the body text. "
                        + "Much preamble about indices and storage, only later do we finally cover search. "
                        + "More content about search follows with many examples. "
                        + "Finally more content after the search occurrences ends the description.");
        doc.put("category", "tutorial");
        redis.hmset("1", doc);
    }

    @AfterAll
    static void teardown() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * Sanity check. The query string is written raw via {@code args.add(query)} so this must work even when a prefixing codec
     * is installed on the connection.
     */
    @Test
    void baselineSearchWorksThroughPrefixingCodec() {
        SearchReply<String, String> result = redis.ftSearch(INDEX, "search");
        assertThat(result.getCount()).isEqualTo(1L);
    }

    /**
     * {@code INFIELDS} names schema fields declared at {@code FT.CREATE} time — they must be written raw. With the current bug,
     * {@code addKeys(inFields)} routes them through the codec producing {@code tenant1:title}, and the server rejects it with
     * {@code Unknown field}.
     */
    @Test
    void inFieldMustNotBeMangledByCodec() {
        SearchArgs<String> args = SearchArgs.<String> builder().inField("title").build();

        SearchReply<String, String> result = redis.ftSearch(INDEX, "search", args);

        assertThat(result.getCount()).as("INFIELDS schema field must be sent raw, not codec-encoded").isEqualTo(1L);
    }

    /**
     * {@code RETURN} field names and their {@code AS} aliases are schema identifiers. With the current bug, both get
     * codec-encoded (the map key and the alias), producing a mismatch against the index schema.
     */
    @Test
    void returnFieldMustNotBeMangledByCodec() {
        SearchArgs<String> args = SearchArgs.<String> builder().returnField("title").build();

        SearchReply<String, String> result = redis.ftSearch(INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields())
                .as("RETURN schema field must appear verbatim as a key in the result map").containsKey("title");
    }

    /**
     * {@code RETURN ... AS alias} — the alias is also a schema-level identifier and must not be codec-encoded.
     */
    @Test
    void returnFieldAliasMustNotBeMangledByCodec() {
        SearchArgs<String> args = SearchArgs.<String> builder().returnField("title", "t").build();

        SearchReply<String, String> result = redis.ftSearch(INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields()).as("RETURN alias must appear verbatim as a key in the result map")
                .containsKey("t");
    }

    /**
     * {@code SUMMARIZE FIELDS} names schema fields. When it is applied correctly Redis abbreviates the field content and
     * terminates it with the configured separator (default {@code ...}). With the current bug, the mangled field name is not
     * recognised and the body is either unchanged or the server errors out.
     */
    @Test
    void summarizeFieldMustNotBeMangledByCodec() {
        SearchArgs<String> args = SearchArgs.<String> builder().summarizeField("body").build();

        SearchReply<String, String> result = redis.ftSearch(INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        String body = result.getResults().get(0).getFields().get("body");
        assertThat(body).as("SUMMARIZE must be applied to the 'body' field").contains("...");
    }

    /**
     * {@code HIGHLIGHT FIELDS} names schema fields. When applied, matching terms are wrapped with the configured tags. With the
     * current bug, the mangled field name is not recognised and the body is returned untouched.
     */
    @Test
    void highlightFieldMustNotBeMangledByCodec() {
        SearchArgs<String> args = SearchArgs.<String> builder().highlightField("body").highlightTags("<b>", "</b>").build();

        SearchReply<String, String> result = redis.ftSearch(INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        String body = result.getResults().get(0).getFields().get("body");
        assertThat(body).as("HIGHLIGHT must wrap 'search' occurrences with the configured tags").contains("<b>search</b>");
    }

    /**
     * {@code FT.AGGREGATE ... LOAD} names schema fields. This is the same protocol clause as
     * {@link io.lettuce.core.search.arguments.hybrid.PostProcessingArgs} which already sends it verbatim. {@link AggregateArgs}
     * currently routes the field name through {@code addKey} and produces a mismatch.
     */
    @Test
    void aggregateLoadFieldMustNotBeMangledByCodec() {
        AggregateArgs<String> args = AggregateArgs.<String> builder().load("title").build();

        AggregationReply<String, String> result = redis.ftAggregate(INDEX, "*", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String, String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).isNotEmpty();
        assertThat(reply.getResults().get(0).getFields()).as("LOAD schema field must be fetched verbatim").containsKey("title");
    }

    /**
     * Minimal codec that prefixes every encoded key with a fixed prefix and strips that prefix on decode when present. Values
     * are passed through unchanged. Modelled after a tenant-scoping key transformation that real applications use to partition
     * a Redis database by tenant id.
     */
    static class PrefixingStringCodec implements RedisCodec<String, String> {

        private final String prefix;

        PrefixingStringCodec(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String decodeKey(ByteBuffer bytes) {
            String s = StandardCharsets.UTF_8.decode(bytes).toString();
            return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return StandardCharsets.UTF_8.encode(prefix + key);
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            return StandardCharsets.UTF_8.encode(value);
        }

    }

}
