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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mirror of {@link RediSearchPrefixingStringCodecSafetyIntegrationTests} using a connection codec whose key type is a
 * structured POJO instead of {@link String}. Asserts the codec-routing invariants for RediSearch: every schema-level identifier
 * (field names) is {@code K}-typed and routed through {@code codec.encodeKey} both at {@code FT.CREATE} and at
 * {@code FT.SEARCH}/{@code FT.AGGREGATE} time. Identifiers are passed as "bare" {@link RedisKey} instances built via
 * {@link #field(String)}; {@link RedisKeyCodec} encodes a bare key as its {@code id} bytes only, with no tenant/entity prefix,
 * so the wire shape matches the literal field name on both sides. If the codec ever bypasses one of these surfaces (or applies
 * a non-trivial transformation when it shouldn't), the bytes sent to the server diverge between {@code FT.CREATE} and the read
 * clauses, the schema lookup fails and the assertions below break. Covers both {@code ON HASH} and {@code ON JSON} indexes:
 * {@code HSET}/{@code HMSET} route hash field names and values through the connection's {@link RedisCodec}; {@code JSON.SET}
 * sends its payload verbatim via {@link io.lettuce.core.protocol.CommandArgs#add(String)} (or {@code add(byte[])} for
 * {@link io.lettuce.core.json.JsonValue}), bypassing the value codec entirely. The JSON schema uses bare {@link RedisKey} JSON
 * paths (e.g. {@code $.title}) and aliases them back to plain field names so the same read-side identifiers exercise both
 * indexes.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchStructuredKeyCodecSafetyIntegrationTests {

    private static final String HASH_INDEX = "codec-safety-rk-idx";

    private static final String JSON_INDEX = "codec-safety-rk-json-idx";

    private static final String HASH_DOC_ID = "1";

    private static final String JSON_DOC_ID = "json-1";

    private static final String TENANT = "tenant1";

    private static final String ENTITY = "doc";

    private static final String BODY_TEXT = "A long introduction to Redis that mentions search only deep into the body text. "
            + "Much preamble about indices and storage, only later do we finally cover search. "
            + "More content about search follows with many examples. "
            + "Finally more content after the search occurrences ends the description.";

    private static RedisClient client;

    private static StatefulRedisConnection<RedisKey, String> connection;

    private static RedisCommands<RedisKey, String> redis;

    public RediSearchStructuredKeyCodecSafetyIntegrationTests() {
        RedisURI uri = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(uri);
        connection = client.connect(new RedisKeyCodec());
        redis = connection.sync();
    }

    @BeforeEach
    void prepare() {
        redis.flushall();

        CreateArgs<RedisKey> hashCreate = CreateArgs.<RedisKey> builder().on(CreateArgs.TargetType.HASH).build();
        FieldArgs<RedisKey> hashTitle = TextFieldArgs.<RedisKey> builder().name(field("title")).sortable().build();
        FieldArgs<RedisKey> hashBody = TextFieldArgs.<RedisKey> builder().name(field("body")).build();
        FieldArgs<RedisKey> hashCategory = TagFieldArgs.<RedisKey> builder().name(field("category")).build();
        redis.ftCreate(HASH_INDEX, hashCreate, Arrays.asList(hashTitle, hashBody, hashCategory));

        Map<RedisKey, String> doc = new HashMap<>();
        doc.put(field("title"), "Redis search guide");
        doc.put(field("body"), BODY_TEXT);
        doc.put(field("category"), "tutorial");
        redis.hmset(new RedisKey(TENANT, ENTITY, HASH_DOC_ID), doc);

        CreateArgs<RedisKey> jsonCreate = CreateArgs.<RedisKey> builder().on(CreateArgs.TargetType.JSON).build();
        FieldArgs<RedisKey> jsonTitle = TextFieldArgs.<RedisKey> builder().name(field("$.title")).as("title").sortable()
                .build();
        FieldArgs<RedisKey> jsonBody = TextFieldArgs.<RedisKey> builder().name(field("$.body")).as("body").build();
        FieldArgs<RedisKey> jsonCategory = TagFieldArgs.<RedisKey> builder().name(field("$.category")).as("category").build();
        redis.ftCreate(JSON_INDEX, jsonCreate, Arrays.asList(jsonTitle, jsonBody, jsonCategory));

        String jsonDoc = "{\"title\":\"Redis search guide\",\"body\":\"" + BODY_TEXT + "\",\"category\":\"tutorial\"}";
        redis.jsonSet(field(JSON_DOC_ID), JsonPath.ROOT_PATH, jsonDoc);
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
     * Build a "bare" {@link RedisKey} that round-trips through {@link RedisKeyCodec} as its {@code id} bytes only, with no
     * tenant/entity prefix. Used for hash field names and result-map keys so they retain their literal byte shape.
     */
    private static RedisKey field(String name) {
        return new RedisKey("", "", name);
    }

    /**
     * Sanity check. The query string is written raw via {@code args.add(query)} so this must work even when a structured-key
     * codec is installed on the connection.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void baselineSearchWorksThroughCodec(String indexName) {
        SearchReply<RedisKey, String> result = redis.ftSearch(indexName, "search");
        assertThat(result.getCount()).isEqualTo(1L);
    }

    /**
     * {@code INFIELDS} names schema fields declared at {@code FT.CREATE} time — they must be written raw, regardless of the
     * connection's key type.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void inFieldMustNotBeMangledByCodec(String indexName) {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder().inField(field("title")).build();

        SearchReply<RedisKey, String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).as("INFIELDS schema field must be sent raw, not codec-encoded").isEqualTo(1L);
    }

    /**
     * {@code RETURN} field names are schema identifiers and must appear verbatim in the result map.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void returnFieldMustNotBeMangledByCodec(String indexName) {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder().returnField(field("title")).build();

        SearchReply<RedisKey, String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields())
                .as("RETURN schema field must appear verbatim as a key in the result map").containsKey(field("title"));
    }

    /**
     * {@code RETURN ... AS alias} — the alias is also a schema-level identifier and must not be codec-encoded.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void returnFieldAliasMustNotBeMangledByCodec(String indexName) {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder().returnField(field("title"), "t").build();

        SearchReply<RedisKey, String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields()).as("RETURN alias must appear verbatim as a key in the result map")
                .containsKey(field("t"));
    }

    /**
     * {@code SUMMARIZE FIELDS} names schema fields. When applied correctly Redis abbreviates the field content and terminates
     * it with the configured separator (default {@code ...}).
     */
    @Test
    void summarizeFieldMustNotBeMangledByCodec() {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder().summarizeField(field("body")).build();

        SearchReply<RedisKey, String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        String body = result.getResults().get(0).getFields().get(field("body"));
        assertThat(body).as("SUMMARIZE must be applied to the 'body' field").contains("...");
    }

    /**
     * {@code HIGHLIGHT FIELDS} names schema fields. When applied, matching terms are wrapped with the configured tags.
     */
    @Test
    void highlightFieldMustNotBeMangledByCodec() {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder().highlightField(field("body"))
                .highlightTags("<b>", "</b>").build();

        SearchReply<RedisKey, String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        String body = result.getResults().get(0).getFields().get(field("body"));
        assertThat(body).as("HIGHLIGHT must wrap 'search' occurrences with the configured tags").contains("<b>search</b>");
    }

    /**
     * {@code FT.AGGREGATE ... LOAD} names schema fields. Same protocol clause as {@code PostProcessingArgs.load} — must be sent
     * verbatim.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void aggregateLoadFieldMustNotBeMangledByCodec(String indexName) {
        AggregateArgs<RedisKey, String> args = AggregateArgs.<RedisKey, String> builder().load(field("title")).build();

        AggregationReply<RedisKey, String> result = redis.ftAggregate(indexName, "*", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<RedisKey, String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).isNotEmpty();
        assertThat(reply.getResults().get(0).getFields()).as("LOAD schema field must be fetched verbatim")
                .containsKey(field("title"));
    }

    /**
     * {@code SORTBY} names a {@code SORTABLE} schema field. {@link SortByArgs} routes the attribute through {@code addKey}; a
     * bare {@link RedisKey} round-trips as plain {@code "title"} bytes and matches the schema attribute on both target types.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void sortByMustNotBeMangledByCodec(String indexName) {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder()
                .sortBy(SortByArgs.<RedisKey> builder().attribute(field("title")).build()).build();

        SearchReply<RedisKey, String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).as("SORTBY schema field must be sent raw, not codec-encoded").isEqualTo(1L);
    }

    /**
     * {@code PARAMS} substitution names are referenced from the query string as {@code $name}. The query is sent verbatim, but
     * {@link SearchArgs.Builder#param} routes the parameter name through {@code addKey}; with a bare {@link RedisKey} it
     * round-trips as plain {@code "term"} bytes so the server can resolve {@code $term} in the query.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void searchParamNameMustNotBeMangledByCodec(String indexName) {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder().param("term", "search").build();

        SearchReply<RedisKey, String> result = redis.ftSearch(indexName, "@body:$term", args);

        assertThat(result.getCount()).as("PARAMS substitution name must be sent raw so $term resolves on the server side")
                .isEqualTo(1L);
    }

    /**
     * Aggregate variant of {@link #searchParamNameMustNotBeMangledByCodec}. {@link AggregateArgs.Builder#param} is built via
     * the same {@code addKey} routing and a bare {@link RedisKey} round-trips as plain bytes.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void aggregateParamNameMustNotBeMangledByCodec(String indexName) {
        AggregateArgs<RedisKey, String> args = AggregateArgs.<RedisKey, String> builder().param("term", "search").build();

        AggregationReply<RedisKey, String> result = redis.ftAggregate(indexName, "@body:$term", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<RedisKey, String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).as("PARAMS substitution name must resolve on the server side").isNotEmpty();
    }

    /**
     * {@code INKEYS} restricts the search to a list of actual document keys. Unlike the schema-identifier clauses above, this
     * one is legitimately {@code K}-typed: the codec must encode the {@link RedisKey} to {@code "tenant1:doc:<id>"} so the
     * entry matches what was written by {@code HMSET}. Positive control for the codec routing on the legitimate key surface.
     */
    @Test
    void inKeyMustBeRoutedThroughCodec() {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder()
                .inKey(new RedisKey(TENANT, ENTITY, HASH_DOC_ID)).build();

        SearchReply<RedisKey, String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).as("INKEYS must route the document key through encodeKey to match the stored key")
                .isEqualTo(1L);
    }

    /**
     * Codec-routing witness. The other tests in this class pass schema identifiers as <em>bare</em> {@link RedisKey} instances
     * so {@link RedisKeyCodec} encodes them to plain {@code id} bytes on both the {@code FT.CREATE} side (where
     * {@link FieldArgs} routes the field name through {@code addKey}) and the read side. That symmetry keeps the round-trip
     * lossless but it also makes the positive assertions insensitive to whether the read side actually invokes the codec — bare
     * keys would round-trip correctly even if {@code inField} were sent raw. Here we deliberately pass a <em>non-bare</em>
     * {@link RedisKey} into {@link SearchArgs.Builder#inField} so the codec produces {@code "tenant1:doc:title"} instead of
     * {@code "title"}. The schema field is still {@code "title"} (registered via a bare key in {@link #prepare()}), so the
     * server treats the {@code INFIELDS} entry as an unknown field, restricts the query to a non-existent field and yields zero
     * hits. Asserting {@code count == 0} (vs. the baseline count of 1 with no {@code INFIELDS}) proves {@code inField} is
     * routed through {@code encodeKey}; if the codec were bypassed for a non-bare {@link RedisKey}, the wire bytes would be
     * {@code "title"} and the query would find the document.
     */
    @Test
    void inFieldWithNonBareKeyDemonstratesCodecRouting() {
        SearchArgs<RedisKey, String> args = SearchArgs.<RedisKey, String> builder()
                .inField(new RedisKey(TENANT, ENTITY, "title")).build();

        SearchReply<RedisKey, String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount())
                .as("inField routed through encodeKey must mangle a non-bare RedisKey into an unknown field name (0 hits)")
                .isEqualTo(0L);
    }

    /**
     * Value object used as the key type by {@link RedisKeyCodec}. Carries a tenant, an entity type and an id so a single Redis
     * database can be partitioned along more than one dimension. A "bare" key — empty {@code tenant} and {@code entity} —
     * round-trips identically to its {@code id} bytes; this lets hash field names like {@code "title"} retain their literal
     * shape and stay compatible with the FT.CREATE schema.
     */
    static class RedisKey {

        private final String tenant;

        private final String entity;

        private final String id;

        RedisKey(String tenant, String entity, String id) {
            this.tenant = tenant;
            this.entity = entity;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof RedisKey)) {
                return false;
            }
            RedisKey k = (RedisKey) o;
            return tenant.equals(k.tenant) && entity.equals(k.entity) && id.equals(k.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenant, entity, id);
        }

        @Override
        public String toString() {
            return tenant.isEmpty() && entity.isEmpty() ? id : tenant + ":" + entity + ":" + id;
        }

    }

    /**
     * Encodes a {@link RedisKey} as {@code tenant:entity:id} (UTF-8) and parses the same shape back on decode. Bare keys (empty
     * tenant and entity) round-trip as plain id bytes so that hash field names and other identity-shaped keys are not disturbed
     * by the tenant/entity prefix.
     */
    static class RedisKeyCodec implements RedisCodec<RedisKey, String> {

        @Override
        public RedisKey decodeKey(ByteBuffer bytes) {
            String s = StandardCharsets.UTF_8.decode(bytes).toString();
            String[] parts = s.split(":", 3);
            if (parts.length < 3) {
                return new RedisKey("", "", s);
            }
            return new RedisKey(parts[0], parts[1], parts[2]);
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }

        @Override
        public ByteBuffer encodeKey(RedisKey key) {
            return StandardCharsets.UTF_8.encode(key.toString());
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            return StandardCharsets.UTF_8.encode(value);
        }

    }

}
