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
import io.lettuce.core.json.JsonPath;
import io.lettuce.core.search.arguments.AggregateArgs;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.SortByArgs;
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

    // ── CreateArgs.prefix ────────────────────────────────────────────────────────

    /**
     * {@code FT.CREATE ... PREFIX 1 doc:} restricts the index to keys whose raw Redis key starts with the given prefix. When a
     * prefixing codec is on the connection, the user writes {@code withPrefix("doc:")} and the data is stored under
     * {@code "tenant1:doc:1"} (encoded by the codec). For the PREFIX to match the actual stored key, the prefix must also be
     * routed through {@code encodeKey} — producing {@code "tenant1:doc:"} on the wire. Currently {@link CreateArgs} writes the
     * prefix via {@code args.add(p.toString())} (raw), so the wire gets {@code "doc:"} which does NOT match
     * {@code "tenant1:doc:1"}. This test verifies the current behavior and documents the inconsistency.
     */
    @Test
    void prefixInCreateArgsMustBeEncodedByCodecToMatchStoredKeys() {
        redis.flushall();

        // Create index WITH a prefix — the bug is that the prefix goes raw ("doc:") while hmset encodes keys
        // ("tenant1:doc:1")
        CreateArgs prefixCreate = CreateArgs.builder().on(CreateArgs.TargetType.HASH).withPrefix("doc:").build();
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        redis.ftCreate("prefix-test-idx", prefixCreate, Arrays.asList(titleField));

        Map<String, String> doc = new HashMap<>();
        doc.put("title", "Redis search guide");
        redis.hmset("doc:1", doc);

        // Wait for indexing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SearchReply<String, String> result = redis.ftSearch("prefix-test-idx", "search");

        // If prefix encoding is correct (through codec), this should find the document.
        // If prefix goes raw while keys are encoded, the prefix "doc:" won't match "tenant1:doc:1" and count will be 0.
        assertThat(result.getCount()).as("PREFIX in CreateArgs should be encoded through codec to match stored keys; "
                + "if this fails with 0, the prefix is being sent raw instead of through addKey").isEqualTo(1L);
    }

    // ── SortBy ─────────────────────────────────────────────────────────────────

    /**
     * {@code SORTBY} names a schema field declared at {@code FT.CREATE} time. Since the "everything encoded" model routes
     * schema field names through {@code encodeKey} in both {@code FT.CREATE} (via {@code FieldArgs.addKey(name)}) and
     * {@code SORTBY} (via {@code SortByArgs.addKey(attribute)}), the encoding is symmetric and the server should resolve the
     * field correctly.
     */
    @Test
    void sortByFieldMustNotBeMangledByCodec() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder()
                .sortBy(SortByArgs.<String> builder().attribute("title").build()).build();

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "*", args);

        assertThat(result.getCount()).as("SORTBY schema field must resolve correctly through codec").isEqualTo(1L);
    }

    // ── JSON index tests ───────────────────────────────────────────────────────

    /**
     * Baseline for JSON indexes with a prefixing codec. {@code JSON.SET} sends the JSON payload via {@code args.add(String)}
     * (raw), so field names inside JSON are never touched by the codec. Schema field names (JSONPaths like {@code $.title}) go
     * through {@code addKey} in {@code FieldArgs.build()}, but since the codec prefixes them to {@code "tenant1:$.title"} on
     * the wire, the server won't match them against the literal {@code $.title} inside the JSON document — unless the same
     * encoding is applied symmetrically at create and query time. This test documents the current behavior.
     */
    @Test
    void jsonBaselineSearchWorksThroughPrefixingCodec() {
        redis.flushall();

        // Create JSON index — no prefix filter (defaults to all keys)
        CreateArgs jsonCreate = CreateArgs.builder().on(CreateArgs.TargetType.JSON).build();
        FieldArgs<String> jsonTitle = TextFieldArgs.<String> builder().name("$.title").as("title").build();
        FieldArgs<String> jsonBody = TextFieldArgs.<String> builder().name("$.body").as("body").build();
        FieldArgs<String> jsonCategory = TagFieldArgs.<String> builder().name("$.category").as("category").build();
        redis.ftCreate("json-codec-idx", jsonCreate, Arrays.asList(jsonTitle, jsonBody, jsonCategory));

        // Add JSON document — the key goes through codec, but JSON payload is raw
        String jsonDoc = "{\"title\":\"Redis search guide\","
                + "\"body\":\"A long introduction to Redis that mentions search in the body text.\","
                + "\"category\":\"tutorial\"}";
        redis.jsonSet("json:1", JsonPath.ROOT_PATH, jsonDoc);

        // Wait for indexing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SearchReply<String, String> result = redis.ftSearch("json-codec-idx", "search");

        assertThat(result.getCount()).as("JSON baseline search should find the document").isEqualTo(1L);
    }

    /**
     * {@code INFIELDS} on a JSON index references the AS alias declared at schema creation time. With a prefixing codec, the
     * alias is sent raw (it's a {@code String}, not routed through codec) while the field path goes through {@code addKey}.
     * This test verifies whether INFIELDS resolves correctly against the schema alias.
     */
    @Test
    void jsonInFieldMustNotBeMangledByCodec() {
        redis.flushall();

        CreateArgs jsonCreate = CreateArgs.builder().on(CreateArgs.TargetType.JSON).build();
        FieldArgs<String> jsonTitle = TextFieldArgs.<String> builder().name("$.title").as("title").build();
        FieldArgs<String> jsonBody = TextFieldArgs.<String> builder().name("$.body").as("body").build();
        redis.ftCreate("json-infield-idx", jsonCreate, Arrays.asList(jsonTitle, jsonBody));

        String jsonDoc = "{\"title\":\"Redis search guide\",\"body\":\"Some body text about search\"}";
        redis.jsonSet("json:1", JsonPath.ROOT_PATH, jsonDoc);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // INFIELDS uses the alias "title" — which must match the AS alias in the schema
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().inField("title").build();
        SearchReply<String, String> result = redis.ftSearch("json-infield-idx", "search", args);

        assertThat(result.getCount()).as("JSON INFIELDS with alias must resolve correctly").isEqualTo(1L);
    }

    /**
     * {@code RETURN} on a JSON index should return field values using the schema alias. Verifies that the alias appears
     * verbatim in the result map despite the prefixing codec.
     */
    @Test
    void jsonReturnFieldMustNotBeMangledByCodec() {
        redis.flushall();

        CreateArgs jsonCreate = CreateArgs.builder().on(CreateArgs.TargetType.JSON).build();
        FieldArgs<String> jsonTitle = TextFieldArgs.<String> builder().name("$.title").as("title").build();
        redis.ftCreate("json-return-idx", jsonCreate, Arrays.asList(jsonTitle));

        String jsonDoc = "{\"title\":\"Redis search guide\"}";
        redis.jsonSet("json:1", JsonPath.ROOT_PATH, jsonDoc);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SearchArgs<String, String> args = SearchArgs.<String, String> builder().returnField("title").build();
        SearchReply<String, String> result = redis.ftSearch("json-return-idx", "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields())
                .as("JSON RETURN field must appear verbatim as a key in the result map").containsKey("title");
    }

    /**
     * {@code FT.AGGREGATE ... LOAD} on a JSON index with prefixing codec. The LOAD field should reference the schema alias and
     * the value must be returned correctly.
     */
    @Test
    void jsonAggregateLoadFieldMustNotBeMangledByCodec() {
        redis.flushall();

        CreateArgs jsonCreate = CreateArgs.builder().on(CreateArgs.TargetType.JSON).build();
        FieldArgs<String> jsonTitle = TextFieldArgs.<String> builder().name("$.title").as("title").build();
        redis.ftCreate("json-agg-idx", jsonCreate, Arrays.asList(jsonTitle));

        String jsonDoc = "{\"title\":\"Redis search guide\"}";
        redis.jsonSet("json:1", JsonPath.ROOT_PATH, jsonDoc);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().load("title").build();
        AggregationReply<String, String> result = redis.ftAggregate("json-agg-idx", "*", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String, String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).isNotEmpty();
        assertThat(reply.getResults().get(0).getFields()).as("JSON AGGREGATE LOAD field must be fetched verbatim")
                .containsKey("title");
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
