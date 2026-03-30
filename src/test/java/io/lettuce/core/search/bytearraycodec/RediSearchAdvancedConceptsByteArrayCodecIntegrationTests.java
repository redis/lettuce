/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.bytearraycodec;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.search.SearchReply;
import io.lettuce.core.search.arguments.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis Search advanced concepts using {@link ByteArrayCodec}.
 * <p>
 * These tests cover advanced Redis Search features including: - Stop words management and customization - Text tokenization and
 * character escaping - Sorting by indexed fields with normalization options - Tag field operations with custom separators and
 * case sensitivity - Text highlighting and summarization - Document scoring functions and algorithms - Language-specific
 * stemming and verbatim search
 *
 * @author Viktoriya Kutsarova
 */
@Tag(INTEGRATION_TEST)
public class RediSearchAdvancedConceptsByteArrayCodecIntegrationTests {

    // Index names
    private static final String STOPWORDS_INDEX = "stopwords-idx";

    private static final String TOKENIZATION_INDEX = "tokenization-idx";

    private static final String SORTING_INDEX = "sorting-idx";

    private static final String TAGS_INDEX = "tags-idx";

    private static final String HIGHLIGHT_INDEX = "highlight-idx";

    private static final String SCORING_INDEX = "scoring-idx";

    private static final String STEMMING_INDEX = "stemming-idx";

    // Key prefixes
    private static final byte[] ARTICLE_PREFIX = "article:".getBytes();

    private static final byte[] DOCUMENT_PREFIX = "doc:".getBytes();

    private static final byte[] USER_PREFIX = "user:".getBytes();

    private static final byte[] PRODUCT_PREFIX = "product:".getBytes();

    private static final byte[] BOOK_PREFIX = "book:".getBytes();

    private static final byte[] REVIEW_PREFIX = "review:".getBytes();

    private static final byte[] WORD_PREFIX = "word:".getBytes();

    protected static RedisClient client;

    protected static RedisCommands<byte[], byte[]> redis;

