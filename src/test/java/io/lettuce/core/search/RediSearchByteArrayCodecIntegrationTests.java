/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.search.arguments.CreateArgs;
import io.lettuce.core.search.arguments.FieldArgs;
import io.lettuce.core.search.arguments.NumericFieldArgs;
import io.lettuce.core.search.arguments.SearchArgs;
import io.lettuce.core.search.arguments.TagFieldArgs;
import io.lettuce.core.search.arguments.TextFieldArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis Search functionality using {@link ByteArrayCodec}.
 * <p>
 * These tests are based on the examples from the Redis documentation: -
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/schema-definition/">...</a> -
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/">...</a>
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchByteArrayCodecIntegrationTests {

    // Index names
    private static final String BLOG_INDEX = "blog-idx";

    // Prefixes
    private static final byte[] BLOG_PREFIX = "blog:post:".getBytes();

    protected static RedisClient client;

    protected static RedisCommands<byte[], byte[]> redis;

    public RediSearchByteArrayCodecIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        redis = client.connect(ByteArrayCodec.INSTANCE).sync();
    }

    protected ClientOptions getOptions() {
        return ClientOptions.builder().build();
    }

    @BeforeEach
    public void prepare() {
        redis.flushall();
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    /**
     * Test basic text search functionality based on the blog post example from Redis documentation. Creates an index with TEXT,
     * NUMERIC, and TAG fields and performs various search operations.
     */
    @Test
    void testBasicTextSearchWithBlogPosts() {
        // Create index based on Redis documentation example:
        // FT.CREATE idx ON HASH PREFIX 1 blog:post: SCHEMA title TEXT WEIGHT 5.0 content TEXT author TAG created_date NUMERIC
        // SORTABLE views NUMERIC
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).weight(5).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();
        FieldArgs<byte[]> authorField = TagFieldArgs.<byte[]> builder().name("author".getBytes()).build();
        FieldArgs<byte[]> createdDateField = NumericFieldArgs.<byte[]> builder().name("created_date".getBytes()).sortable()
                .build();
        FieldArgs<byte[]> viewsField = NumericFieldArgs.<byte[]> builder().name("views".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        String result = redis.ftCreate(BLOG_INDEX, createArgs,
                Arrays.asList(titleField, contentField, authorField, createdDateField, viewsField));
        assertThat(result).isEqualTo("OK");

        // Add sample blog posts
        Map<byte[], byte[]> post1 = new HashMap<>();
        post1.put("title".getBytes(), "Redis Search Tutorial".getBytes());
        post1.put("content".getBytes(), "Learn how to use Redis Search for full-text search capabilities".getBytes());
        post1.put("author".getBytes(), "john_doe".getBytes());
        post1.put("created_date".getBytes(), "1640995200".getBytes());
        post1.put("views".getBytes(), "150".getBytes());
        assertThat(redis.hmset("blog:post:1".getBytes(), post1)).isEqualTo("OK");

        Map<byte[], byte[]> post2 = new HashMap<>();
        post2.put("title".getBytes(), "Advanced Redis Techniques".getBytes());
        post2.put("content".getBytes(), "Explore advanced Redis features and optimization techniques".getBytes());
        post2.put("author".getBytes(), "jane_smith".getBytes());
        post2.put("created_date".getBytes(), "1641081600".getBytes());
        post2.put("views".getBytes(), "200".getBytes());
        assertThat(redis.hmset("blog:post:2".getBytes(), post2)).isEqualTo("OK");

        Map<byte[], byte[]> post3 = new HashMap<>();
        post3.put("title".getBytes(), "Database Performance".getBytes());
        post3.put("content".getBytes(), "Tips for improving database performance and scalability".getBytes());
        post3.put("author".getBytes(), "john_doe".getBytes());
        post3.put("created_date".getBytes(), "1641168000".getBytes());
        post3.put("views".getBytes(), "75".getBytes());
        assertThat(redis.hmset("blog:post:3".getBytes(), post3)).isEqualTo("OK");

        // Test 1: Basic text search
        SearchReply<byte[], byte[]> searchReply = redis.ftSearch(BLOG_INDEX, "@title:(Redis)".getBytes());
        assertThat(searchReply.getCount()).isEqualTo(2);
        assertThat(searchReply.getResults()).hasSize(2);
        assertThat(new String(getField(searchReply.getResults().get(1).getFields(), "title")))
                .isEqualTo("Redis Search Tutorial");
        assertThat(new String(getField(searchReply.getResults().get(0).getFields(), "title")))
                .isEqualTo("Advanced Redis Techniques");
        assertThat(new String(getField(searchReply.getResults().get(1).getFields(), "author"))).isEqualTo("john_doe");
        assertThat(new String(getField(searchReply.getResults().get(0).getFields(), "author"))).isEqualTo("jane_smith");

        // Test 2: Search with field-specific query
        SearchArgs<byte[], byte[]> titleSearchArgs = SearchArgs.<byte[], byte[]> builder().build();
        searchReply = redis.ftSearch(BLOG_INDEX, "@title:Redis".getBytes(), titleSearchArgs);
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Test 3: Tag search
        searchReply = redis.ftSearch(BLOG_INDEX, "@author:{john_doe}".getBytes());
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Test 4: Numeric range search
        searchReply = redis.ftSearch(BLOG_INDEX, "@views:[100 300]".getBytes());
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Cleanup
        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test search options like INFIELDS, RETURN, LIMIT.
     */
    @Test
    void testSearchArgsWithByteArrayParams() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();
        FieldArgs<byte[]> authorField = TagFieldArgs.<byte[]> builder().name("author".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField, contentField, authorField));

        // Add documents
        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("title".getBytes(), "Redis Guide".getBytes());
        doc1.put("content".getBytes(), "Complete Redis guide".getBytes());
        doc1.put("author".getBytes(), "alice".getBytes());
        redis.hmset("blog:post:1".getBytes(), doc1);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("title".getBytes(), "Database Book".getBytes());
        doc2.put("content".getBytes(), "Redis and databases".getBytes());
        doc2.put("author".getBytes(), "bob".getBytes());
        redis.hmset("blog:post:2".getBytes(), doc2);

        // Test inField - search only in title field
        SearchArgs<byte[], byte[]> inFieldArgs = SearchArgs.<byte[], byte[]> builder().inField("title".getBytes()).build();
        SearchReply<byte[], byte[]> inFieldResults = redis.ftSearch(BLOG_INDEX, "Redis".getBytes(), inFieldArgs);
        assertThat(inFieldResults.getCount()).isEqualTo(1); // Only doc1 has "Redis" in title

        // Test returnField - return only specific fields
        SearchArgs<byte[], byte[]> returnFieldArgs = SearchArgs.<byte[], byte[]> builder().returnField("title".getBytes())
                .returnField("author".getBytes()).build();
        SearchReply<byte[], byte[]> returnResults = redis.ftSearch(BLOG_INDEX, "*".getBytes(), returnFieldArgs);
        assertThat(returnResults.getCount()).isEqualTo(2);
        // Content field should not be returned
        Map<byte[], byte[]> fields = returnResults.getResults().get(0).getFields();
        assertThat(getField(fields, "content")).isNull();
        assertThat(getField(fields, "title")).isNotNull();

        // Test limit
        SearchArgs<byte[], byte[]> limitArgs = SearchArgs.<byte[], byte[]> builder().limit(0, 1).build();
        SearchReply<byte[], byte[]> limitResults = redis.ftSearch(BLOG_INDEX, "*".getBytes(), limitArgs);
        assertThat(limitResults.getCount()).isEqualTo(2); // Total count is still 2
        assertThat(limitResults.getResults()).hasSize(1); // But only 1 result returned

        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test search with PARAMS option using byte[] keys and values.
     */
    @Test
    void testSearchArgsWithParams() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> priceField = NumericFieldArgs.<byte[]> builder().name("price".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField, priceField));

        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("title".getBytes(), "Cheap Item".getBytes());
        doc1.put("price".getBytes(), "50".getBytes());
        redis.hmset("blog:post:1".getBytes(), doc1);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("title".getBytes(), "Expensive Item".getBytes());
        doc2.put("price".getBytes(), "500".getBytes());
        redis.hmset("blog:post:2".getBytes(), doc2);

        // Search using params with byte[] key and value
        SearchArgs<byte[], byte[]> paramsArgs = SearchArgs.<byte[], byte[]> builder()
                .param("maxprice".getBytes(), "100".getBytes()).build();
        SearchReply<byte[], byte[]> results = redis.ftSearch(BLOG_INDEX, "@price:[0 $maxprice]".getBytes(), paramsArgs);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "title"))).isEqualTo("Cheap Item");

        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test FT.DROPINDEX command.
     */
    @Test
    void testDropIndex() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField));

        String result = redis.ftDropindex(BLOG_INDEX);
        assertThat(result).isEqualTo("OK");
    }

    /**
     * Helper method to find a field value in a byte[] keyed map. Since arrays in Java use reference equality rather than value
     * equality, Map.get() won't work with byte[] keys. We need to iterate and compare using Arrays.equals().
     */
    private byte[] getField(Map<byte[], byte[]> fields, String fieldName) {
        byte[] key = fieldName.getBytes();
        return fields.entrySet().stream().filter(e -> Arrays.equals(e.getKey(), key)).map(Map.Entry::getValue).findFirst()
                .orElse(null);
    }

}
