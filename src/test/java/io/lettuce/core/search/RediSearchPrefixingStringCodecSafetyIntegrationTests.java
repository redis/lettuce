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
 * bypassing the value codec entirely. With a prefixing key codec the stored hash field names become {@code tenant1:<field>}.
 * Schema field names and the read-side field clauses ({@code INFIELDS}, {@code RETURN}, {@code SORTBY}, {@code SUMMARIZE},
 * {@code HIGHLIGHT}) are all sent raw, so each test reproduces the prefix on every field reference to match the codec-encoded
 * stored fields. Index prefixes are also sent raw, so callers must supply the physical prefix when a codec transforms key
 * bytes. {@code INKEYS} is the one legitimately {@code K}-typed surface and must route through {@code encodeKey}.
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
        // FieldArgs.name(String) is now sent raw (no codec). The hash field names in the document below are written
        // through hmset, which routes each map key via addKey and stores them codec-encoded as "tenant1:<field>". To make
        // the schema match the stored fields, the caller has to reproduce the codec's encodeKey transformation on the
        // schema name manually (encodedFieldRef -> "tenant1:title", etc.).
        FieldArgs hashTitle = TextFieldArgs.builder().name(encodedFieldRef("title")).sortable().build();
        FieldArgs hashBody = TextFieldArgs.builder().name(encodedFieldRef("body")).build();
        FieldArgs hashCategory = TagFieldArgs.builder().name(encodedFieldRef("category")).build();
        FieldArgs hashPrice = NumericFieldArgs.builder().name(encodedFieldRef("price")).sortable().build();
        redis.ftCreate(HASH_INDEX, hashCreate, Arrays.asList(hashTitle, hashBody, hashCategory, hashPrice));

        Map<String, String> doc = new HashMap<>();
        doc.put("title", "Redis search guide");
        doc.put("body", BODY_TEXT);
        doc.put("category", "tutorial");
        doc.put("price", "50");
        redis.hmset(HASH_DOC_KEY, doc);

        CreateArgs jsonCreate = CreateArgs.builder().on(CreateArgs.TargetType.JSON).build();
        // For JSON the field name is a JSONPath sent raw (a codec prefix would corrupt the path, e.g. "tenant1:$.title").
        // JSON.SET sends its payload verbatim, so the document fields are NOT codec-encoded the way hmset field names are.
        // The schema identifier that queries resolve against is the alias (AS), sent raw. Read-side clauses are also raw and
        // pre-encoded by the caller, so the alias is encoded to match. Net effect: both HASH and JSON expose their fields as
        // "tenant1:<field>" to the query layer.
        FieldArgs jsonTitle = TextFieldArgs.builder().name("$.title").as(encodedFieldRef("title")).sortable().build();
        FieldArgs jsonBody = TextFieldArgs.builder().name("$.body").as(encodedFieldRef("body")).build();
        FieldArgs jsonCategory = TagFieldArgs.builder().name("$.category").as(encodedFieldRef("category")).build();
        FieldArgs jsonPrice = NumericFieldArgs.builder().name("$.price").as(encodedFieldRef("price")).sortable().build();
        redis.ftCreate(JSON_INDEX, jsonCreate, Arrays.asList(jsonTitle, jsonBody, jsonCategory, jsonPrice));

        String jsonDoc = "{\"title\":\"Redis search guide\",\"body\":\"" + BODY_TEXT + "\",\"category\":\"tutorial\","
                + "\"price\":50}";
        redis.jsonSet(JSON_DOC_KEY, JsonPath.ROOT_PATH, redis.getJsonParser().createJsonValue(jsonDoc));
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
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void baselineSearchWorksThroughCodec(String indexName) {
        SearchReply<String> result = redis.ftSearch(indexName, "search");
        assertThat(result.getCount()).isEqualTo(1L);
    }

    /**
     * Field-scoped query syntax ({@code @field:value}) is sent verbatim via {@code args.add(query)}. The schema field is stored
     * under its encoded name {@code tenant1:title}, so the query must reference that encoded name (with {@code ':'} escaped) to
     * resolve.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void fieldScopedQueryMustResolveAgainstSchemaThroughCodec(String indexName) {
        // Manual workaround: the user has to (a) know the codec exists, (b) reproduce its prefix, and (c) escape any
        // RediSearch-reserved characters that the prefix introduces (here, ':' must be escaped as '\:' inside the field
        // reference because ':' is the field/value separator). On the wire this becomes "@tenant1\:title:Redis", which the
        // server unescapes to look up the schema field "tenant1:title".
        String query = "@" + escapedFieldRef("title") + ":Redis";

        SearchReply<String> result = redis.ftSearch(indexName, query);

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
     * Numeric range query ({@code @price:[40 60]}) is sent verbatim. The schema field is stored as {@code tenant1:price}, so
     * the query must reference the encoded name (with {@code ':'} escaped) to resolve. NUMERIC variant of
     * {@link #fieldScopedQueryMustResolveAgainstSchemaThroughCodec}.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void numericRangeQueryMustResolveAgainstSchemaThroughCodec(String indexName) {
        // Manual workaround: schema field "price" is codec-encoded to "tenant1:price"; the ':' must be escaped inside the
        // @field reference so the RediSearch query parser resolves the encoded schema name.
        String query = "@" + escapedFieldRef("price") + ":[40 60]";

        SearchReply<String> result = redis.ftSearch(indexName, query);

        assertThat(result.getCount()).as("@price:[40 60] must resolve against the schema field name on the server side")
                .isEqualTo(1L);
    }

    /**
     * Tag query ({@code @category:{tutorial}}) is sent verbatim. The schema field is stored as {@code tenant1:category}, so the
     * query must reference the encoded name (with {@code ':'} escaped) to resolve. TAG variant of
     * {@link #fieldScopedQueryMustResolveAgainstSchemaThroughCodec}.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void tagQueryMustResolveAgainstSchemaThroughCodec(String indexName) {
        // Manual workaround: schema field "category" is codec-encoded to "tenant1:category"; ':' must be escaped inside the
        // @field reference so the RediSearch query parser resolves the encoded schema name.
        String query = "@" + escapedFieldRef("category") + ":{tutorial}";

        SearchReply<String> result = redis.ftSearch(indexName, query);

        assertThat(result.getCount()).as("@category:{tutorial} must resolve against the schema field name on the server side")
                .isEqualTo(1L);
    }

    /**
     * Aggregate {@code GROUPBY} renders properties via {@code args.add("@" + property)} (raw, no codec). The schema field is
     * stored as {@code tenant1:category}, so the property must be pre-encoded to match.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void aggregateGroupByMustResolveAgainstSchemaThroughCodec(String indexName) {
        // Manual workaround: GroupBy.build emits "@" + property as a standalone command argument (not embedded in an
        // expression with delimiters), so we pre-encode the property to "tenant1:category" without escaping the ':'.
        AggregateArgs args = AggregateArgs.builder()
                .groupBy(AggregateArgs.GroupBy.of(encodedFieldRef("category")).reduce(AggregateArgs.Reducer.count().as("cnt")))
                .build();

        AggregationReply<String> result = redis.ftAggregate(indexName, "*", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).as("GROUPBY @category must resolve against the schema field on the server side")
                .isNotEmpty();
        assertThat(reply.getResults().get(0).getFields()).containsKey("cnt");
    }

    /**
     * {@code FT.CREATE ... FILTER <expression>} is sent raw via {@code args.add(filter)} and evaluated at indexing time, so the
     * filter must reference the schema field by its encoded name ({@code tenant1:price}, {@code ':'} escaped). The predicate
     * ({@code @price>1000}) must drop the seeded {@code price=50} document, yielding count {@code 0}.
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
        CreateArgs create = CreateArgs.builder().on(CreateArgs.TargetType.HASH).withPrefix(CODEC_PREFIX + "filtered:")
                .filter("@" + escapedFieldRef("price") + ">1000").build();
        FieldArgs titleField = TextFieldArgs.builder().name("title").build();
        FieldArgs priceField = NumericFieldArgs.builder().name("price").build();
        redis.ftCreate(filteredIndex, create, Arrays.asList(titleField, priceField));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SearchReply<String> result = redis.ftSearch(filteredIndex, "*");

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
        SearchArgs<String> args = SearchArgs.<String> builder().noContent().build();

        SearchReply<String> result = redis.ftSearch(HASH_INDEX, "search", args);

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
     * {@code INFIELDS} names a schema field and is sent raw, so the caller passes the encoded name ({@code tenant1:title}) to
     * match the codec-encoded schema field.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void inFieldMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String> args = SearchArgs.<String> builder().inField(encodedFieldRef("title")).build();

        SearchReply<String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).as("INFIELDS must resolve against the encoded schema field").isEqualTo(1L);
    }

    /**
     * {@code RETURN} names a schema field, sent raw; the caller passes the encoded name ({@code tenant1:title}) to match the
     * schema. Result-map field names are returned raw too, so the key appears in its encoded form.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void returnFieldMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String> args = SearchArgs.<String> builder().returnField(encodedFieldRef("title")).build();

        SearchReply<String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields()).as("RETURN schema field must appear as a key in the result map")
                .containsKey(encodedFieldRef("title"));
    }

    /**
     * {@code RETURN ... AS alias} — the field is the encoded schema name; the alias is a raw logical identifier and appears
     * verbatim in the result map.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void returnFieldAliasMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String> args = SearchArgs.<String> builder().returnField(encodedFieldRef("title"), "t").build();

        SearchReply<String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        assertThat(result.getResults().get(0).getFields()).as("RETURN alias must appear verbatim as a key in the result map")
                .containsKey("t");
    }

    /**
     * {@code SUMMARIZE FIELDS} names a schema field, sent raw; the caller passes the encoded name ({@code tenant1:body}) to
     * match the schema. Redis then abbreviates the content and terminates it with the separator (default {@code ...}).
     */
    // HASH only: SUMMARIZE/HIGHLIGHT are not supported on JSON indexes (server rejects with SEARCH_QUERY_BAD), so
    // JSON_INDEX cannot be added to the @ValueSource here.
    @Test
    void summarizeFieldMustNotBeMangledByCodec() {
        SearchArgs<String> args = SearchArgs.<String> builder().summarizeField(encodedFieldRef("body")).build();

        SearchReply<String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        String body = result.getResults().get(0).getFields().get(encodedFieldRef("body"));
        assertThat(body).as("SUMMARIZE must be applied to the 'body' field").contains("...");
    }

    /**
     * {@code HIGHLIGHT FIELDS} names a schema field, sent raw; the caller passes the encoded name ({@code tenant1:body}) to
     * match the schema. Matching terms are then wrapped with the configured tags.
     */
    // HASH only: SUMMARIZE/HIGHLIGHT are not supported on JSON indexes (server rejects with SEARCH_QUERY_BAD), so
    // JSON_INDEX cannot be added to the @ValueSource here.
    @Test
    void highlightFieldMustNotBeMangledByCodec() {
        SearchArgs<String> args = SearchArgs.<String> builder().highlightField(encodedFieldRef("body"))
                .highlightTags("<b>", "</b>").build();

        SearchReply<String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).isEqualTo(1L);
        String body = result.getResults().get(0).getFields().get(encodedFieldRef("body"));
        assertThat(body).as("HIGHLIGHT must wrap 'search' occurrences with the configured tags").contains("<b>search</b>");
    }

    /**
     * {@code FT.AGGREGATE ... LOAD} names a schema field, sent raw; the caller passes the encoded name ({@code tenant1:title})
     * to match the schema, and the result-map key comes back in the same encoded form.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void aggregateLoadFieldMustNotBeMangledByCodec(String indexName) {
        AggregateArgs args = AggregateArgs.builder().load(encodedFieldRef("title")).build();

        AggregationReply<String> result = redis.ftAggregate(indexName, "*", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).isNotEmpty();
        assertThat(reply.getResults().get(0).getFields()).as("LOAD schema field must be fetched verbatim")
                .containsKey(encodedFieldRef("title"));
    }

    /**
     * {@code SORTBY} names a {@code SORTABLE} schema attribute, sent raw; the caller passes the encoded name
     * ({@code tenant1:title}) to match the schema.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void sortByMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String> args = SearchArgs.<String> builder()
                .sortBy(SortByArgs.builder().attribute(encodedFieldRef("title")).build()).build();

        SearchReply<String> result = redis.ftSearch(indexName, "search", args);

        assertThat(result.getCount()).as("SORTBY schema field must be sent raw, not codec-encoded").isEqualTo(1L);
    }

    /**
     * {@code PARAMS} substitution names are referenced as {@code $name} from the verbatim query.
     * {@link SearchArgs.Builder#param} sends the name raw, so {@code $term} resolves on the server. Bare {@code $term} (no
     * {@code @field:}) isolates this from schema-field-name routing.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void searchParamNameMustNotBeMangledByCodec(String indexName) {
        SearchArgs<String> args = SearchArgs.<String> builder().param("term", "search").build();

        SearchReply<String> result = redis.ftSearch(indexName, "$term", args);

        assertThat(result.getCount()).as("PARAMS substitution name must be sent raw so $term resolves on the server side")
                .isEqualTo(1L);
    }

    /**
     * Aggregate variant of {@link #searchParamNameMustNotBeMangledByCodec}; {@link AggregateArgs.Builder#param} sends the name
     * raw too.
     */
    @ParameterizedTest
    @ValueSource(strings = { HASH_INDEX, JSON_INDEX })
    void aggregateParamNameMustNotBeMangledByCodec(String indexName) {
        AggregateArgs args = AggregateArgs.builder().param("term", "search").build();

        AggregationReply<String> result = redis.ftAggregate(indexName, "$term", args);

        assertThat(result.getReplies()).isNotEmpty();
        SearchReply<String> reply = result.getReplies().get(0);
        assertThat(reply.getResults()).as("PARAMS substitution name must resolve on the server side").isNotEmpty();
    }

    /**
     * {@code INKEYS} restricts the search to a list of actual document keys. Unlike the schema-identifier clauses above, this
     * one is legitimately {@code K}-typed and {@link io.lettuce.core.protocol.CommandArgs#addKeys} must route through
     * {@code encodeKey} so the prefix matches what was applied at write-time. Acts as a positive control: if the key channel
     * stops routing through the codec, this assertion stops finding the document.
     */
    @Test
    void inKeyMustBeRoutedThroughCodecOnHash() {
        SearchArgs<String> args = SearchArgs.<String> builder().inKey(HASH_DOC_KEY).build();

        SearchReply<String> result = redis.ftSearch(HASH_INDEX, "search", args);

        assertThat(result.getCount()).as("INKEYS must route the document key through encodeKey to match the stored prefix")
                .isEqualTo(1L);
    }

    /**
     * JSON variant of {@link #inKeyMustBeRoutedThroughCodecOnHash}. The JSON document is stored via {@code jsonSet} under a
     * codec-encoded key too, so {@code INKEYS} must route through {@code encodeKey} to match it.
     */
    @Test
    void inKeyMustBeRoutedThroughCodecOnJson() {
        SearchArgs<String> args = SearchArgs.<String> builder().inKey(JSON_DOC_KEY).build();

        SearchReply<String> result = redis.ftSearch(JSON_INDEX, "search", args);

        assertThat(result.getCount()).as("INKEYS must route the document key through encodeKey to match the stored prefix")
                .isEqualTo(1L);
    }

    /**
     * {@code FT.CREATE ... PREFIX 1 tenant1:doc:} restricts the index to keys whose Redis key starts with the given prefix.
     * Prefixes are sent as raw strings, so the caller supplies the physical prefix needed to match the stored HASH key
     * {@code "tenant1:doc:1"}. If the prefix were routed through {@code encodeKey}, it would become
     * {@code "tenant1:tenant1:doc:"} and the document would not be indexed.
     */
    @Test
    void prefixInCreateArgsMustBeSentRawForHashKeys() {
        redis.flushall();

        CreateArgs prefixCreate = CreateArgs.builder().on(CreateArgs.TargetType.HASH).withPrefix(CODEC_PREFIX + "doc:").build();
        FieldArgs titleField = TextFieldArgs.builder().name(encodedFieldRef("title")).build();
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

        SearchReply<String> result = redis.ftSearch("prefix-test-idx", "search");

        assertThat(result.getCount()).as("PREFIX must be sent raw to match the stored HASH key without double-prefixing")
                .isEqualTo(1L);
    }

    /**
     * JSON variant of {@link #prefixInCreateArgsMustBeSentRawForHashKeys}. {@code JSON.SET} stores the document under a
     * codec-encoded key too, so the caller supplies the matching physical prefix.
     */
    @Test
    void prefixInCreateArgsMustBeSentRawForJsonKeys() {
        redis.flushall();

        CreateArgs prefixCreate = CreateArgs.builder().on(CreateArgs.TargetType.JSON).withPrefix(CODEC_PREFIX + "doc:").build();
        FieldArgs titleField = TextFieldArgs.builder().name("$.title").as(encodedFieldRef("title")).build();
        redis.ftCreate("prefix-test-json-idx", prefixCreate, Arrays.asList(titleField));

        redis.jsonSet("doc:1", JsonPath.ROOT_PATH, redis.getJsonParser().createJsonValue("{\"title\":\"Redis search guide\"}"));

        // Wait for indexing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        SearchReply<String> result = redis.ftSearch("prefix-test-json-idx", "search");

        assertThat(result.getCount()).as("PREFIX must be sent raw to match the stored JSON key without double-prefixing")
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
