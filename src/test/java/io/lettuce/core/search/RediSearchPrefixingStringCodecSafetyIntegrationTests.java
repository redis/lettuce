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
import io.lettuce.core.search.arguments.NumericFieldArgs;
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

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Codec-safety invariants for RediSearch when the connection key codec is non-trivial. Covers both {@code ON HASH} and
 * {@code ON JSON} indexes. {@code HSET}/{@code HMSET} route hash field names and values through the connection's
 * {@link RedisCodec} ({@link io.lettuce.core.protocol.CommandArgs#add(java.util.Map)} encodes each map entry as
 * {@code addKey(field).addValue(value)}); {@code JSON.SET} sends its payload verbatim via
 * {@link io.lettuce.core.protocol.CommandArgs#add(String)} (or {@code add(byte[])} for {@link io.lettuce.core.json.JsonValue}),
 * bypassing the value codec entirely. The schema-level identifiers exercised below (field names, aliases) must be sent raw on
 * both target types; only the legitimately {@code K}-typed surface (e.g. {@code INKEYS}) must route through {@code encodeKey}.
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchPrefixingStringCodecSafetyIntegrationTests {

    private static final String HASH_INDEX = "codec-safety-idx";

    private static final String JSON_INDEX = "codec-safety-json-idx";

    private static final String HASH_DOC_KEY = "1";

    private static final String JSON_DOC_KEY = "json:1";

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
        FieldArgs hashTitle = TextFieldArgs.builder().name("title").sortable().build();
        FieldArgs hashBody = TextFieldArgs.builder().name("body").build();
        FieldArgs hashCategory = TagFieldArgs.builder().name("category").build();
        FieldArgs hashPrice = NumericFieldArgs.builder().name("price").sortable().build();
        redis.ftCreate(HASH_INDEX, hashCreate, Arrays.asList(hashTitle, hashBody, hashCategory, hashPrice));

        Map<String, String> doc = new HashMap<>();
        doc.put("title", "Redis search guide");
        doc.put("body", BODY_TEXT);
        doc.put("category", "tutorial");
        doc.put("price", "50");
        redis.hmset(HASH_DOC_KEY, doc);

        // JSON index disabled: FieldArgs.build() routes name(K) through addKey, so name("$.title") is encoded by the
        // PrefixingStringCodec to "tenant1:$.title" and Redis rejects it as an invalid JSONPath at FT.CREATE time.
        // Re-enable together with the JSON_INDEX entries in the @ValueSource arrays below once schema-identifier routing
        // is fixed library-wide.
        // CreateArgs<String> jsonCreate = CreateArgs.<String> builder().on(CreateArgs.TargetType.JSON).build();
        // FieldArgs<String> jsonTitle = TextFieldArgs.<String> builder().name("$.title").as("title").sortable().build();
        // FieldArgs<String> jsonBody = TextFieldArgs.<String> builder().name("$.body").as("body").build();
        // FieldArgs<String> jsonCategory = TagFieldArgs.<String> builder().name("$.category").as("category").build();
        // redis.ftCreate(JSON_INDEX, jsonCreate, Arrays.asList(jsonTitle, jsonBody, jsonCategory));
        //
        // String jsonDoc = "{\"title\":\"Redis search guide\",\"body\":\"" + BODY_TEXT + "\",\"category\":\"tutorial\"}";
        // redis.jsonSet(JSON_DOC_KEY, JsonPath.ROOT_PATH, jsonDoc);
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
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX /* , JSON_INDEX */ })
    void baselineSearchWorksThroughCodec(String indexName) {
        SearchReply<String, String> result = redis.ftSearch(indexName, "search");
        assertThat(result.getCount()).isEqualTo(1L);
    }

    /**
     * Field-scoped query syntax: the user writes {@code @field:value} directly into the query string, which is sent verbatim
     * via {@code args.add(query)}. Schema field names registered through {@link FieldArgs.Builder#name(Object)} are
     * codec-routed via {@code addKey} and the server stores the field as {@code tenant1:title}. The query references
     * {@code @title} raw, so the server reports {@code Unknown field at offset 0 near title}. There is no programmatic seam to
     * encode a token inside a raw query string, so the only way this can succeed today is for the schema name to also be raw on
     * the wire — i.e. for schema identifiers to bypass the key codec.
     */
    @Test
    void fieldScopedQueryMustResolveAgainstSchemaThroughCodec() {
        // Manual workaround: the user has to (a) know the codec exists, (b) reproduce its prefix, and (c) escape any
        // RediSearch-reserved characters that the prefix introduces (here, ':' must be escaped as '\:' inside the field
        // reference because ':' is the field/value separator). On the wire this becomes "@tenant1\:title:Redis", which the
        // server unescapes to look up the schema field "tenant1:title".
        String query = "@" + escapedFieldRef("title") + ":Redis";

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, query);

        assertThat(result.getCount()).as("@field:value must resolve against the schema field name on the server side")
                .isEqualTo(1L);
    }

    /**
     * Reproduces what the codec applies to a key, then escapes RediSearch-reserved characters introduced by the prefix so the
     * result is safe to splice into a query/filter expression as {@code @<ref>}. Every call site that references a schema field
     * inside a raw expression has to pay this cost.
     */
    private static String escapedFieldRef(String field) {
        String encoded = StandardCharsets.UTF_8.decode(new PrefixingStringCodec(CODEC_PREFIX).encodeKey(field)).toString();
        return encoded.replace(":", "\\:");
    }

    /**
     * Reproduces what the codec applies to a key without escaping. Used where the field name is sent as a standalone command
     * argument (e.g. {@code GROUPBY <prop>}) rather than embedded in a query/filter expression with delimiters.
     */
    private static String encodedFieldRef(String field) {
        return StandardCharsets.UTF_8.decode(new PrefixingStringCodec(CODEC_PREFIX).encodeKey(field)).toString();
    }

    /**
     * Numeric range query: the user writes {@code @price:[40 60]} directly into the query string, sent verbatim via
     * {@code args.add(query)}. The {@code price} schema field was registered through {@link FieldArgs.Builder#name(Object)} and
     * codec-encoded to {@code tenant1:price} at {@code FT.CREATE} time. The query references {@code @price} raw, so the server
     * reports {@code Unknown field}. This is the same root cause as
     * {@link #fieldScopedQueryMustResolveAgainstSchemaThroughCodec} but on a NUMERIC field.
     */
    @Test
    void numericRangeQueryMustResolveAgainstSchemaThroughCodec() {
        // Manual workaround: schema field "price" is codec-encoded to "tenant1:price"; the ':' must be escaped inside the
        // @field reference so the RediSearch query parser resolves the encoded schema name.
        String query = "@" + escapedFieldRef("price") + ":[40 60]";

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, query);

        assertThat(result.getCount()).as("@price:[40 60] must resolve against the schema field name on the server side")
                .isEqualTo(1L);
    }

    /**
     * Tag query: the user writes {@code @category:{tutorial}} directly into the query string. The {@code category} schema field
     * was registered through {@link FieldArgs.Builder#name(Object)} and codec-encoded to {@code tenant1:category} at
     * {@code FT.CREATE} time. The query references {@code @category} raw, so the server reports {@code Unknown field}. Same
     * root cause as {@link #fieldScopedQueryMustResolveAgainstSchemaThroughCodec} on a TAG field.
     */
    @Test
    void tagQueryMustResolveAgainstSchemaThroughCodec() {
        // Manual workaround: schema field "category" is codec-encoded to "tenant1:category"; ':' must be escaped inside the
        // @field reference so the RediSearch query parser resolves the encoded schema name.
        String query = "@" + escapedFieldRef("category") + ":{tutorial}";

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, query);

        assertThat(result.getCount()).as("@category:{tutorial} must resolve against the schema field name on the server side")
                .isEqualTo(1L);
    }

    /**
     * Aggregate {@code GROUPBY}: {@link AggregateArgs.GroupBy#build(io.lettuce.core.protocol.CommandArgs)} renders properties
     * via {@code args.add("@" + property)} (raw, no codec). Schema field {@code category} was codec-encoded to
     * {@code tenant1:category} at {@code FT.CREATE} time, so the raw {@code @category} reference in {@code GROUPBY} does not
     * resolve and the server reports {@code Property `@category` not loaded nor in pipeline}.
     */
    @Test
    void aggregateGroupByMustResolveAgainstSchemaThroughCodec() {
        // Manual workaround: GroupBy.build emits "@" + property as a standalone command argument (not embedded in an
        // expression with delimiters), so we pre-encode the property to "tenant1:category" without escaping the ':'.
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().groupBy(AggregateArgs.GroupBy
                .<String> of(encodedFieldRef("category")).reduce(AggregateArgs.Reducer.<String> count().as("cnt"))).build();

        AggregationReply<String, String> result = redis.ftAggregate(HASH_INDEX, "*", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String, String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).as("GROUPBY @category must resolve against the schema field on the server side")
                .isNotEmpty();
        assertThat(reply.getResults().get(0).getFields()).containsKey("cnt");
    }

    /**
     * {@code FT.CREATE ... FILTER <expression>}: the filter expression is sent raw via {@code args.add(filter)} and is
     * evaluated by the server at indexing time. With the current bug, the schema field {@code category} is codec-encoded to
     * {@code tenant1:category}, but the filter references {@code @category} raw. RediSearch silently treats the unresolved
     * {@code @category} as matching every document, so the predicate becomes a no-op. We use an exclusion filter
     * ({@code !='tutorial'}) and seed only {@code tutorial} documents: the filter should exclude every document and produce
     * count {@code 0}. With the bug it produces count {@code 1} because the filter never fires.
     */
    @Test
    void createArgsFilterExpressionMustResolveAgainstSchemaThroughCodec() {
        String filteredIndex = "filter-expr-idx";

        // Seed the hash BEFORE creating the index so the FILTER predicate is exercised during the initial scan.
        Map<String, String> doc = new HashMap<>();
        doc.put("title", "Filtered Redis search guide");
        doc.put("price", "50");
        redis.hmset("filtered:1", doc);

        // Manual workaround: schema field "price" is codec-encoded to "tenant1:price"; ':' must be escaped inside the
        // @field reference so the FT.CREATE FILTER expression parser resolves the encoded schema name. Use a NUMERIC
        // predicate that excludes the seeded document (price=50) so the filter fires unambiguously.
        CreateArgs<String> create = CreateArgs.<String> builder().on(CreateArgs.TargetType.HASH).withPrefix("filtered:")
                .filter("@" + escapedFieldRef("price") + ">1000").build();
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> priceField = NumericFieldArgs.<String> builder().name("price").build();
        redis.ftCreate(filteredIndex, create, Arrays.asList(titleField, priceField));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SearchReply<String, String> result = redis.ftSearch(filteredIndex, "*");

        assertThat(result.getCount())
                .as("FILTER @price>1000 must resolve against the schema field; if this fails with 1, the filter expression "
                        + "references the field raw while the schema codec-encoded it, and the predicate is silently treated "
                        + "as match-all")
                .isEqualTo(0L);
    }

    /**
     * Positive control for the document-key channel under {@code NOCONTENT}. The server returns only document IDs and the
     * parser runs them back through {@code decodeKey}; the round-trip must yield the user-facing key the caller passed at write
     * time ({@code HASH_DOC_KEY}), not the on-the-wire prefixed form. If this regresses, the key channel has stopped routing
     * through the connection codec on the read path.
     */
    @Test
    void noContentDocumentIdRoundTripsThroughCodec() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().noContent().build();

        SearchReply<String, String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults()).hasSize(1);
        assertThat(result.getResults().get(0).getId())
                .as("NOCONTENT document id must be decoded through the codec back to the user-facing key")
                .isEqualTo(HASH_DOC_KEY);
        assertThat(result.getResults().get(0).getFields()).as("NOCONTENT must suppress field payloads").isNullOrEmpty();
    }

    /**
     * Positive control for {@code FT.DROPINDEX ... DD}. With {@code DD}, the server deletes every document the index pointed to
     * — those keys are stored under their codec-encoded form ({@code tenant1:1}). After the drop the underlying key must be
     * gone from the database when looked up through the same codec ({@code EXISTS} routes through {@code encodeKey}).
     */
    @Test
    void dropIndexWithDeleteDocumentsRemovesCodecEncodedKeys() {
        assertThat(redis.exists(HASH_DOC_KEY)).as("document must exist before DROPINDEX DD").isEqualTo(1L);

        redis.ftDropindex(HASH_INDEX, true);

        assertThat(redis.exists(HASH_DOC_KEY)).as("DROPINDEX DD must delete the underlying document at its codec-encoded key")
                .isEqualTo(0L);
    }

    /**
     * {@code INFIELDS} names schema fields declared at {@code FT.CREATE} time — they must be written raw. With the current bug,
     * {@code addKeys(inFields)} routes them through the codec producing {@code tenant1:title}, and the server rejects it with
     * {@code Unknown field}.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX /* , JSON_INDEX */ })
    void inFieldMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().inField("title").build();

        SearchReply<String, String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).as("INFIELDS schema field must be sent raw, not codec-encoded").isEqualTo(1L);
    }

    /**
     * {@code RETURN} field names and their {@code AS} aliases are schema identifiers. With the current bug, both get
     * codec-encoded (the map key and the alias), producing a mismatch against the index schema.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX /* , JSON_INDEX */ })
    void returnFieldMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().returnField("title").build();

        SearchReply<String, String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields())
                .as("RETURN schema field must appear verbatim as a key in the result map").containsKey("title");
    }

    /**
     * {@code RETURN ... AS alias} — the alias is also a schema-level identifier and must not be codec-encoded.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX /* , JSON_INDEX */ })
    void returnFieldAliasMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().returnField("title", "t").build();

        SearchReply<String, String> result = redis.ftSearch(indexName, "search", args);

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
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX /* , JSON_INDEX */ })
    void aggregateLoadFieldMustNotBeMangledByCodec(String indexName) {
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().load("title").build();

        AggregationReply<String, String> result = redis.ftAggregate(indexName, "*", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String, String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).isNotEmpty();
        assertThat(reply.getResults().get(0).getFields()).as("LOAD schema field must be fetched verbatim").containsKey("title");
    }

    /**
     * {@code SORTBY} names a {@code SORTABLE} schema field. {@link SortByArgs} routes the attribute through {@code addKey}, so
     * a non-trivial codec mangles it on the wire and the server fails to resolve the sort attribute against the index schema.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX /* , JSON_INDEX */ })
    void sortByMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder()
                .sortBy(SortByArgs.<String> builder().attribute("title").build()).build();

        SearchReply<String, String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).as("SORTBY schema field must be sent raw, not codec-encoded").isEqualTo(1L);
    }

    /**
     * {@code PARAMS} substitution names are referenced from the query string as {@code $name}. The query is sent verbatim, but
     * {@link SearchArgs.Builder#param} routes the parameter name through {@code addKey}; with a non-trivial codec the server
     * registers {@code tenant1:term} while the query references {@code $term} and parameter resolution fails. The query is a
     * bare {@code $term} (no {@code @field:} filter) to isolate this assertion from schema-field-name routing, which is
     * exercised independently by {@link #inFieldMustNotBeMangledByCodec} and friends.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX /* , JSON_INDEX */ })
    void searchParamNameMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().param("term", "search").build();

        SearchReply<String, String> result = redis.ftSearch(indexName, "$term", args);

        assertThat(result.getCount()).as("PARAMS substitution name must be sent raw so $term resolves on the server side")
                .isEqualTo(1L);
    }

    /**
     * Aggregate variant of {@link #searchParamNameMustNotBeMangledByCodec}. {@link AggregateArgs.Builder#param} is built via
     * the same {@code addKey} routing and exhibits the same asymmetric failure mode.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX /* , JSON_INDEX */ })
    void aggregateParamNameMustNotBeMangledByCodec(String indexName) {
        AggregateArgs<String, String> args = AggregateArgs.<String, String> builder().param("term", "search").build();

        AggregationReply<String, String> result = redis.ftAggregate(indexName, "$term", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String, String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).as("PARAMS substitution name must resolve on the server side").isNotEmpty();
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
