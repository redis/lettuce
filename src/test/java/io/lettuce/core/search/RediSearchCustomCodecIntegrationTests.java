/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.ByteBufferCodec;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis Search functionality using {@link ByteBufferCodec}.
 * <p>
 * These tests are based on the examples from the Redis documentation: -
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/schema-definition/">...</a> -
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/">...</a>
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchCustomCodecIntegrationTests {

    // Index names
    private static final String BLOG_INDEX = "blog-idx";

    // Prefixes
    private static final ByteBuffer BLOG_PREFIX = toBuffer("blog:post:");

    protected static RedisClient client;

    protected static RedisCommands<ByteBuffer, ByteBuffer> redis;

    public RediSearchCustomCodecIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        redis = client.connect(new ByteBufferCodec()).sync();
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

    private static ByteBuffer toBuffer(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String fromBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
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
        FieldArgs<ByteBuffer> titleField = TextFieldArgs.<ByteBuffer> builder().name(toBuffer("title")).weight(5).build();
        FieldArgs<ByteBuffer> contentField = TextFieldArgs.<ByteBuffer> builder().name(toBuffer("content")).build();
        FieldArgs<ByteBuffer> authorField = TagFieldArgs.<ByteBuffer> builder().name(toBuffer("author")).build();
        FieldArgs<ByteBuffer> createdDateField = NumericFieldArgs.<ByteBuffer> builder().name(toBuffer("created_date"))
                .sortable().build();
        FieldArgs<ByteBuffer> viewsField = NumericFieldArgs.<ByteBuffer> builder().name(toBuffer("views")).build();

        CreateArgs<ByteBuffer, ByteBuffer> createArgs = CreateArgs.<ByteBuffer, ByteBuffer> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        String result = redis.ftCreate(BLOG_INDEX, createArgs,
                Arrays.asList(titleField, contentField, authorField, createdDateField, viewsField));
        assertThat(result).isEqualTo("OK");

        // Add sample blog posts
        Map<ByteBuffer, ByteBuffer> post1 = new HashMap<>();
        post1.put(toBuffer("title"), toBuffer("Redis Search Tutorial"));
        post1.put(toBuffer("content"), toBuffer("Learn how to use Redis Search for full-text search capabilities"));
        post1.put(toBuffer("author"), toBuffer("john_doe"));
        post1.put(toBuffer("created_date"), toBuffer("1640995200"));
        post1.put(toBuffer("views"), toBuffer("150"));
        assertThat(redis.hmset(toBuffer("blog:post:1"), post1)).isEqualTo("OK");

        Map<ByteBuffer, ByteBuffer> post2 = new HashMap<>();
        post2.put(toBuffer("title"), toBuffer("Advanced Redis Techniques"));
        post2.put(toBuffer("content"), toBuffer("Explore advanced Redis features and optimization techniques"));
        post2.put(toBuffer("author"), toBuffer("jane_smith"));
        post2.put(toBuffer("created_date"), toBuffer("1641081600"));
        post2.put(toBuffer("views"), toBuffer("200"));
        assertThat(redis.hmset(toBuffer("blog:post:2"), post2)).isEqualTo("OK");

        Map<ByteBuffer, ByteBuffer> post3 = new HashMap<>();
        post3.put(toBuffer("title"), toBuffer("Database Performance"));
        post3.put(toBuffer("content"), toBuffer("Tips for improving database performance and scalability"));
        post3.put(toBuffer("author"), toBuffer("john_doe"));
        post3.put(toBuffer("created_date"), toBuffer("1641168000"));
        post3.put(toBuffer("views"), toBuffer("75"));
        assertThat(redis.hmset(toBuffer("blog:post:3"), post3)).isEqualTo("OK");

        // Test 1: Basic text search
        SearchReply<ByteBuffer, ByteBuffer> searchReply = redis.ftSearch(BLOG_INDEX, toBuffer("@title:(Redis)"));
        assertThat(searchReply.getCount()).isEqualTo(2);
        assertThat(searchReply.getResults()).hasSize(2);
        // ByteBuffer uses value equality (unlike byte[] which uses reference equality), so Map.get() works correctly
        assertThat(fromBuffer(searchReply.getResults().get(1).getFields().get(toBuffer("title"))))
                .isEqualTo("Redis Search Tutorial");
        assertThat(fromBuffer(searchReply.getResults().get(0).getFields().get(toBuffer("title"))))
                .isEqualTo("Advanced Redis Techniques");

        // Test 2: Tag search
        searchReply = redis.ftSearch(BLOG_INDEX, toBuffer("@author:{john_doe}"));
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Test 3: Numeric range search
        searchReply = redis.ftSearch(BLOG_INDEX, toBuffer("@views:[100 300]"));
        assertThat(searchReply.getCount()).isEqualTo(2);

        // Cleanup
        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test search options like INFIELDS, RETURN, LIMIT.
     */
    @Test
    void testSearchArgsWithByteBufferParams() {
        FieldArgs<ByteBuffer> titleField = TextFieldArgs.<ByteBuffer> builder().name(toBuffer("title")).build();
        FieldArgs<ByteBuffer> contentField = TextFieldArgs.<ByteBuffer> builder().name(toBuffer("content")).build();
        FieldArgs<ByteBuffer> authorField = TagFieldArgs.<ByteBuffer> builder().name(toBuffer("author")).build();

        CreateArgs<ByteBuffer, ByteBuffer> createArgs = CreateArgs.<ByteBuffer, ByteBuffer> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField, contentField, authorField));

        // Add documents
        Map<ByteBuffer, ByteBuffer> doc1 = new HashMap<>();
        doc1.put(toBuffer("title"), toBuffer("Redis Guide"));
        doc1.put(toBuffer("content"), toBuffer("Complete Redis guide"));
        doc1.put(toBuffer("author"), toBuffer("alice"));
        redis.hmset(toBuffer("blog:post:1"), doc1);

        Map<ByteBuffer, ByteBuffer> doc2 = new HashMap<>();
        doc2.put(toBuffer("title"), toBuffer("Database Book"));
        doc2.put(toBuffer("content"), toBuffer("Redis and databases"));
        doc2.put(toBuffer("author"), toBuffer("bob"));
        redis.hmset(toBuffer("blog:post:2"), doc2);

        // Test inField - search only in title field
        SearchArgs<ByteBuffer, ByteBuffer> inFieldArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .inField(toBuffer("title")).build();
        SearchReply<ByteBuffer, ByteBuffer> inFieldResults = redis.ftSearch(BLOG_INDEX, toBuffer("Redis"), inFieldArgs);
        assertThat(inFieldResults.getCount()).isEqualTo(1); // Only doc1 has "Redis" in title

        // Test returnField - return only specific fields
        SearchArgs<ByteBuffer, ByteBuffer> returnFieldArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .returnField(toBuffer("title")).returnField(toBuffer("author")).build();
        SearchReply<ByteBuffer, ByteBuffer> returnResults = redis.ftSearch(BLOG_INDEX, toBuffer("*"), returnFieldArgs);
        assertThat(returnResults.getCount()).isEqualTo(2);
        // Content field should not be returned - ByteBuffer uses value equality, so Map.get() works
        Map<ByteBuffer, ByteBuffer> fields = returnResults.getResults().get(0).getFields();
        assertThat(fields.get(toBuffer("content"))).isNull();
        assertThat(fields.get(toBuffer("title"))).isNotNull();

        // Test limit
        SearchArgs<ByteBuffer, ByteBuffer> limitArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder().limit(0, 1).build();
        SearchReply<ByteBuffer, ByteBuffer> limitResults = redis.ftSearch(BLOG_INDEX, toBuffer("*"), limitArgs);
        assertThat(limitResults.getCount()).isEqualTo(2); // Total count is still 2
        assertThat(limitResults.getResults()).hasSize(1); // But only 1 result returned

        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test search with PARAMS option using ByteBuffer keys and values.
     */
    @Test
    void testSearchArgsWithParams() {
        FieldArgs<ByteBuffer> titleField = TextFieldArgs.<ByteBuffer> builder().name(toBuffer("title")).build();
        FieldArgs<ByteBuffer> priceField = NumericFieldArgs.<ByteBuffer> builder().name(toBuffer("price")).build();

        CreateArgs<ByteBuffer, ByteBuffer> createArgs = CreateArgs.<ByteBuffer, ByteBuffer> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField, priceField));

        Map<ByteBuffer, ByteBuffer> doc1 = new HashMap<>();
        doc1.put(toBuffer("title"), toBuffer("Cheap Item"));
        doc1.put(toBuffer("price"), toBuffer("50"));
        redis.hmset(toBuffer("blog:post:1"), doc1);

        Map<ByteBuffer, ByteBuffer> doc2 = new HashMap<>();
        doc2.put(toBuffer("title"), toBuffer("Expensive Item"));
        doc2.put(toBuffer("price"), toBuffer("500"));
        redis.hmset(toBuffer("blog:post:2"), doc2);

        // Search using params with ByteBuffer key and value
        SearchArgs<ByteBuffer, ByteBuffer> paramsArgs = SearchArgs.<ByteBuffer, ByteBuffer> builder()
                .param(toBuffer("maxprice"), toBuffer("100")).build();
        SearchReply<ByteBuffer, ByteBuffer> results = redis.ftSearch(BLOG_INDEX, toBuffer("@price:[0 $maxprice]"), paramsArgs);
        assertThat(results.getCount()).isEqualTo(1);
        assertThat(fromBuffer(results.getResults().get(0).getFields().get(toBuffer("title")))).isEqualTo("Cheap Item");

        redis.ftDropindex(BLOG_INDEX);
    }

    /**
     * Test FT.DROPINDEX command.
     */
    @Test
    void testDropIndex() {
        FieldArgs<ByteBuffer> titleField = TextFieldArgs.<ByteBuffer> builder().name(toBuffer("title")).build();
        CreateArgs<ByteBuffer, ByteBuffer> createArgs = CreateArgs.<ByteBuffer, ByteBuffer> builder().withPrefix(BLOG_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(BLOG_INDEX, createArgs, Arrays.asList(titleField));

        String result = redis.ftDropindex(BLOG_INDEX);
        assertThat(result).isEqualTo("OK");
    }

}