    public RediSearchAdvancedConceptsByteArrayCodecIntegrationTests() {
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
     * Helper method to find a field value in a byte[] keyed map.
     */
    private byte[] getField(Map<byte[], byte[]> fields, String fieldName) {
        byte[] key = fieldName.getBytes();
        return fields.entrySet().stream().filter(e -> Arrays.equals(e.getKey(), key)).map(Map.Entry::getValue).findFirst()
                .orElse(null);
    }

    /**
     * Test stop words functionality including custom stop words and disabling stop words.
     */
    @Test
    void testStopWordsManagement() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();

        CreateArgs<byte[], byte[]> customStopWordsArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(ARTICLE_PREFIX)
                .on(CreateArgs.TargetType.HASH).stopWords(Arrays.asList("foo".getBytes(), "bar".getBytes(), "baz".getBytes()))
                .build();

        redis.ftCreate(STOPWORDS_INDEX, customStopWordsArgs, Arrays.asList(titleField, contentField));

        Map<byte[], byte[]> article1 = new HashMap<>();
        article1.put("title".getBytes(), "The foo and bar guide".getBytes());
        article1.put("content".getBytes(), "This is a comprehensive guide about foo and bar concepts".getBytes());
        redis.hmset("article:1".getBytes(), article1);

        Map<byte[], byte[]> article2 = new HashMap<>();
        article2.put("title".getBytes(), "Advanced baz techniques".getBytes());
        article2.put("content".getBytes(), "Learn advanced baz programming techniques and best practices".getBytes());
        redis.hmset("article:2".getBytes(), article2);

        SearchReply<byte[], byte[]> results = redis.ftSearch(STOPWORDS_INDEX, "foo".getBytes());
        assertThat(results.getCount()).isEqualTo(0);

        results = redis.ftSearch(STOPWORDS_INDEX, "guide".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(STOPWORDS_INDEX, "comprehensive".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex(STOPWORDS_INDEX);
    }

    /**
     * Test text tokenization and character escaping.
     */
    @Test
    void testTokenizationAndEscaping() {
        FieldArgs<byte[]> textField = TextFieldArgs.<byte[]> builder().name("text".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(DOCUMENT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(TOKENIZATION_INDEX, createArgs, Collections.singletonList(textField));

        Map<byte[], byte[]> doc1 = new HashMap<>();
        doc1.put("text".getBytes(), "hello-world foo.bar baz_qux".getBytes());
        redis.hmset("doc:1".getBytes(), doc1);

        Map<byte[], byte[]> doc2 = new HashMap<>();
        doc2.put("text".getBytes(), "hello\\-world test@example.com".getBytes());
        redis.hmset("doc:2".getBytes(), doc2);

        Map<byte[], byte[]> doc3 = new HashMap<>();
        doc3.put("text".getBytes(), "version-2.0 price$19.99 email@domain.org".getBytes());
        redis.hmset("doc:3".getBytes(), doc3);

        SearchReply<byte[], byte[]> results = redis.ftSearch(TOKENIZATION_INDEX, "hello".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TOKENIZATION_INDEX, "world".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TOKENIZATION_INDEX, "baz_qux".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TOKENIZATION_INDEX, "test".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TOKENIZATION_INDEX, "example".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TOKENIZATION_INDEX, "2".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TOKENIZATION_INDEX, "19".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex(TOKENIZATION_INDEX);
    }

    /**
     * Test sorting by indexed fields with normalization options.
     */
    @Test
    void testSortingByIndexedFields() {
        FieldArgs<byte[]> firstNameField = TextFieldArgs.<byte[]> builder().name("first_name".getBytes()).sortable().build();
        FieldArgs<byte[]> lastNameField = TextFieldArgs.<byte[]> builder().name("last_name".getBytes()).sortable().build();
        FieldArgs<byte[]> ageField = NumericFieldArgs.<byte[]> builder().name("age".getBytes()).sortable().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(USER_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(SORTING_INDEX, createArgs, Arrays.asList(firstNameField, lastNameField, ageField));

        Map<byte[], byte[]> user1 = new HashMap<>();
        user1.put("first_name".getBytes(), "alice".getBytes());
        user1.put("last_name".getBytes(), "jones".getBytes());
        user1.put("age".getBytes(), "35".getBytes());
        redis.hmset("user:1".getBytes(), user1);

        Map<byte[], byte[]> user2 = new HashMap<>();
        user2.put("first_name".getBytes(), "bob".getBytes());
        user2.put("last_name".getBytes(), "jones".getBytes());
        user2.put("age".getBytes(), "36".getBytes());
        redis.hmset("user:2".getBytes(), user2);

        Map<byte[], byte[]> user3 = new HashMap<>();
        user3.put("first_name".getBytes(), "Alice".getBytes());
        user3.put("last_name".getBytes(), "Smith".getBytes());
        user3.put("age".getBytes(), "28".getBytes());
        redis.hmset("user:3".getBytes(), user3);

        SortByArgs<byte[]> sortByFirstName = SortByArgs.<byte[]> builder().attribute("first_name".getBytes()).descending()
                .build();
        SearchArgs<byte[], byte[]> sortArgs = SearchArgs.<byte[], byte[]> builder().sortBy(sortByFirstName).build();
        SearchReply<byte[], byte[]> results = redis.ftSearch(SORTING_INDEX, "@last_name:jones".getBytes(), sortArgs);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "first_name"))).isEqualTo("bob");
        assertThat(new String(getField(results.getResults().get(1).getFields(), "first_name"))).isEqualTo("alice");

        SortByArgs<byte[]> sortByAge = SortByArgs.<byte[]> builder().attribute("age".getBytes()).build();
        SearchArgs<byte[], byte[]> ageSort = SearchArgs.<byte[], byte[]> builder().sortBy(sortByAge).build();
        results = redis.ftSearch(SORTING_INDEX, "*".getBytes(), ageSort);

        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);
        assertThat(new String(getField(results.getResults().get(0).getFields(), "age"))).isEqualTo("28");
        assertThat(new String(getField(results.getResults().get(1).getFields(), "age"))).isEqualTo("35");
        assertThat(new String(getField(results.getResults().get(2).getFields(), "age"))).isEqualTo("36");

        redis.ftDropindex(SORTING_INDEX);
    }

    /**
     * Test tag field operations with custom separators and case sensitivity.
     */
    @Test
    void testTagFieldOperations() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> categoriesField = TagFieldArgs.<byte[]> builder().name("categories".getBytes()).separator(";")
                .build();
        FieldArgs<byte[]> tagsField = TagFieldArgs.<byte[]> builder().name("tags".getBytes()).caseSensitive().build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(PRODUCT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(TAGS_INDEX, createArgs, Arrays.asList(titleField, categoriesField, tagsField));

        Map<byte[], byte[]> product1 = new HashMap<>();
        product1.put("title".getBytes(), "Gaming Laptop".getBytes());
        product1.put("categories".getBytes(), "electronics;computers;gaming".getBytes());
        product1.put("tags".getBytes(), "High-Performance,RGB,Gaming".getBytes());
        redis.hmset("product:1".getBytes(), product1);

        Map<byte[], byte[]> product2 = new HashMap<>();
        product2.put("title".getBytes(), "Office Laptop".getBytes());
        product2.put("categories".getBytes(), "electronics;computers;business".getBytes());
        product2.put("tags".getBytes(), "Business,Productivity,high-performance".getBytes());
        redis.hmset("product:2".getBytes(), product2);

        Map<byte[], byte[]> product3 = new HashMap<>();
        product3.put("title".getBytes(), "Gaming Mouse".getBytes());
        product3.put("categories".getBytes(), "electronics;gaming;accessories".getBytes());
        product3.put("tags".getBytes(), "RGB,Wireless,gaming".getBytes());
        redis.hmset("product:3".getBytes(), product3);

        SearchReply<byte[], byte[]> results = redis.ftSearch(TAGS_INDEX, "@categories:{gaming}".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(TAGS_INDEX, "@categories:{computers}".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(TAGS_INDEX, "@categories:{business|accessories}".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(TAGS_INDEX, "@categories:{electronics} @categories:{gaming}".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(TAGS_INDEX, "@tags:{RGB}".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch(TAGS_INDEX, "@tags:{rgb}".getBytes());
        assertThat(results.getCount()).isEqualTo(0);

        results = redis.ftSearch(TAGS_INDEX, "@tags:{High*}".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TAGS_INDEX, "@tags:{high*}".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TAGS_INDEX, "@tags:{High\\-Performance}".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex(TAGS_INDEX);
    }

    /**
     * Test text highlighting and summarization features.
     */
    @Test
    void testHighlightingAndSummarization() {
        FieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();
        FieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();
        FieldArgs<byte[]> authorField = TextFieldArgs.<byte[]> builder().name("author".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(BOOK_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(HIGHLIGHT_INDEX, createArgs, Arrays.asList(titleField, contentField, authorField));

        Map<byte[], byte[]> book1 = new HashMap<>();
        book1.put("title".getBytes(), "Redis in Action".getBytes());
        byte[] book1Content = ("Redis is an open-source, in-memory data structure store used as a database, cache, and message broker. "
                + "Redis provides data structures such as strings, hashes, lists, sets, sorted sets with range queries, bitmaps, "
                + "hyperloglogs, geospatial indexes, and streams. Redis has built-in replication, Lua scripting, LRU eviction, "
                + "transactions, and different levels of on-disk persistence, and provides high availability via Redis Sentinel "
                + "and automatic partitioning with Redis Cluster.").getBytes();
        book1.put("content".getBytes(), book1Content);
        book1.put("author".getBytes(), "Josiah Carlson".getBytes());
        redis.hmset("book:1".getBytes(), book1);

        Map<byte[], byte[]> book2 = new HashMap<>();
        book2.put("title".getBytes(), "Database Design Patterns".getBytes());
        byte[] book2Content = ("Database design patterns are reusable solutions to commonly occurring problems in database design. "
                + "These patterns help developers create efficient, scalable, and maintainable database schemas. Common patterns "
                + "include normalization, denormalization, partitioning, sharding, and indexing strategies. Understanding these "
                + "patterns is crucial for building high-performance applications that can handle large amounts of data.")
                        .getBytes();
        book2.put("content".getBytes(), book2Content);
        book2.put("author".getBytes(), "Jane Smith".getBytes());
        redis.hmset("book:2".getBytes(), book2);

        // Test 1: Basic highlighting with default tags
        HighlightArgs<byte[], byte[]> basicHighlight = HighlightArgs.<byte[], byte[]> builder().build();
        SearchArgs<byte[], byte[]> highlightArgs = SearchArgs.<byte[], byte[]> builder().highlightArgs(basicHighlight).build();

        SearchReply<byte[], byte[]> results = redis.ftSearch(HIGHLIGHT_INDEX, "Redis".getBytes(), highlightArgs);
        assertThat(results.getCount()).isEqualTo(1);

        String highlightedContent = new String(getField(results.getResults().get(0).getFields(), "content"));
        assertThat(highlightedContent).contains("<b>Redis</b>");

        // Test 2: Custom highlighting tags
        SearchArgs<byte[], byte[]> customHighlightArgs = SearchArgs.<byte[], byte[]> builder()
                .highlightField("title".getBytes()).highlightField("content".getBytes())
                .highlightTags("<mark>".getBytes(), "</mark>".getBytes()).build();

        results = redis.ftSearch(HIGHLIGHT_INDEX, "database".getBytes(), customHighlightArgs);
        assertThat(results.getCount()).isEqualTo(2);

        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            String content = new String(getField(result.getFields(), "content"));
            if (content.contains("database")) {
                assertThat(content).contains("<mark>database</mark>");
            }
        }

        // Test 3: Summarization with custom parameters
        SummarizeArgs<byte[], byte[]> summarize = SummarizeArgs.<byte[], byte[]> builder().field("content".getBytes())
                .fragments(2).len(25).separator(" ... ".getBytes()).build();
        SearchArgs<byte[], byte[]> summarizeArgs = SearchArgs.<byte[], byte[]> builder().summarizeArgs(summarize).build();

        results = redis.ftSearch(HIGHLIGHT_INDEX, "patterns".getBytes(), summarizeArgs);
        assertThat(results.getCount()).isEqualTo(1);

        String summarizedContent = new String(getField(results.getResults().get(0).getFields(), "content"));
        assertThat(summarizedContent).contains(" ... ");
        assertThat(summarizedContent.length()).isLessThan(book2Content.length);

        // Test 4: Combined highlighting and summarization
        HighlightArgs<byte[], byte[]> combineHighlight = HighlightArgs.<byte[], byte[]> builder().field("content".getBytes())
                .tags("**".getBytes(), "**".getBytes()).build();
        SearchArgs<byte[], byte[]> combinedArgs = SearchArgs.<byte[], byte[]> builder().highlightArgs(combineHighlight)
                .summarizeField("content".getBytes()).summarizeFragments(1).summarizeLen(30).build();

        results = redis.ftSearch(HIGHLIGHT_INDEX, "Redis data".getBytes(), combinedArgs);
        assertThat(results.getCount()).isEqualTo(1);

        String combinedContent = new String(getField(results.getResults().get(0).getFields(), "content"));
        assertThat(combinedContent).contains("**");
        assertThat(combinedContent).contains("...");

        redis.ftDropindex(HIGHLIGHT_INDEX);
    }

    /**
     * Test document scoring functions and algorithms.
     */
    @Test
    void testDocumentScoring() {
        TextFieldArgs<byte[]> titleField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).weight(2).build();
        TextFieldArgs<byte[]> contentField = TextFieldArgs.<byte[]> builder().name("content".getBytes()).build();
        NumericFieldArgs<byte[]> ratingField = NumericFieldArgs.<byte[]> builder().name("rating".getBytes()).build();

        CreateArgs<byte[], byte[]> createArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(REVIEW_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(SCORING_INDEX, createArgs, Arrays.asList(titleField, contentField, ratingField));

        Map<byte[], byte[]> review1 = new HashMap<>();
        review1.put("title".getBytes(), "Excellent Redis Tutorial".getBytes());
        review1.put("content".getBytes(),
                "This Redis tutorial is excellent and comprehensive. Redis is amazing for caching.".getBytes());
        review1.put("rating".getBytes(), "5".getBytes());
        redis.hmset("review:1".getBytes(), review1);

        Map<byte[], byte[]> review2 = new HashMap<>();
        review2.put("title".getBytes(), "Good Database Guide".getBytes());
        review2.put("content".getBytes(),
                ("A good guide about databases. Mentions Redis briefly in one chapter. Redis mentioned as a good choice for caching. No other mentions of Redis.")
                        .getBytes());
        review2.put("rating".getBytes(), "4".getBytes());
        redis.hmset("review:2".getBytes(), review2);

        Map<byte[], byte[]> review3 = new HashMap<>();
        review3.put("title".getBytes(), "Redis Performance Tips".getBytes());
        review3.put("content".getBytes(),
                "Performance optimization tips for Redis. Very detailed Redis configuration guide.".getBytes());
        review3.put("rating".getBytes(), "5".getBytes());
        redis.hmset("review:3".getBytes(), review3);

        // Test 1: Default BM25 scoring with scores
        SearchArgs<byte[], byte[]> withScores = SearchArgs.<byte[], byte[]> builder().withScores().build();
        SearchReply<byte[], byte[]> results = redis.ftSearch(SCORING_INDEX, "Redis".getBytes(), withScores);

        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);

        double previousScore = Double.MAX_VALUE;
        for (SearchReply.SearchResult<byte[], byte[]> result : results.getResults()) {
            assertThat(result.getScore()).isNotNull();
            assertThat(result.getScore()).isLessThanOrEqualTo(previousScore);
            previousScore = result.getScore();
        }

        // Test 2: TFIDF scoring
        SearchArgs<byte[], byte[]> tfidfScoring = SearchArgs.<byte[], byte[]> builder().withScores()
                .scorer(ScoringFunction.TF_IDF).build();
        results = redis.ftSearch(SCORING_INDEX, "Redis guide".getBytes(), tfidfScoring);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(new String(results.getResults().get(0).getId())).isEqualTo("review:3");

        // Test 3: DISMAX scoring
        SearchArgs<byte[], byte[]> dismaxScoring = SearchArgs.<byte[], byte[]> builder().withScores()
                .scorer(ScoringFunction.DIS_MAX).build();
        results = redis.ftSearch(SCORING_INDEX, "Redis guide".getBytes(), dismaxScoring);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(new String(results.getResults().get(0).getId())).isEqualTo("review:2");

        // Test 4: DOCSCORE scoring
        SearchArgs<byte[], byte[]> docScoring = SearchArgs.<byte[], byte[]> builder().withScores()
                .scorer(ScoringFunction.DOCUMENT_SCORE).build();
        results = redis.ftSearch(SCORING_INDEX, "*".getBytes(), docScoring);

        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);

        redis.ftDropindex(SCORING_INDEX);
    }

    /**
     * Test language-specific stemming and verbatim search.
     */
    @Test
    void testStemmingAndLanguageSupport() {
        FieldArgs<byte[]> englishWordField = TextFieldArgs.<byte[]> builder().name("word".getBytes()).build();

        CreateArgs<byte[], byte[]> englishArgs = CreateArgs.<byte[], byte[]> builder().withPrefix(WORD_PREFIX)
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.ENGLISH).build();

        redis.ftCreate(STEMMING_INDEX, englishArgs, Collections.singletonList(englishWordField));

        redis.hmset("word:1".getBytes(), singleFieldMap("word", "running"));
        redis.hmset("word:2".getBytes(), singleFieldMap("word", "runs"));
        redis.hmset("word:3".getBytes(), singleFieldMap("word", "runner"));
        redis.hmset("word:4".getBytes(), singleFieldMap("word", "run"));

        // FIXME Seems like a bug in the server, "runner" needs to also be stemmed, but it is not
        SearchReply<byte[], byte[]> results = redis.ftSearch(STEMMING_INDEX, "run".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        // FIXME Seems like a bug in the server, "runner" needs to also be stemmed, but it is not
        results = redis.ftSearch(STEMMING_INDEX, "running".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        SearchArgs<byte[], byte[]> verbatimArgs = SearchArgs.<byte[], byte[]> builder().verbatim().build();
        results = redis.ftSearch(STEMMING_INDEX, "run".getBytes(), verbatimArgs);
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(STEMMING_INDEX, "running".getBytes(), verbatimArgs);
        assertThat(results.getCount()).isEqualTo(1);

        SearchArgs<byte[], byte[]> languageArgs = SearchArgs.<byte[], byte[]> builder().language(DocumentLanguage.GERMAN)
                .build();
        results = redis.ftSearch(STEMMING_INDEX, "run".getBytes(), languageArgs);
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex(STEMMING_INDEX);

        // Test 2: German stemming
        FieldArgs<byte[]> germanWordField = TextFieldArgs.<byte[]> builder().name("wort".getBytes()).build();

        CreateArgs<byte[], byte[]> germanArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("wort:".getBytes())
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.GERMAN).build();

        redis.ftCreate("idx:german", germanArgs, Collections.singletonList(germanWordField));

        redis.hset("wort:1".getBytes(), "wort".getBytes(), "stück".getBytes());
        redis.hset("wort:2".getBytes(), "wort".getBytes(), "stücke".getBytes());
        redis.hset("wort:3".getBytes(), "wort".getBytes(), "stuck".getBytes());
        redis.hset("wort:4".getBytes(), "wort".getBytes(), "stucke".getBytes());

        results = redis.ftSearch("idx:german", "@wort:(stuck)".getBytes());
        assertThat(results.getCount()).isEqualTo(4);

        redis.ftDropindex("idx:german");
    }

    /**
     * Test TextFieldArgs phonetic matcher options for different languages.
     */
    @Test
    void testPhoneticMatchers() {
        // Test 1: English phonetic matching
        FieldArgs<byte[]> englishNameField = TextFieldArgs.<byte[]> builder().name("name".getBytes())
                .phonetic(TextFieldArgs.PhoneticMatcher.ENGLISH).build();

        CreateArgs<byte[], byte[]> englishArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("person:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("phonetic-en-idx", englishArgs, Collections.singletonList(englishNameField));

        redis.hset("person:1".getBytes(), "name".getBytes(), "Smith".getBytes());
        redis.hset("person:2".getBytes(), "name".getBytes(), "Smyth".getBytes());
        redis.hset("person:3".getBytes(), "name".getBytes(), "Schmidt".getBytes());
        redis.hset("person:4".getBytes(), "name".getBytes(), "Johnson".getBytes());
        redis.hset("person:5".getBytes(), "name".getBytes(), "Jonson".getBytes());

        SearchReply<byte[], byte[]> results = redis.ftSearch("phonetic-en-idx", "@name:Smith".getBytes());
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        results = redis.ftSearch("phonetic-en-idx", "@name:Johnson".getBytes());
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        redis.ftDropindex("phonetic-en-idx");

        // Test 2: French phonetic matching
        FieldArgs<byte[]> frenchNameField = TextFieldArgs.<byte[]> builder().name("nom".getBytes())
                .phonetic(TextFieldArgs.PhoneticMatcher.FRENCH).build();

        CreateArgs<byte[], byte[]> frenchArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("personne:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("phonetic-fr-idx", frenchArgs, Collections.singletonList(frenchNameField));

        redis.hset("personne:1".getBytes(), "nom".getBytes(), "Martin".getBytes());
        redis.hset("personne:2".getBytes(), "nom".getBytes(), "Martain".getBytes());
        redis.hset("personne:3".getBytes(), "nom".getBytes(), "Dupont".getBytes());
        redis.hset("personne:4".getBytes(), "nom".getBytes(), "Dupond".getBytes());

        results = redis.ftSearch("phonetic-fr-idx", "@nom:Martin".getBytes());
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        results = redis.ftSearch("phonetic-fr-idx", "@nom:Dupont".getBytes());
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex("phonetic-fr-idx");

        // Test 3: Spanish phonetic matching
        FieldArgs<byte[]> spanishNameField = TextFieldArgs.<byte[]> builder().name("nombre".getBytes())
                .phonetic(TextFieldArgs.PhoneticMatcher.SPANISH).build();

        CreateArgs<byte[], byte[]> spanishArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("persona:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("phonetic-es-idx", spanishArgs, Collections.singletonList(spanishNameField));

        redis.hset("persona:1".getBytes(), "nombre".getBytes(), "García".getBytes());
        redis.hset("persona:2".getBytes(), "nombre".getBytes(), "Garcia".getBytes());
        redis.hset("persona:3".getBytes(), "nombre".getBytes(), "Rodríguez".getBytes());
        redis.hset("persona:4".getBytes(), "nombre".getBytes(), "Rodriguez".getBytes());

        results = redis.ftSearch("phonetic-es-idx", "@nombre:Garcia".getBytes());
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex("phonetic-es-idx");

        // Test 4: Portuguese phonetic matching
        FieldArgs<byte[]> portugueseNameField = TextFieldArgs.<byte[]> builder().name("nome".getBytes())
                .phonetic(TextFieldArgs.PhoneticMatcher.PORTUGUESE).build();

        CreateArgs<byte[], byte[]> portugueseArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("pessoa:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("phonetic-pt-idx", portugueseArgs, Collections.singletonList(portugueseNameField));

        redis.hset("pessoa:1".getBytes(), "nome".getBytes(), "Silva".getBytes());
        redis.hset("pessoa:2".getBytes(), "nome".getBytes(), "Silveira".getBytes());
        redis.hset("pessoa:3".getBytes(), "nome".getBytes(), "Santos".getBytes());
        redis.hset("pessoa:4".getBytes(), "nome".getBytes(), "Santtos".getBytes());

        results = redis.ftSearch("phonetic-pt-idx", "@nome:Silva".getBytes());
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex("phonetic-pt-idx");
    }

    /**
     * Test TextFieldArgs noStem option to disable stemming for specific fields.
     */
    @Test
    void testNoStemmingOption() {
        // Test 1: Field with stemming enabled (default)
        FieldArgs<byte[]> stemmingField = TextFieldArgs.<byte[]> builder().name("content_stemmed".getBytes()).build();

        CreateArgs<byte[], byte[]> stemmingArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("stem:".getBytes())
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.ENGLISH).build();

        redis.ftCreate("stemming-idx", stemmingArgs, Collections.singletonList(stemmingField));

        redis.hset("stem:1".getBytes(), "content_stemmed".getBytes(), "running quickly".getBytes());
        redis.hset("stem:2".getBytes(), "content_stemmed".getBytes(), "runs fast".getBytes());
        redis.hset("stem:3".getBytes(), "content_stemmed".getBytes(), "runner speed".getBytes());

        SearchReply<byte[], byte[]> results = redis.ftSearch("stemming-idx", "@content_stemmed:run".getBytes());
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2);

        redis.ftDropindex("stemming-idx");

        // Test 2: Field with stemming disabled
        FieldArgs<byte[]> noStemmingField = TextFieldArgs.<byte[]> builder().name("content_exact".getBytes()).noStem().build();

        CreateArgs<byte[], byte[]> noStemmingArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("nostem:".getBytes())
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.ENGLISH).build();

        redis.ftCreate("nostemming-idx", noStemmingArgs, Collections.singletonList(noStemmingField));

        redis.hset("nostem:1".getBytes(), "content_exact".getBytes(), "running quickly".getBytes());
        redis.hset("nostem:2".getBytes(), "content_exact".getBytes(), "runs fast".getBytes());
        redis.hset("nostem:3".getBytes(), "content_exact".getBytes(), "runner speed".getBytes());
        redis.hset("nostem:4".getBytes(), "content_exact".getBytes(), "run now".getBytes());

        results = redis.ftSearch("nostemming-idx", "@content_exact:run".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch("nostemming-idx", "@content_exact:running".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch("nostemming-idx", "@content_exact:runs".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex("nostemming-idx");

        // Test 3: Mixed fields - one with stemming, one without
        FieldArgs<byte[]> mixedStemField = TextFieldArgs.<byte[]> builder().name("stemmed_content".getBytes()).build();
        FieldArgs<byte[]> mixedNoStemField = TextFieldArgs.<byte[]> builder().name("exact_content".getBytes()).noStem().build();

        CreateArgs<byte[], byte[]> mixedArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("mixed:".getBytes())
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.ENGLISH).build();

        redis.ftCreate("mixed-idx", mixedArgs, Arrays.asList(mixedStemField, mixedNoStemField));

        Map<byte[], byte[]> mixedDoc = new HashMap<>();
        mixedDoc.put("stemmed_content".getBytes(), "running marathon".getBytes());
        mixedDoc.put("exact_content".getBytes(), "running marathon".getBytes());
        redis.hmset("mixed:1".getBytes(), mixedDoc);

        results = redis.ftSearch("mixed-idx", "@stemmed_content:run".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch("mixed-idx", "@exact_content:run".getBytes());
        assertThat(results.getCount()).isEqualTo(0);

        results = redis.ftSearch("mixed-idx", "@exact_content:running".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex("mixed-idx");
    }

    /**
     * Test TextFieldArgs withSuffixTrie option for efficient prefix and suffix matching.
     */
    @Test
    void testWithSuffixTrieOption() {
        // Test 1: Field without suffix trie (default)
        FieldArgs<byte[]> normalField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).build();

        CreateArgs<byte[], byte[]> normalArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("normal:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("normal-idx", normalArgs, Collections.singletonList(normalField));

        redis.hset("normal:1".getBytes(), "title".getBytes(), "JavaScript Programming".getBytes());
        redis.hset("normal:2".getBytes(), "title".getBytes(), "Java Development".getBytes());
        redis.hset("normal:3".getBytes(), "title".getBytes(), "Python Scripting".getBytes());
        redis.hset("normal:4".getBytes(), "title".getBytes(), "Programming Languages".getBytes());

        SearchReply<byte[], byte[]> results = redis.ftSearch("normal-idx", "@title:Java*".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        redis.ftDropindex("normal-idx");

        // Test 2: Field with suffix trie enabled
        FieldArgs<byte[]> suffixTrieField = TextFieldArgs.<byte[]> builder().name("title".getBytes()).withSuffixTrie().build();

        CreateArgs<byte[], byte[]> suffixTrieArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("suffix:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("suffix-idx", suffixTrieArgs, Collections.singletonList(suffixTrieField));

        redis.hset("suffix:1".getBytes(), "title".getBytes(), "JavaScript Programming".getBytes());
        redis.hset("suffix:2".getBytes(), "title".getBytes(), "Java Development".getBytes());
        redis.hset("suffix:3".getBytes(), "title".getBytes(), "Python Scripting".getBytes());
        redis.hset("suffix:4".getBytes(), "title".getBytes(), "Programming Languages".getBytes());
        redis.hset("suffix:5".getBytes(), "title".getBytes(), "Advanced JavaScript".getBytes());
        redis.hset("suffix:6".getBytes(), "title".getBytes(), "Script Writing".getBytes());

        results = redis.ftSearch("suffix-idx", "@title:Java*".getBytes());
        assertThat(results.getCount()).isEqualTo(3);

        results = redis.ftSearch("suffix-idx", "@title:*Script*".getBytes());
        assertThat(results.getCount()).isEqualTo(4);

        results = redis.ftSearch("suffix-idx", "@title:*gram*".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch("suffix-idx", "@title:Programming".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        redis.ftDropindex("suffix-idx");

        // Test 3: Autocomplete-style functionality with suffix trie
        FieldArgs<byte[]> autocompleteField = TextFieldArgs.<byte[]> builder().name("product_name".getBytes()).withSuffixTrie()
                .build();

        CreateArgs<byte[], byte[]> autocompleteArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("product:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("autocomplete-idx", autocompleteArgs, Collections.singletonList(autocompleteField));

        redis.hset("product:1".getBytes(), "product_name".getBytes(), "iPhone 15 Pro".getBytes());
        redis.hset("product:2".getBytes(), "product_name".getBytes(), "iPhone 15 Pro Max".getBytes());
        redis.hset("product:3".getBytes(), "product_name".getBytes(), "iPad Pro".getBytes());
        redis.hset("product:4".getBytes(), "product_name".getBytes(), "iPad Air".getBytes());
        redis.hset("product:5".getBytes(), "product_name".getBytes(), "MacBook Pro".getBytes());
        redis.hset("product:6".getBytes(), "product_name".getBytes(), "MacBook Air".getBytes());

        results = redis.ftSearch("autocomplete-idx", "@product_name:iP*".getBytes());
        assertThat(results.getCount()).isEqualTo(4);

        results = redis.ftSearch("autocomplete-idx", "@product_name:iPhone*".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch("autocomplete-idx", "@product_name:Mac*".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        results = redis.ftSearch("autocomplete-idx", "@product_name:*Pro".getBytes());
        assertThat(results.getCount()).isEqualTo(4);

        results = redis.ftSearch("autocomplete-idx", "@product_name:*Air*".getBytes());
        assertThat(results.getCount()).isEqualTo(2);

        redis.ftDropindex("autocomplete-idx");

        // Test 4: Performance comparison - complex wildcard queries
        FieldArgs<byte[]> performanceField = TextFieldArgs.<byte[]> builder().name("description".getBytes()).withSuffixTrie()
                .build();

        CreateArgs<byte[], byte[]> performanceArgs = CreateArgs.<byte[], byte[]> builder().withPrefix("perf:".getBytes())
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("performance-idx", performanceArgs, Collections.singletonList(performanceField));

        redis.hset("perf:1".getBytes(), "description".getBytes(),
                "High-performance computing with advanced algorithms".getBytes());
        redis.hset("perf:2".getBytes(), "description".getBytes(),
                "Machine learning performance optimization techniques".getBytes());
        redis.hset("perf:3".getBytes(), "description".getBytes(), "Database performance tuning and monitoring".getBytes());
        redis.hset("perf:4".getBytes(), "description".getBytes(), "Web application performance best practices".getBytes());
        redis.hset("perf:5".getBytes(), "description".getBytes(),
                "Network performance analysis and troubleshooting".getBytes());

        results = redis.ftSearch("performance-idx", "@description:*perform*".getBytes());
        assertThat(results.getCount()).isEqualTo(5);

        results = redis.ftSearch("performance-idx", "@description:*algorithm*".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch("performance-idx", "@description:*optim*".getBytes());
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex("performance-idx");
    }

    /**
     * Helper to create a single-field byte[] map.
     */
    private Map<byte[], byte[]> singleFieldMap(String key, String value) {
        Map<byte[], byte[]> map = new HashMap<>();
        map.put(key.getBytes(), value.getBytes());
        return map;
    }

}
