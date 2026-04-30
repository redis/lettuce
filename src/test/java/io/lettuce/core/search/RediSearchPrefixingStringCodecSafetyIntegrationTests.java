/*
 * Copyright 2026-present
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

/**
 * Codec-safety invariants for RediSearch when the connection key codec is non-trivial. Scoped to {@code ON HASH} indexes
 * because only {@code HSET}/{@code HMSET} route hash field names and values through the connection's {@link RedisCodec}
 * ({@link io.lettuce.core.protocol.CommandArgs#add(java.util.Map)} encodes each map entry as
 * {@code addKey(field).addValue(value)}). {@code JSON.SET} does not — the JSON payload is sent verbatim via
 * {@link io.lettuce.core.protocol.CommandArgs#add(String)} (or {@code add(byte[])} for {@link io.lettuce.core.json.JsonValue}),
 * bypassing the value codec entirely; consequently a non-trivial codec produces no observable mismatch on the JSON side and
 * adding {@code ON JSON} variants here would only duplicate the HASH coverage. Schema-level identifiers (field names, aliases)
 * must be sent raw; only the legitimately {@code K}-typed surface (e.g. {@code INKEYS}) must route through {@code encodeKey}.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchPrefixingStringCodecSafetyIntegrationTests {

    private static final String HASH_INDEX = "codec-safety-idx";

    private static final String HASH_DOC_KEY = "1";

    private static final String BODY_TEXT = "A long introduction to Redis that mentions search only deep into the body text. "
            + "Much preamble about indices and storage, only later do we finally cover search. "
            + "More content about search follows with many examples. "
            + "Finally more content after the search occurrences ends the description.";

    private static final String CODEC_PREFIX = "tenant1:";

    private static RedisClient client;

    private static StatefulRedisConnection<String, String> connection;

    private static RedisCommands<String, String> redis;

    public RediSearchPrefixingStringCodecSafetyIntegrationTests() {
        RedisURI uri = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(uri);
        connection = client.connect(new PrefixingStringCodec(CODEC_PREFIX));
        redis = connection.sync();
    }

    @BeforeEach
    void prepare() {
        redis.flushall();

        CreateArgs hashCreate = CreateArgs.builder().on(CreateArgs.TargetType.HASH).build();
        FieldArgs hashTitle = TextFieldArgs.builder().name("title").build();
        FieldArgs hashBody = TextFieldArgs.builder().name("body").build();
        FieldArgs hashCategory = TagFieldArgs.builder().name("category").build();
        redis.ftCreate(HASH_INDEX, hashCreate, Arrays.asList(hashTitle, hashBody, hashCategory));

        Map<String, String> doc = new HashMap<>();
        doc.put("title", "Redis search guide");
        doc.put("body", BODY_TEXT);
        doc.put("category", "tutorial");
        redis.hmset(HASH_DOC_KEY, doc);
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
        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "search");
        assertThat(result.getCount()).isEqualTo(1L);
    }

    /**
     * {@code INFIELDS} names schema fields declared at {@code FT.CREATE} time — they must be written raw. With the current bug,
     * {@code addKeys(inFields)} routes them through the codec producing {@code tenant1:title}, and the server rejects it with
     * {@code Unknown field}.
     */
    @Test
    void inFieldMustNotBeMangledByCodec() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().inField("title").build();

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).as("INFIELDS schema field must be sent raw, not codec-encoded").isEqualTo(1L);
    }

    /**
     * {@code RETURN} field names and their {@code AS} aliases are schema identifiers. With the current bug, both get
     * codec-encoded (the map key and the alias), producing a mismatch against the index schema.
     */
    @Test
    void returnFieldMustNotBeMangledByCodec() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().returnField("title").build();

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields())
                .as("RETURN schema field must appear verbatim as a key in the result map").containsKey("title");
    }

    /**
     * {@code RETURN ... AS alias} — the alias is also a schema-level identifier and must not be codec-encoded.
     */
    @Test
    void returnFieldAliasMustNotBeMangledByCodec() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().returnField("title", "t").build();

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "search", args);

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
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().summarizeField("body").build();

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "search", args);

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
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().highlightField("body")
                .highlightTags("<b>", "</b>").build();

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "search", args);

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
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().load("title").build();

        AggregationReply<String, String> result = redis.ftAggregate(HASH_INDEX, "*", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String, String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).isNotEmpty();
        assertThat(reply.getResults().get(0).getFields()).as("LOAD schema field must be fetched verbatim").containsKey("title");
    }

    /**
     * {@code INKEYS} restricts the search to a list of actual document keys. Unlike the schema-identifier clauses above, this
     * one is legitimately {@code K}-typed and {@link io.lettuce.core.protocol.CommandArgs#addKeys} must route through
     * {@code encodeKey} so the prefix matches what was applied at write-time. Acts as a positive control: if the key channel
     * stops routing through the codec, this assertion stops finding the document.
     */
    @Test
    void inKeyMustBeRoutedThroughCodec() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().inKey(HASH_DOC_KEY).build();

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).as("INKEYS must route the document key through encodeKey to match the stored prefix")
                .isEqualTo(1L);
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
