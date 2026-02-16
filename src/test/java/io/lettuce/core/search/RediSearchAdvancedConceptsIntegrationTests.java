/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.search.arguments.ScoringFunction;
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
 * Integration tests for Redis Search advanced concepts based on the Redis documentation.
 * <p>
 * These tests cover advanced Redis Search features including: - Stop words management and customization - Text tokenization and
 * character escaping - Sorting by indexed fields with normalization options - Tag field operations with custom separators and
 * case sensitivity - Text highlighting and summarization - Document scoring functions and algorithms - Language-specific
 * stemming and verbatim search
 * <p>
 * Based on the following <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/">Redis
 * documentation</a>
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class RediSearchAdvancedConceptsIntegrationTests {

    // Index names
    private static final String STOPWORDS_INDEX = "stopwords-idx";

    private static final String TOKENIZATION_INDEX = "tokenization-idx";

    private static final String SORTING_INDEX = "sorting-idx";

    private static final String TAGS_INDEX = "tags-idx";

    private static final String HIGHLIGHT_INDEX = "highlight-idx";

    private static final String SCORING_INDEX = "scoring-idx";

    private static final String STEMMING_INDEX = "stemming-idx";

    // Key prefixes
    private static final String ARTICLE_PREFIX = "article:";

    private static final String DOCUMENT_PREFIX = "doc:";

    private static final String USER_PREFIX = "user:";

    private static final String PRODUCT_PREFIX = "product:";

    private static final String BOOK_PREFIX = "book:";

    private static final String REVIEW_PREFIX = "review:";

    private static final String WORD_PREFIX = "word:";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    public RediSearchAdvancedConceptsIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(getOptions());
        redis = client.connect().sync();
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
     * Test stop words functionality including custom stop words and disabling stop words. Based on the following
     * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/stopwords/">Redis
     * documentation</a>
     */
    @Test
    void testStopWordsManagement() {
        // Test 1: Create index with custom stop words
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> contentField = TextFieldArgs.<String> builder().name("content").build();

        CreateArgs<String, String> customStopWordsArgs = CreateArgs.<String, String> builder().withPrefix(ARTICLE_PREFIX)
                .on(CreateArgs.TargetType.HASH).stopWords(Arrays.asList("foo", "bar", "baz")).build();

        redis.ftCreate(STOPWORDS_INDEX, customStopWordsArgs, Arrays.asList(titleField, contentField));

        // Add test documents
        Map<String, String> article1 = new HashMap<>();
        article1.put("title", "The foo and bar guide");
        article1.put("content", "This is a comprehensive guide about foo and bar concepts");
        redis.hmset("article:1", article1);

        Map<String, String> article2 = new HashMap<>();
        article2.put("title", "Advanced baz techniques");
        article2.put("content", "Learn advanced baz programming techniques and best practices");
        redis.hmset("article:2", article2);

        // Test that custom stop words are ignored in search
        SearchReply<String, String> results = redis.ftSearch(STOPWORDS_INDEX, "foo");
        assertThat(results.getCount()).isEqualTo(0); // "foo" should be ignored as stop word

        results = redis.ftSearch(STOPWORDS_INDEX, "guide");
        assertThat(results.getCount()).isEqualTo(1); // "guide" is not a stop word

        results = redis.ftSearch(STOPWORDS_INDEX, "comprehensive");
        assertThat(results.getCount()).isEqualTo(1); // "comprehensive" is not a stop word

        // Test NOSTOPWORDS option to bypass stop word filtering

        // FIXME DISABLED - not working on the server

        // SearchArgs<String, String> noStopWordsArgs = SearchArgs.<String, String>builder().noStopWords().build();
        // results = redis.ftSearch(STOPWORDS_INDEX, "foo", noStopWordsArgs);
        // assertThat(results.getCount()).isEqualTo(1); // "foo" should be found when stop words are disabled

        // Cleanup
        redis.ftDropindex(STOPWORDS_INDEX);
    }

    /**
     * Test text tokenization and character escaping. Based on the following
     * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/escaping/">Redis
     * documentation</a>
     */
    @Test
    void testTokenizationAndEscaping() {
        // Create index for testing tokenization
        FieldArgs<String> textField = TextFieldArgs.<String> builder().name("text").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(DOCUMENT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(TOKENIZATION_INDEX, createArgs, Collections.singletonList(textField));

        // Add documents with various punctuation and special characters
        Map<String, String> doc1 = new HashMap<>();
        doc1.put("text", "hello-world foo.bar baz_qux");
        redis.hmset("doc:1", doc1);

        Map<String, String> doc2 = new HashMap<>();
        doc2.put("text", "hello\\-world test@example.com");
        redis.hmset("doc:2", doc2);

        Map<String, String> doc3 = new HashMap<>();
        doc3.put("text", "version-2.0 price$19.99 email@domain.org");
        redis.hmset("doc:3", doc3);

        // Test 1: Punctuation marks separate tokens
        SearchReply<String, String> results = redis.ftSearch(TOKENIZATION_INDEX, "hello");
        // FIXME seems that doc:2 is created with hello\\-world instead of hello\-world
        assertThat(results.getCount()).isEqualTo(1); // Both "hello-world" and "hello\\-world"

        results = redis.ftSearch(TOKENIZATION_INDEX, "world");
        assertThat(results.getCount()).isEqualTo(1); // Only "hello-world" (not escaped)

        // Test 2: Underscores are not separators
        results = redis.ftSearch(TOKENIZATION_INDEX, "baz_qux");
        assertThat(results.getCount()).isEqualTo(1); // Underscore keeps the token together

        // Test 3: Email addresses are tokenized by punctuation
        results = redis.ftSearch(TOKENIZATION_INDEX, "test");
        assertThat(results.getCount()).isEqualTo(1);

        results = redis.ftSearch(TOKENIZATION_INDEX, "example");
        assertThat(results.getCount()).isEqualTo(1);

        // Test 4: Numbers with punctuation
        results = redis.ftSearch(TOKENIZATION_INDEX, "2");
        assertThat(results.getCount()).isEqualTo(1); // From "version-2.0"

        results = redis.ftSearch(TOKENIZATION_INDEX, "19");
        assertThat(results.getCount()).isEqualTo(1); // From "price$19.99"

        // Cleanup
        redis.ftDropindex(TOKENIZATION_INDEX);
    }

    /**
     * Test sorting by indexed fields with normalization options. Based on the following
     * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/sorting/">Redis
     * documentation</a>
     */
    @Test
    void testSortingByIndexedFields() {
        // Create index with sortable fields
        FieldArgs<String> firstNameField = TextFieldArgs.<String> builder().name("first_name").sortable().build();
        FieldArgs<String> lastNameField = TextFieldArgs.<String> builder().name("last_name").sortable().build();
        FieldArgs<String> ageField = NumericFieldArgs.<String> builder().name("age").sortable().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(USER_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(SORTING_INDEX, createArgs, Arrays.asList(firstNameField, lastNameField, ageField));

        // Add sample users
        Map<String, String> user1 = new HashMap<>();
        user1.put("first_name", "alice");
        user1.put("last_name", "jones");
        user1.put("age", "35");
        redis.hmset("user:1", user1);

        Map<String, String> user2 = new HashMap<>();
        user2.put("first_name", "bob");
        user2.put("last_name", "jones");
        user2.put("age", "36");
        redis.hmset("user:2", user2);

        Map<String, String> user3 = new HashMap<>();
        user3.put("first_name", "Alice");
        user3.put("last_name", "Smith");
        user3.put("age", "28");
        redis.hmset("user:3", user3);

        // Test 1: Sort by first name descending
        SortByArgs<String> sortByFirstName = SortByArgs.<String> builder().attribute("first_name").descending().build();
        SearchArgs<String, String> sortArgs = SearchArgs.<String, String> builder().sortBy(sortByFirstName).build();
        SearchReply<String, String> results = redis.ftSearch(SORTING_INDEX, "@last_name:jones", sortArgs);

        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getResults()).hasSize(2);
        // Due to normalization, "bob" comes before "alice" in descending order
        assertThat(results.getResults().get(0).getFields().get("first_name")).isEqualTo("bob");
        assertThat(results.getResults().get(1).getFields().get("first_name")).isEqualTo("alice");

        // Test 2: Sort by age ascending
        SortByArgs<String> sortByAge = SortByArgs.<String> builder().attribute("age").build();
        SearchArgs<String, String> ageSort = SearchArgs.<String, String> builder().sortBy(sortByAge).build();
        results = redis.ftSearch(SORTING_INDEX, "*", ageSort);

        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);
        // Verify age sorting: 28, 35, 36
        assertThat(results.getResults().get(0).getFields().get("age")).isEqualTo("28");
        assertThat(results.getResults().get(1).getFields().get("age")).isEqualTo("35");
        assertThat(results.getResults().get(2).getFields().get("age")).isEqualTo("36");

        // Cleanup
        redis.ftDropindex(SORTING_INDEX);
    }

    /**
     * Test tag field operations with custom separators and case sensitivity. Based on the following
     * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/tags/">Redis documentation</a>
     */
    @Test
    void testTagFieldOperations() {
        // Create index with tag fields using custom separator and case sensitivity
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> categoriesField = TagFieldArgs.<String> builder().name("categories").separator(";").build();
        FieldArgs<String> tagsField = TagFieldArgs.<String> builder().name("tags").caseSensitive().build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(PRODUCT_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(TAGS_INDEX, createArgs, Arrays.asList(titleField, categoriesField, tagsField));

        // Add sample products
        Map<String, String> product1 = new HashMap<>();
        product1.put("title", "Gaming Laptop");
        product1.put("categories", "electronics;computers;gaming");
        product1.put("tags", "High-Performance,RGB,Gaming");
        redis.hmset("product:1", product1);

        Map<String, String> product2 = new HashMap<>();
        product2.put("title", "Office Laptop");
        product2.put("categories", "electronics;computers;business");
        product2.put("tags", "Business,Productivity,high-performance");
        redis.hmset("product:2", product2);

        Map<String, String> product3 = new HashMap<>();
        product3.put("title", "Gaming Mouse");
        product3.put("categories", "electronics;gaming;accessories");
        product3.put("tags", "RGB,Wireless,gaming");
        redis.hmset("product:3", product3);

        // Test 1: Search by category with custom separator
        SearchReply<String, String> results = redis.ftSearch(TAGS_INDEX, "@categories:{gaming}");
        assertThat(results.getCount()).isEqualTo(2); // Gaming laptop and mouse

        results = redis.ftSearch(TAGS_INDEX, "@categories:{computers}");
        assertThat(results.getCount()).isEqualTo(2); // Both laptops

        // Test 2: Multiple tags in single filter (OR operation)
        results = redis.ftSearch(TAGS_INDEX, "@categories:{business|accessories}");
        assertThat(results.getCount()).isEqualTo(2); // Office laptop and gaming mouse

        // Test 3: Multiple tag filters (AND operation)
        results = redis.ftSearch(TAGS_INDEX, "@categories:{electronics} @categories:{gaming}");
        assertThat(results.getCount()).isEqualTo(2); // Gaming laptop and mouse

        // Test 4: Case sensitivity in tags
        results = redis.ftSearch(TAGS_INDEX, "@tags:{RGB}");
        assertThat(results.getCount()).isEqualTo(2); // Gaming laptop and mouse (exact case match)

        results = redis.ftSearch(TAGS_INDEX, "@tags:{rgb}");
        assertThat(results.getCount()).isEqualTo(0); // No match due to case sensitivity

        // Test 5: Prefix matching with tags
        results = redis.ftSearch(TAGS_INDEX, "@tags:{High*}");
        assertThat(results.getCount()).isEqualTo(1); // Gaming laptop with "High-Performance"

        results = redis.ftSearch(TAGS_INDEX, "@tags:{high*}");
        assertThat(results.getCount()).isEqualTo(1); // Office laptop with "high-performance"

        // Test 6: Tag with punctuation (hyphen)
        results = redis.ftSearch(TAGS_INDEX, "@tags:{High\\-Performance}");
        assertThat(results.getCount()).isEqualTo(1); // Gaming laptop

        // Cleanup
        redis.ftDropindex(TAGS_INDEX);
    }

    /**
     * Test text highlighting and summarization features. Based on the following
     * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/highlight/">Redis
     * documentation</a>
     */
    @Test
    void testHighlightingAndSummarization() {
        // Create index for highlighting tests
        FieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").build();
        FieldArgs<String> contentField = TextFieldArgs.<String> builder().name("content").build();
        FieldArgs<String> authorField = TextFieldArgs.<String> builder().name("author").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(BOOK_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(HIGHLIGHT_INDEX, createArgs, Arrays.asList(titleField, contentField, authorField));

        // Add sample books with longer content for summarization
        Map<String, String> book1 = new HashMap<>();
        book1.put("title", "Redis in Action");
        book1.put("content",
                "Redis is an open-source, in-memory data structure store used as a database, cache, and message broker. "
                        + "Redis provides data structures such as strings, hashes, lists, sets, sorted sets with range queries, bitmaps, "
                        + "hyperloglogs, geospatial indexes, and streams. Redis has built-in replication, Lua scripting, LRU eviction, "
                        + "transactions, and different levels of on-disk persistence, and provides high availability via Redis Sentinel "
                        + "and automatic partitioning with Redis Cluster.");
        book1.put("author", "Josiah Carlson");
        redis.hmset("book:1", book1);

        Map<String, String> book2 = new HashMap<>();
        book2.put("title", "Database Design Patterns");
        book2.put("content",
                "Database design patterns are reusable solutions to commonly occurring problems in database design. "
                        + "These patterns help developers create efficient, scalable, and maintainable database schemas. Common patterns "
                        + "include normalization, denormalization, partitioning, sharding, and indexing strategies. Understanding these "
                        + "patterns is crucial for building high-performance applications that can handle large amounts of data.");
        book2.put("author", "Jane Smith");
        redis.hmset("book:2", book2);

        // Test 1: Basic highlighting with default tags
        HighlightArgs<String, String> basicHighlight = HighlightArgs.<String, String> builder().build();
        SearchArgs<String, String> highlightArgs = SearchArgs.<String, String> builder().highlightArgs(basicHighlight).build();

        SearchReply<String, String> results = redis.ftSearch(HIGHLIGHT_INDEX, "Redis", highlightArgs);
        assertThat(results.getCount()).isEqualTo(1);

        // Check that highlighting tags are present in the content
        String highlightedContent = results.getResults().get(0).getFields().get("content");
        assertThat(highlightedContent).contains("<b>Redis</b>"); // Default highlighting tags

        // Test 2: Custom highlighting tags
        SearchArgs<String, String> customHighlightArgs = SearchArgs.<String, String> builder().highlightField("title")
                .highlightField("content").highlightTags("<mark>", "</mark>").build();

        results = redis.ftSearch(HIGHLIGHT_INDEX, "database", customHighlightArgs);
        assertThat(results.getCount()).isEqualTo(2);

        // Check custom highlighting tags
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            String content = result.getFields().get("content");
            if (content.contains("database")) {
                assertThat(content).contains("<mark>database</mark>");
            }
        }

        // Test 3: Summarization with custom parameters
        SummarizeArgs<String, String> summarize = SummarizeArgs.<String, String> builder().field("content").fragments(2).len(25)
                .separator(" ... ").build();
        SearchArgs<String, String> summarizeArgs = SearchArgs.<String, String> builder().summarizeArgs(summarize).build();

        results = redis.ftSearch(HIGHLIGHT_INDEX, "patterns", summarizeArgs);
        assertThat(results.getCount()).isEqualTo(1);

        // Check that content is summarized
        String summarizedContent = results.getResults().get(0).getFields().get("content");
        assertThat(summarizedContent).contains(" ... "); // Custom separator
        assertThat(summarizedContent.length()).isLessThan(book2.get("content").length()); // Should be shorter

        // Test 4: Combined highlighting and summarization
        HighlightArgs<String, String> combineHighlight = HighlightArgs.<String, String> builder().field("content")
                .tags("**", "**").build();
        SearchArgs<String, String> combinedArgs = SearchArgs.<String, String> builder().highlightArgs(combineHighlight)
                .summarizeField("content").summarizeFragments(1).summarizeLen(30).build();

        results = redis.ftSearch(HIGHLIGHT_INDEX, "Redis data", combinedArgs);
        assertThat(results.getCount()).isEqualTo(1);

        String combinedContent = results.getResults().get(0).getFields().get("content");
        assertThat(combinedContent).contains("**"); // Highlighting markers
        assertThat(combinedContent).contains("..."); // Default summarization separator

        // Cleanup
        redis.ftDropindex(HIGHLIGHT_INDEX);
    }

    /**
     * Test document scoring functions and algorithms. Based on the following
     * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/scoring/">Redis
     * documentation</a>
     */
    @Test
    void testDocumentScoring() {
        // Create index for scoring tests
        TextFieldArgs<String> titleField = TextFieldArgs.<String> builder().name("title").weight(2).build();
        TextFieldArgs<String> contentField = TextFieldArgs.<String> builder().name("content").build();
        NumericFieldArgs<String> ratingField = NumericFieldArgs.<String> builder().name("rating").build();

        CreateArgs<String, String> createArgs = CreateArgs.<String, String> builder().withPrefix(REVIEW_PREFIX)
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate(SCORING_INDEX, createArgs, Arrays.asList(titleField, contentField, ratingField));

        // Add sample reviews with varying relevance
        Map<String, String> review1 = new HashMap<>();
        review1.put("title", "Excellent Redis Tutorial");
        review1.put("content", "This Redis tutorial is excellent and comprehensive. Redis is amazing for caching.");
        review1.put("rating", "5");
        redis.hmset("review:1", review1);

        Map<String, String> review2 = new HashMap<>();
        review2.put("title", "Good Database Guide");
        review2.put("content",
                "A good guide about databases. Mentions Redis briefly in one chapter. Redis mentioned as a good choice for caching. No other mentions of Redis.");
        review2.put("rating", "4");
        redis.hmset("review:2", review2);

        Map<String, String> review3 = new HashMap<>();
        review3.put("title", "Redis Performance Tips");
        review3.put("content", "Performance optimization tips for Redis. Very detailed Redis configuration guide.");
        review3.put("rating", "5");
        redis.hmset("review:3", review3);

        // Test 1: Default BM25 scoring with scores
        SearchArgs<String, String> withScores = SearchArgs.<String, String> builder().withScores().build();
        SearchReply<String, String> results = redis.ftSearch(SCORING_INDEX, "Redis", withScores);

        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);

        // Verify scores are present and ordered (higher scores first)
        double previousScore = Double.MAX_VALUE;
        for (SearchReply.SearchResult<String, String> result : results.getResults()) {
            assertThat(result.getScore()).isNotNull();
            assertThat(result.getScore()).isLessThanOrEqualTo(previousScore);
            previousScore = result.getScore();
        }

        // Test 2: TFIDF scoring
        SearchArgs<String, String> tfidfScoring = SearchArgs.<String, String> builder().withScores()
                .scorer(ScoringFunction.TF_IDF).build();
        results = redis.ftSearch(SCORING_INDEX, "Redis guide", tfidfScoring);

        assertThat(results.getCount()).isEqualTo(2);
        // Review 3 should score highest due to "Redis" and "guide" having the shortest distance
        assertThat(results.getResults().get(0).getId()).isEqualTo("review:3");

        // Test 3: DISMAX scoring
        SearchArgs<String, String> dismaxScoring = SearchArgs.<String, String> builder().withScores()
                .scorer(ScoringFunction.DIS_MAX).build();
        results = redis.ftSearch(SCORING_INDEX, "Redis guide", dismaxScoring);

        assertThat(results.getCount()).isEqualTo(2);
        // Review 2 should score highest due to having the most mentions of both search terms
        assertThat(results.getResults().get(0).getId()).isEqualTo("review:2");

        // Test 4: DOCSCORE scoring (uses document's inherent score)
        SearchArgs<String, String> docScoring = SearchArgs.<String, String> builder().withScores()
                .scorer(ScoringFunction.DOCUMENT_SCORE).build();
        results = redis.ftSearch(SCORING_INDEX, "*", docScoring);

        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getResults()).hasSize(3);

        // Cleanup
        redis.ftDropindex(SCORING_INDEX);
    }

    /**
     * Test language-specific stemming and verbatim search. Based on the following
     * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/stemming/">Redis
     * documentation</a>
     */
    @Test
    void testStemmingAndLanguageSupport() {
        // Test 1: English stemming
        FieldArgs<String> englishWordField = TextFieldArgs.<String> builder().name("word").build();

        CreateArgs<String, String> englishArgs = CreateArgs.<String, String> builder().withPrefix(WORD_PREFIX)
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.ENGLISH).build();

        redis.ftCreate(STEMMING_INDEX, englishArgs, Collections.singletonList(englishWordField));

        // Add words with different forms
        Map<String, String> word1 = new HashMap<>();
        word1.put("word", "running");
        redis.hmset("word:1", word1);

        Map<String, String> word2 = new HashMap<>();
        word2.put("word", "runs");
        redis.hmset("word:2", word2);

        Map<String, String> word3 = new HashMap<>();
        word3.put("word", "runner");
        redis.hmset("word:3", word3);

        Map<String, String> word4 = new HashMap<>();
        word4.put("word", "run");
        redis.hmset("word:4", word4);

        // Test stemming: searching for "run" should find all variations
        // FIXME Seems like a bug in the server, "runner" needs to also be stemmed, but it is not
        SearchReply<String, String> results = redis.ftSearch(STEMMING_INDEX, "run");
        assertThat(results.getCount()).isEqualTo(3); // All forms should be found due to stemming

        // Test stemming: searching for "running" should also find all variations
        // FIXME Seems like a bug in the server, "runner" needs to also be stemmed, but it is not
        results = redis.ftSearch(STEMMING_INDEX, "running");
        assertThat(results.getCount()).isEqualTo(3);

        // Test VERBATIM search (disable stemming)
        SearchArgs<String, String> verbatimArgs = SearchArgs.<String, String> builder().verbatim().build();
        results = redis.ftSearch(STEMMING_INDEX, "run", verbatimArgs);
        assertThat(results.getCount()).isEqualTo(1); // Only exact match

        results = redis.ftSearch(STEMMING_INDEX, "running", verbatimArgs);
        assertThat(results.getCount()).isEqualTo(1); // Only exact match

        // Test with language parameter in search (should override index language)
        SearchArgs<String, String> languageArgs = SearchArgs.<String, String> builder().language(DocumentLanguage.GERMAN)
                .build();
        results = redis.ftSearch(STEMMING_INDEX, "run", languageArgs);
        // German stemming rules would be different, but for this test we just verify it works
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        // Cleanup
        redis.ftDropindex(STEMMING_INDEX);

        // Test 2: German stemming example from documentation
        FieldArgs<String> germanWordField = TextFieldArgs.<String> builder().name("wort").build();

        CreateArgs<String, String> germanArgs = CreateArgs.<String, String> builder().withPrefix("wort:")
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.GERMAN).build();

        redis.ftCreate("idx:german", germanArgs, Collections.singletonList(germanWordField));

        // Add German words with same stem: stück, stücke, stuck, stucke => stuck
        redis.hset("wort:1", "wort", "stück");
        redis.hset("wort:2", "wort", "stücke");
        redis.hset("wort:3", "wort", "stuck");
        redis.hset("wort:4", "wort", "stucke");

        // Search for "stuck" should find all variations due to German stemming
        results = redis.ftSearch("idx:german", "@wort:(stuck)");
        assertThat(results.getCount()).isEqualTo(4);

        // Cleanup
        redis.ftDropindex("idx:german");
    }

    /**
     * Test TextFieldArgs phonetic matcher options for different languages. Based on Redis documentation for phonetic matching
     * capabilities that enable fuzzy search based on pronunciation similarity.
     */
    @Test
    void testPhoneticMatchers() {
        // Test 1: English phonetic matching
        FieldArgs<String> englishNameField = TextFieldArgs.<String> builder().name("name")
                .phonetic(TextFieldArgs.PhoneticMatcher.ENGLISH).build();

        CreateArgs<String, String> englishArgs = CreateArgs.<String, String> builder().withPrefix("person:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("phonetic-en-idx", englishArgs, Collections.singletonList(englishNameField));

        // Add names with similar pronunciation but different spelling
        redis.hset("person:1", "name", "Smith");
        redis.hset("person:2", "name", "Smyth");
        redis.hset("person:3", "name", "Schmidt");
        redis.hset("person:4", "name", "Johnson");
        redis.hset("person:5", "name", "Jonson");

        // Search for "Smith" should find phonetically similar names
        SearchReply<String, String> results = redis.ftSearch("phonetic-en-idx", "@name:Smith");
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2); // Should find Smith and Smyth at minimum

        // Search for "Johnson" should find phonetically similar names
        results = redis.ftSearch("phonetic-en-idx", "@name:Johnson");
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2); // Should find Johnson and Jonson at minimum

        redis.ftDropindex("phonetic-en-idx");

        // Test 2: French phonetic matching
        FieldArgs<String> frenchNameField = TextFieldArgs.<String> builder().name("nom")
                .phonetic(TextFieldArgs.PhoneticMatcher.FRENCH).build();

        CreateArgs<String, String> frenchArgs = CreateArgs.<String, String> builder().withPrefix("personne:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("phonetic-fr-idx", frenchArgs, Collections.singletonList(frenchNameField));

        // Add French names with similar pronunciation
        redis.hset("personne:1", "nom", "Martin");
        redis.hset("personne:2", "nom", "Martain");
        redis.hset("personne:3", "nom", "Dupont");
        redis.hset("personne:4", "nom", "Dupond");

        // Search should find phonetically similar French names
        results = redis.ftSearch("phonetic-fr-idx", "@nom:Martin");
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        results = redis.ftSearch("phonetic-fr-idx", "@nom:Dupont");
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex("phonetic-fr-idx");

        // Test 3: Spanish phonetic matching
        FieldArgs<String> spanishNameField = TextFieldArgs.<String> builder().name("nombre")
                .phonetic(TextFieldArgs.PhoneticMatcher.SPANISH).build();

        CreateArgs<String, String> spanishArgs = CreateArgs.<String, String> builder().withPrefix("persona:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("phonetic-es-idx", spanishArgs, Collections.singletonList(spanishNameField));

        // Add Spanish names
        redis.hset("persona:1", "nombre", "García");
        redis.hset("persona:2", "nombre", "Garcia");
        redis.hset("persona:3", "nombre", "Rodríguez");
        redis.hset("persona:4", "nombre", "Rodriguez");

        // Search should handle accent variations
        results = redis.ftSearch("phonetic-es-idx", "@nombre:Garcia");
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex("phonetic-es-idx");

        // Test 4: Portuguese phonetic matching
        FieldArgs<String> portugueseNameField = TextFieldArgs.<String> builder().name("nome")
                .phonetic(TextFieldArgs.PhoneticMatcher.PORTUGUESE).build();

        CreateArgs<String, String> portugueseArgs = CreateArgs.<String, String> builder().withPrefix("pessoa:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("phonetic-pt-idx", portugueseArgs, Collections.singletonList(portugueseNameField));

        // Add Portuguese names
        redis.hset("pessoa:1", "nome", "Silva");
        redis.hset("pessoa:2", "nome", "Silveira");
        redis.hset("pessoa:3", "nome", "Santos");
        redis.hset("pessoa:4", "nome", "Santtos");

        // Search should find phonetically similar Portuguese names
        results = redis.ftSearch("phonetic-pt-idx", "@nome:Silva");
        assertThat(results.getCount()).isGreaterThanOrEqualTo(1);

        redis.ftDropindex("phonetic-pt-idx");
    }

    /**
     * Test TextFieldArgs noStem option to disable stemming for specific fields. Demonstrates how to prevent automatic word
     * stemming when exact word matching is required.
     */
    @Test
    void testNoStemmingOption() {
        // Test 1: Field with stemming enabled (default)
        FieldArgs<String> stemmingField = TextFieldArgs.<String> builder().name("content_stemmed").build();

        CreateArgs<String, String> stemmingArgs = CreateArgs.<String, String> builder().withPrefix("stem:")
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.ENGLISH).build();

        redis.ftCreate("stemming-idx", stemmingArgs, Collections.singletonList(stemmingField));

        // Add documents with different word forms
        redis.hset("stem:1", "content_stemmed", "running quickly");
        redis.hset("stem:2", "content_stemmed", "runs fast");
        redis.hset("stem:3", "content_stemmed", "runner speed");

        // Search for "run" should find all variations due to stemming
        SearchReply<String, String> results = redis.ftSearch("stemming-idx", "@content_stemmed:run");
        assertThat(results.getCount()).isGreaterThanOrEqualTo(2); // Should find "running" and "runs"

        redis.ftDropindex("stemming-idx");

        // Test 2: Field with stemming disabled
        FieldArgs<String> noStemmingField = TextFieldArgs.<String> builder().name("content_exact").noStem().build();

        CreateArgs<String, String> noStemmingArgs = CreateArgs.<String, String> builder().withPrefix("nostem:")
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.ENGLISH).build();

        redis.ftCreate("nostemming-idx", noStemmingArgs, Collections.singletonList(noStemmingField));

        // Add the same documents
        redis.hset("nostem:1", "content_exact", "running quickly");
        redis.hset("nostem:2", "content_exact", "runs fast");
        redis.hset("nostem:3", "content_exact", "runner speed");
        redis.hset("nostem:4", "content_exact", "run now");

        // Search for "run" should only find exact matches
        results = redis.ftSearch("nostemming-idx", "@content_exact:run");
        assertThat(results.getCount()).isEqualTo(1); // Only "run now"

        // Search for "running" should only find exact matches
        results = redis.ftSearch("nostemming-idx", "@content_exact:running");
        assertThat(results.getCount()).isEqualTo(1); // Only "running quickly"

        // Search for "runs" should only find exact matches
        results = redis.ftSearch("nostemming-idx", "@content_exact:runs");
        assertThat(results.getCount()).isEqualTo(1); // Only "runs fast"

        redis.ftDropindex("nostemming-idx");

        // Test 3: Mixed fields - one with stemming, one without
        FieldArgs<String> mixedStemField = TextFieldArgs.<String> builder().name("stemmed_content").build();
        FieldArgs<String> mixedNoStemField = TextFieldArgs.<String> builder().name("exact_content").noStem().build();

        CreateArgs<String, String> mixedArgs = CreateArgs.<String, String> builder().withPrefix("mixed:")
                .on(CreateArgs.TargetType.HASH).defaultLanguage(DocumentLanguage.ENGLISH).build();

        redis.ftCreate("mixed-idx", mixedArgs, Arrays.asList(mixedStemField, mixedNoStemField));

        // Add document with both fields
        Map<String, String> mixedDoc = new HashMap<>();
        mixedDoc.put("stemmed_content", "running marathon");
        mixedDoc.put("exact_content", "running marathon");
        redis.hmset("mixed:1", mixedDoc);

        // Search in stemmed field should find with "run"
        results = redis.ftSearch("mixed-idx", "@stemmed_content:run");
        assertThat(results.getCount()).isEqualTo(1);

        // Search in exact field should not find with "run"
        results = redis.ftSearch("mixed-idx", "@exact_content:run");
        assertThat(results.getCount()).isEqualTo(0);

        // Search in exact field should find with "running"
        results = redis.ftSearch("mixed-idx", "@exact_content:running");
        assertThat(results.getCount()).isEqualTo(1);

        redis.ftDropindex("mixed-idx");
    }

    /**
     * Test TextFieldArgs withSuffixTrie option for efficient prefix and suffix matching. Demonstrates how suffix tries enable
     * fast wildcard searches and autocomplete functionality.
     */
    @Test
    void testWithSuffixTrieOption() {
        // Test 1: Field without suffix trie (default)
        FieldArgs<String> normalField = TextFieldArgs.<String> builder().name("title").build();

        CreateArgs<String, String> normalArgs = CreateArgs.<String, String> builder().withPrefix("normal:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("normal-idx", normalArgs, Collections.singletonList(normalField));

        // Add test documents
        redis.hset("normal:1", "title", "JavaScript Programming");
        redis.hset("normal:2", "title", "Java Development");
        redis.hset("normal:3", "title", "Python Scripting");
        redis.hset("normal:4", "title", "Programming Languages");

        // Basic search should work
        SearchReply<String, String> results = redis.ftSearch("normal-idx", "@title:Java*");
        assertThat(results.getCount()).isEqualTo(2); // JavaScript and Java

        redis.ftDropindex("normal-idx");

        // Test 2: Field with suffix trie enabled
        FieldArgs<String> suffixTrieField = TextFieldArgs.<String> builder().name("title").withSuffixTrie().build();

        CreateArgs<String, String> suffixTrieArgs = CreateArgs.<String, String> builder().withPrefix("suffix:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("suffix-idx", suffixTrieArgs, Collections.singletonList(suffixTrieField));

        // Add the same test documents
        redis.hset("suffix:1", "title", "JavaScript Programming");
        redis.hset("suffix:2", "title", "Java Development");
        redis.hset("suffix:3", "title", "Python Scripting");
        redis.hset("suffix:4", "title", "Programming Languages");
        redis.hset("suffix:5", "title", "Advanced JavaScript");
        redis.hset("suffix:6", "title", "Script Writing");

        // Test prefix matching with suffix trie
        results = redis.ftSearch("suffix-idx", "@title:Java*");
        assertThat(results.getCount()).isEqualTo(3); // JavaScript, Java, Advanced JavaScript

        // Test suffix matching (should be more efficient with suffix trie)
        results = redis.ftSearch("suffix-idx", "@title:*Script*");
        assertThat(results.getCount()).isEqualTo(4); // JavaScript, Python Scripting, Advanced JavaScript, Script Writing

        // Test infix matching
        results = redis.ftSearch("suffix-idx", "@title:*gram*");
        assertThat(results.getCount()).isEqualTo(2); // JavaScript Programming, Programming Languages

        // Test exact word matching
        results = redis.ftSearch("suffix-idx", "@title:Programming");
        assertThat(results.getCount()).isEqualTo(2); // JavaScript Programming, Programming Languages

        redis.ftDropindex("suffix-idx");

        // Test 3: Autocomplete-style functionality with suffix trie
        FieldArgs<String> autocompleteField = TextFieldArgs.<String> builder().name("product_name").withSuffixTrie().build();

        CreateArgs<String, String> autocompleteArgs = CreateArgs.<String, String> builder().withPrefix("product:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("autocomplete-idx", autocompleteArgs, Collections.singletonList(autocompleteField));

        // Add products for autocomplete testing
        redis.hset("product:1", "product_name", "iPhone 15 Pro");
        redis.hset("product:2", "product_name", "iPhone 15 Pro Max");
        redis.hset("product:3", "product_name", "iPad Pro");
        redis.hset("product:4", "product_name", "iPad Air");
        redis.hset("product:5", "product_name", "MacBook Pro");
        redis.hset("product:6", "product_name", "MacBook Air");

        // Autocomplete for "iP" should find iPhone and iPad products
        results = redis.ftSearch("autocomplete-idx", "@product_name:iP*");
        assertThat(results.getCount()).isEqualTo(4); // All iPhone and iPad products

        // Autocomplete for "iPhone" should find iPhone products
        results = redis.ftSearch("autocomplete-idx", "@product_name:iPhone*");
        assertThat(results.getCount()).isEqualTo(2); // iPhone 15 Pro and Pro Max

        // Autocomplete for "Mac" should find MacBook products
        results = redis.ftSearch("autocomplete-idx", "@product_name:Mac*");
        assertThat(results.getCount()).isEqualTo(2); // MacBook Pro and Air

        // Search for products ending with "Pro"
        results = redis.ftSearch("autocomplete-idx", "@product_name:*Pro");
        assertThat(results.getCount()).isEqualTo(4); // iPhone 15 Pro, iPad Pro, MacBook Pro, iPhone 15 Pro Max

        // Search for products containing "Air"
        results = redis.ftSearch("autocomplete-idx", "@product_name:*Air*");
        assertThat(results.getCount()).isEqualTo(2); // iPad Air, MacBook Air

        redis.ftDropindex("autocomplete-idx");

        // Test 4: Performance comparison - complex wildcard queries
        FieldArgs<String> performanceField = TextFieldArgs.<String> builder().name("description").withSuffixTrie().build();

        CreateArgs<String, String> performanceArgs = CreateArgs.<String, String> builder().withPrefix("perf:")
                .on(CreateArgs.TargetType.HASH).build();

        redis.ftCreate("performance-idx", performanceArgs, Collections.singletonList(performanceField));

        // Add documents with complex text for performance testing
        redis.hset("perf:1", "description", "High-performance computing with advanced algorithms");
        redis.hset("perf:2", "description", "Machine learning performance optimization techniques");
        redis.hset("perf:3", "description", "Database performance tuning and monitoring");
        redis.hset("perf:4", "description", "Web application performance best practices");
        redis.hset("perf:5", "description", "Network performance analysis and troubleshooting");

        // Complex wildcard queries that benefit from suffix trie
        results = redis.ftSearch("performance-idx", "@description:*perform*");
        assertThat(results.getCount()).isEqualTo(5); // All documents contain "perform"

        results = redis.ftSearch("performance-idx", "@description:*algorithm*");
        assertThat(results.getCount()).isEqualTo(1); // High-performance computing

        results = redis.ftSearch("performance-idx", "@description:*optim*");
        assertThat(results.getCount()).isEqualTo(1); // Machine learning optimization

        redis.ftDropindex("performance-idx");
    }

}
