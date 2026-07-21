# Redis Search support in Lettuce

Lettuce supports [Redis Search](https://redis.io/docs/latest/develop/ai/search-and-query/) starting from [Lettuce 6.8.0.RELEASE](https://github.com/redis/lettuce/releases/tag/6.8.0.RELEASE).

Redis Search provides a rich query engine that enables full-text search, vector search, geospatial queries, and aggregations on Redis data. It transforms Redis into a powerful document database, vector database, secondary index, and search engine.

!!! INFO
    Redis Search is available in Redis Open Source version 8.0, Redis Enterprise, and Redis Cloud. For older versions of Redis Open Source the functionality requires the RediSearch module to be loaded.

!!! WARNING
    Redis Search commands are marked as `@Experimental` in Lettuce 6.8 and may undergo API changes in future releases. The underlying Redis Search functionality is stable and production-ready.

## Core Concepts

Redis Search operates on **indexes** that define how your data should be searchable. An index specifies:

- **Data source**: Which Redis keys to index (HASH or JSON documents)
- **Field definitions**: Which fields are searchable and their types (TEXT, NUMERIC, TAG, GEO, VECTOR)
- **Search capabilities**: Full-text search, exact matching, range queries, vector similarity

## Getting Started

### Basic Setup

```java
RedisURI redisURI = RedisURI.Builder.redis("localhost").withPort(6379).build();
RedisClient redisClient = RedisClient.create(redisURI);
StatefulRedisConnection<String, String> connection = redisClient.connect();
RedisCommands<String, String> redis = connection.sync();
RediSearchCommands<String> search = redis;
```

### Creating Your First Index

```java
// Define searchable fields
List<FieldArgs> fields = Arrays.asList(
    TextFieldArgs.builder().name("title").build(),
    TextFieldArgs.builder().name("content").build(),
    NumericFieldArgs.builder().name("price").sortable().build(),
    TagFieldArgs.builder().name("category").sortable().build()
);

// Create the index
String result = search.ftCreate("products-idx", fields);
// Returns: "OK"
```

### Adding Data

```java
// Add documents as Redis hashes
Map<String, String> product1 = new HashMap<>();
product1.put("title", "Wireless Headphones");
product1.put("content", "High-quality wireless headphones with noise cancellation");
product1.put("price", "199.99");
product1.put("category", "electronics");
redis.hmset("product:1", product1);

Map<String, String> product2 = new HashMap<>();
product2.put("title", "Running Shoes");
product2.put("content", "Comfortable running shoes for daily exercise");
product2.put("price", "89.99");
product2.put("category", "sports");
redis.hmset("product:2", product2);
```

### Basic Search

```java
// Simple text search
SearchReply<String> results = search.ftSearch("products-idx", "wireless");

// Access results
System.out.println("Found " + results.getCount() + " documents");
for (SearchReply.SearchResult<String> result : results.getResults()) {
    System.out.println("Key: " + result.getId());
    System.out.println("Title: " + result.getFields().get("title"));
}
```

## Field Types and Indexing

### Text Fields
Full-text searchable fields with stemming, phonetic matching, and scoring.

```java
TextFieldArgs titleField = TextFieldArgs.builder()
    .name("title")
    .weight(2)                                        // Boost importance in scoring
    .sortable()                                       // Enable sorting
    .noStem()                                         // Disable stemming
    .phonetic(TextFieldArgs.PhoneticMatcher.ENGLISH)  // Enable phonetic matching
    .build();
```

### Numeric Fields
For range queries and sorting on numeric values.

```java
NumericFieldArgs priceField = NumericFieldArgs.builder()
    .name("price")
    .sortable()         // Enable sorting
    .noIndex()          // Don't index for search, only for sorting
    .build();
```

### Tag Fields
For exact matching and faceted search.

```java
TagFieldArgs categoryField = TagFieldArgs.builder()
    .name("category")
    .separator(",")     // Custom separator for multiple tags
    .sortable()
    .build();
```

### Geospatial Fields
For location-based queries.

```java
GeoFieldArgs locationField = GeoFieldArgs.builder()
    .name("location")
    .build();
```

### Vector Fields
For semantic search and similarity matching.

```java
VectorFieldArgs embeddingField = VectorFieldArgs.builder()
    .name("embedding")
    .algorithm(VectorFieldArgs.Algorithm.FLAT)
    .type(VectorFieldArgs.VectorType.FLOAT32)
    .dimensions(768)
    .distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
    .build();
```

## Advanced Index Configuration

### Index with Custom Settings

```java
CreateArgs createArgs = CreateArgs.builder()
    .on(CreateArgs.TargetType.HASH)            // Index HASH documents
    .withPrefix("product:")                    // Only index keys with this prefix
    .defaultLanguage(DocumentLanguage.ENGLISH) // Default language for text processing
    .languageField("lang")                     // Field containing document language
    .defaultScore(0.5)                         // Default document score
    .scoreField("popularity")                  // Field containing document score
    .maxTextFields()                           // Allow unlimited text fields
    .temporary(3600)                           // Auto-expire index after 1 hour
    .noOffsets()                               // Disable term offset storage
    .noHighlighting()                          // Disable highlighting
    .noFields()                                // Don't store field contents
    .noFrequency()                             // Don't store term frequencies
    .stopWords(Arrays.asList("the", "a", "an")) // Custom stopwords
    .build();

String result = search.ftCreate("advanced-idx", createArgs, fields);
```

### JSON Document Indexing

```java
CreateArgs jsonArgs = CreateArgs.builder()
    .on(CreateArgs.TargetType.JSON)
    .withPrefix("user:")
    .build();

List<FieldArgs> jsonFields = Arrays.asList(
    TextFieldArgs.builder().name("$.name").as("name").build(),
    NumericFieldArgs.builder().name("$.age").as("age").build(),
    TagFieldArgs.builder().name("$.tags[*]").as("tags").build()
);

search.ftCreate("users-idx", jsonArgs, jsonFields);
```

## Search Queries

!!! INFO
The Lettuce driver uses `DIALECT 2` by default for all search queries, unless configured otherwise. This is the recommended approach for new applications as all other dialects are [deprecated](https://redis.io/docs/latest/develop/ai/search-and-query/advanced-concepts/dialects/).

### Query Syntax

Redis Search supports a rich query language:

```java
// Simple term search
search.ftSearch("products-idx", "wireless");

// Phrase search
search.ftSearch("products-idx", "\"noise cancellation\"");

// Boolean operators
search.ftSearch("products-idx", "wireless AND headphones");
search.ftSearch("products-idx", "headphones OR earbuds");
search.ftSearch("products-idx", "audio -speakers");

// Field-specific search
search.ftSearch("products-idx", "@title:wireless @category:electronics");

// Wildcard and fuzzy search
search.ftSearch("products-idx", "wireles*");              // Prefix matching
search.ftSearch("products-idx", "%wireles%");             // Fuzzy matching

// Numeric range queries
search.ftSearch("products-idx", "@price:[100 200]");      // Inclusive range
search.ftSearch("products-idx", "@price:[(100 (200]");    // Exclusive bounds
search.ftSearch("products-idx", "@price:[100 +inf]");     // Open range
```

### Advanced Search Options

```java
SearchArgs<String> searchArgs = SearchArgs.<String>builder()
    .limit(0, 10)                                              // Pagination: offset 0, limit 10
    .sortBy(SortByArgs.builder().attribute("price").build())   // Sort by price ascending
    .returnField("title").returnField("price")                 // Only return specific fields
    .highlightField("title").highlightField("content")         // Highlight specific fields
    .highlightTags("<b>", "</b>")                              // Custom highlight tags
    .summarizeField("content")                                 // Summarize specific fields
    .summarizeFragments(3)                                     // Number of summary fragments
    .summarizeLen(50)                                          // Summary length
    .scorer(ScoringFunction.TF_IDF)                            // Scoring algorithm
    .withScores()                                              // Include document scores
    .noContent()                                               // Don't return document content
    .verbatim()                                                // Don't use stemming
    .withSortKeys()                                            // Include sort key values
    .inKey("product:1").inKey("product:2")                    // Search only specific keys
    .inField("title").inField("content")                      // Search only specific fields
    .slop(2)                                                   // Allow term reordering
    .timeout(Duration.ofSeconds(5))                             // Query timeout
    .param("category", "electronics")                          // Query parameter
    .dialect(QueryDialects.DIALECT2)                           // Query dialect version
    .build();

SearchReply<String> results = search.ftSearch("products-idx", "@title:$category", searchArgs);
```

## Vector Search

Vector search enables semantic similarity matching using machine learning embeddings.

### Creating a Vector Index

```java
List<FieldArgs> vectorFields = Arrays.asList(
    TextFieldArgs.builder().name("title").build(),
    VectorFieldArgs.builder()
        .name("embedding")
        .algorithm(VectorFieldArgs.Algorithm.FLAT) // or VectorFieldArgs.Algorithm.HNSW
        .type(VectorFieldArgs.VectorType.FLOAT32)
        .dimensions(768)                           // Vector dimension
        .distanceMetric(VectorFieldArgs.DistanceMetric.COSINE) // COSINE, L2, or IP
        .attribute("INITIAL_CAP", 1000)            // Initial vector capacity
        .build()
);

search.ftCreate("semantic-idx", vectorFields);
```

### Adding Vector Data

```java
// Convert text to embeddings (using your ML model)
float[] embedding = textToEmbedding("wireless headphones");
String embeddingStr = Arrays.toString(embedding);

Map<String, String> doc = new HashMap<>();
doc.put("title", "Wireless Headphones");
doc.put("embedding", embeddingStr);
redis.hmset("doc:1", doc);
```

### Vector Similarity Search

```java
// Find similar documents using vector search
float[] queryVector = textToEmbedding("bluetooth audio device");
String vectorQuery = "*=>[KNN 10 @embedding $query_vec AS score]";
ByteBuffer queryVectorBuffer = ByteBuffer.allocate(queryVector.length * Float.BYTES)
    .order(ByteOrder.LITTLE_ENDIAN);
for (float value : queryVector) {
    queryVectorBuffer.putFloat(value);
}

SearchArgs<String> vectorArgs = SearchArgs.<String>builder()
    .param("query_vec", queryVectorBuffer.array())
    .sortBy(SortByArgs.builder().attribute("score").build())
    .returnField("title").returnField("score")
    .dialect(QueryDialects.DIALECT2)
    .build();

SearchReply<String> results = search.ftSearch("semantic-idx", vectorQuery, vectorArgs);
```

## Geospatial Search

Search for documents based on geographic location.

### Creating a Geo Index

```java
List<FieldArgs> geoFields = Arrays.asList(
    TextFieldArgs.builder().name("name").build(),
    GeoFieldArgs.builder().name("location").build()
);

search.ftCreate("places-idx", geoFields);
```

### Adding Geo Data

```java
Map<String, String> place = new HashMap<>();
place.put("name", "Central Park");
place.put("location", "40.7829,-73.9654"); // lat,lon format
redis.hmset("place:1", place);
```

### Geo Queries

```java
// Find places within a 5 km radius
SearchReply<String> results = search.ftSearch("places-idx",
    "@location:[40.7829 -73.9654 5 km]");
```

## Aggregations

Aggregations provide powerful analytics capabilities for processing search results.

### Basic Aggregation

```java
// Simple aggregation without pipeline operations
AggregationReply<String> results = search.ftAggregate("products-idx", "*");
```

### Advanced Aggregation Pipeline

```java
AggregateArgs aggArgs = AggregateArgs.builder()
    // Load specific fields
    .load("title").load("price").load("category")

    // Apply transformations
    .apply("@price * 0.9", "discounted_price")

    // Filter results
    .filter("@price > 50")

    // Group by category with reducers
    .groupBy(AggregateArgs.GroupBy.of("category")
        .reduce(AggregateArgs.Reducer.count().as("product_count"))
        .reduce(AggregateArgs.Reducer.avg("@price").as("avg_price"))
        .reduce(AggregateArgs.Reducer.sum("@price").as("total_value"))
        .reduce(AggregateArgs.Reducer.min("@price").as("min_price"))
        .reduce(AggregateArgs.Reducer.max("@price").as("max_price")))

    // Sort results
    .sortBy("avg_price", AggregateArgs.SortDirection.DESC)

    // Limit results
    .limit(0, 10)

    // Apply final transformations
    .apply("@total_value / @product_count", "calculated_avg")

    // Set query parameters
    .verbatim()
    .timeout(Duration.ofSeconds(5))
    .param("min_price", "50")
    .dialect(QueryDialects.DIALECT2)
    .build();

AggregationReply<String> aggResults = search.ftAggregate("products-idx", "*", aggArgs);

// Process aggregation results
for (SearchReply<String> reply : aggResults.getReplies()) {
    for (SearchReply.SearchResult<String> result : reply.getResults()) {
        System.out.println("Category: " + result.getFields().get("category"));
        System.out.println("Count: " + result.getFields().get("product_count"));
        System.out.println("Avg Price: " + result.getFields().get("avg_price"));
    }
}
```

### Dynamic and Re-entrant Pipelines

Redis aggregations support dynamic pipelines where operations can be repeated and applied in any order:

```java
AggregateArgs complexPipeline = AggregateArgs.builder()
    // First transformation
    .apply("@price * @quantity", "total_value")

    // First filter
    .filter("@total_value > 100")

    // First grouping
    .groupBy(AggregateArgs.GroupBy.of("category")
        .reduce(AggregateArgs.Reducer.sum("@total_value").as("category_revenue")))

    // First sort
    .sortBy("category_revenue", AggregateArgs.SortDirection.DESC)

    // Second transformation
    .apply("@category_revenue / 1000", "revenue_k")

    // Second filter
    .filter("@revenue_k > 5")

    // Second grouping (re-entrant)
    .groupBy(AggregateArgs.GroupBy.of("revenue_k")
        .reduce(AggregateArgs.Reducer.count().as("high_revenue_categories")))

    // Second sort (re-entrant)
    .sortBy("high_revenue_categories", AggregateArgs.SortDirection.DESC)

    .build();
```

### Cursor-based Aggregation

For large result sets, use cursors to process data in batches:

```java
AggregateArgs cursorArgs = AggregateArgs.builder()
    .groupBy(AggregateArgs.GroupBy.of("category")
        .reduce(AggregateArgs.Reducer.count().as("count")))
    .withCursor(AggregateArgs.WithCursor.of(1000L, Duration.ofMinutes(5)))
    .build();

// Initial aggregation with cursor
AggregationReply<String> firstBatch = search.ftAggregate("products-idx", "*", cursorArgs);
AggregationReply.Cursor cursor = firstBatch.getCursor().orElse(null);

// Read subsequent batches
while (cursor != null && cursor.getCursorId() != 0) {
    AggregationReply<String> nextBatch = search.ftCursorread("products-idx", cursor, 500);
    cursor = nextBatch.getCursor().orElse(null);

    // Process batch
    processResults(nextBatch);
}
```

## Index Management

### Index Information and Statistics

```java
// Get index information
Map<String, Object> info = search.ftInfo("products-idx");
System.out.println("Index size: " + info.get("num_docs"));
System.out.println("Index memory: " + info.get("inverted_sz_mb") + " MB");

// List all indexes
List<String> indexes = search.ftList();
```

### Index Aliases

```java
// Create an alias for easier index management
search.ftAliasadd("products", "products-idx-v1");

// Update alias to point to new index version
search.ftAliasupdate("products", "products-idx-v2");

// Remove alias
search.ftAliasdel("products");
```

### Modifying Indexes

```java
// Add new fields to existing index
List<FieldArgs> newFields = Arrays.asList(
    TagFieldArgs.builder().name("brand").build(),
    NumericFieldArgs.builder().name("rating").build()
);

search.ftAlter("products-idx", false, newFields);  // false = scan existing docs
search.ftAlter("products-idx", true, newFields);   // true = skip initial scan
```

### Index Cleanup

```java
// Drop an index (keeps the data)
search.ftDropindex("products-idx");

// Drop an index and delete all associated documents
search.ftDropindex("products-idx", true);
```

## Auto-completion and Suggestions

Redis Search provides auto-completion functionality for building search-as-you-type features.

### Creating Suggestions

```java
// Add suggestions to a dictionary
search.ftSugadd("autocomplete", "wireless headphones", 1.0);
search.ftSugadd("autocomplete", "bluetooth speakers", 0.8);
search.ftSugadd("autocomplete", "noise cancelling earbuds", 0.9);

// Add with additional options
SugAddArgs sugArgs = SugAddArgs.Builder.incr() // Increment score if suggestion exists
    .payload("category:electronics");           // Additional metadata

search.ftSugadd("autocomplete", "gaming headset", 0.7, sugArgs);
```

### Getting Suggestions

```java
// Basic suggestion retrieval
List<Suggestion> suggestions = search.ftSugget("autocomplete", "head");

// Advanced suggestion options
SugGetArgs getArgs = SugGetArgs.Builder.fuzzy() // Enable fuzzy matching
    .max(5)         // Limit to 5 suggestions
    .withScores()   // Include scores
    .withPayloads(); // Include payloads

List<Suggestion> results = search.ftSugget("autocomplete", "head", getArgs);

for (Suggestion suggestion : results) {
    System.out.println("Suggestion: " + suggestion.getValue());
    System.out.println("Score: " + suggestion.getScore());
    System.out.println("Payload: " + suggestion.getPayload());
}
```

### Managing Suggestions

```java
// Get suggestion dictionary size
Long count = search.ftSuglen("autocomplete");

// Delete a suggestion
Boolean deleted = search.ftSugdel("autocomplete", "old suggestion");
```

## Spell Checking

Redis Search can suggest corrections for misspelled queries.

```java
// Basic spell check
SpellCheckResult corrections = search.ftSpellcheck("products-idx", "wireles hedphones");

// Advanced spell check with options
SpellCheckArgs spellArgs = SpellCheckArgs.Builder.distance(2) // Maximum Levenshtein distance
    .termsInclude("dictionary")                               // Include custom dictionary terms
    .termsExclude("stopwords")                                // Exclude stopword dictionary terms
    .dialect(2);

SpellCheckResult results = search.ftSpellcheck("products-idx", "wireles hedphones", spellArgs);

for (SpellCheckResult.MisspelledTerm term : results.getMisspelledTerms()) {
    System.out.println("Original: " + term.getTerm());
    for (SpellCheckResult.Suggestion suggestion : term.getSuggestions()) {
        System.out.println("  Suggestion: " + suggestion.getSuggestion() + " (score: " + suggestion.getScore() + ")");
    }
}
```

## Dictionary Management

Manage custom dictionaries for spell checking and synonyms.

```java
// Add terms to dictionary
search.ftDictadd("custom_dict", "smartphone", "tablet", "laptop");

// Remove terms from dictionary
search.ftDictdel("custom_dict", "outdated_term");

// Get all terms in dictionary
List<String> terms = search.ftDictdump("custom_dict");
```

## Synonym Management

Create synonym groups for query expansion.

```java
// Create synonym group
search.ftSynupdate("products-idx", "group1", "phone", "smartphone", "mobile");

// Update synonym group (replaces existing)
SynUpdateArgs synArgs = SynUpdateArgs.Builder.skipInitialScan(); // Don't reindex existing documents

search.ftSynupdate("products-idx", "group1", synArgs, "phone", "smartphone", "mobile", "cellphone");

// Get synonym groups
Map<String, List<String>> synonyms = search.ftSyndump("products-idx");
```

## Query Profiling and Debugging

### Query Explanation

Understand how Redis Search executes your queries:

```java
// Basic query explanation
String plan = search.ftExplain("products-idx", "@title:wireless");

// Detailed explanation with dialect
ExplainArgs explainArgs = ExplainArgs.Builder.dialect(QueryDialects.DIALECT2);

String detailedPlan = search.ftExplain("products-idx", "@title:wireless", explainArgs);
System.out.println("Execution plan: " + detailedPlan);
```

## Advanced Usage Patterns

### Multi-Index Search

Search across multiple indexes for federated queries:

```java
// Create specialized indexes
search.ftCreate("products-idx", productFields);
search.ftCreate("reviews-idx", reviewFields);

// Search each index separately and combine results
SearchReply<String> productResults = search.ftSearch("products-idx", "wireless");
SearchReply<String> reviewResults = search.ftSearch("reviews-idx", "wireless");

// Combine and process results as needed
```

### Index Versioning and Blue-Green Deployment

```java
// Create new index version
search.ftCreate("products-idx-v2", newFields);

// Populate new index with updated data
// ... data migration logic ...

// Switch alias to new index
search.ftAliasupdate("products", "products-idx-v2");

// Clean up old index after verification
search.ftDropindex("products-idx-v1");
```

### Conditional Indexing

```java
// Index only documents matching certain criteria
CreateArgs conditionalArgs = CreateArgs.builder()
    .on(CreateArgs.TargetType.HASH)
    .withPrefix("product:")
    .filter("@status=='active'")  // Only index active products
    .build();

search.ftCreate("active-products-idx", conditionalArgs, fields);
```

## Performance Optimization

### Index Design Best Practices

1. **Field Selection**: Only index fields you actually search on
2. **Text Field Optimization**: Use `NOOFFSETS`, `NOHL`, `NOFREQS` for memory savings
3. **Numeric Fields**: Use `NOINDEX` for sort-only fields
4. **Vector Fields**: Choose appropriate algorithm (FLAT vs HNSW) based on use case

```java
// Memory-optimized index options
CreateArgs memoryOptimizedArgs = CreateArgs.builder()
    .noOffsets()       // Disable position tracking
    .noHighlighting()  // Disable highlighting
    .noFrequency()     // Disable frequency tracking
    .build();

TextFieldArgs optimizedField = TextFieldArgs.builder()
    .name("description")
    .build();

// Sort-only numeric field
NumericFieldArgs sortField = NumericFieldArgs.builder()
    .name("timestamp")
    .sortable()
    .noIndex()      // Don't index for search
    .build();

search.ftCreate("memory-optimized-idx", memoryOptimizedArgs,
    Arrays.asList(optimizedField, sortField));
```

### Query Optimization

```java
// Use specific field searches instead of global search
search.ftSearch("idx", "@title:wireless");  // Better than "wireless"

// Use numeric ranges for better performance
search.ftSearch("idx", "@price:[100 200]"); // Better than "@price:>=100 @price:<=200"

// Limit result sets appropriately
SearchArgs<String> limitedArgs = SearchArgs.<String>builder()
    .limit(0, 20)   // Don't fetch more than needed
    .noContent()    // Skip content if only metadata needed
    .build();
```

## Error Handling and Troubleshooting

### Common Error Scenarios

```java
try {
    search.ftCreate("existing-idx", fields);
} catch (RedisCommandExecutionException e) {
    if (e.getMessage().contains("Index already exists")) {
        // Handle index already exists
        System.out.println("Index already exists, skipping creation");
    } else {
        throw e;
    }
}

try {
    SearchReply<String> results = search.ftSearch("idx", "invalid:query[");
} catch (RedisCommandExecutionException e) {
    if (e.getMessage().contains("Syntax error")) {
        // Handle query syntax error
        System.out.println("Invalid query syntax: " + e.getMessage());
    }
}
```

### Index Health Monitoring

```java
// Monitor index statistics
Map<String, Object> info = search.ftInfo("products-idx");
long numDocs = (Long) info.get("num_docs");
double memoryMB = (Double) info.get("inverted_sz_mb");

if (memoryMB > 1000) {  // Alert if index uses > 1GB
    System.out.println("Warning: Index memory usage is high: " + memoryMB + " MB");
}

// Check for indexing errors
List<String> errors = (List<String>) info.get("hash_indexing_failures");
if (!errors.isEmpty()) {
    System.out.println("Indexing errors detected: " + errors);
}
```

## Integration Examples

### Spring Boot Integration

```java
@Configuration
public class RedisSearchConfig {

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create("redis://localhost:6379");
    }

    @Bean
    public RediSearchCommands<String> rediSearchCommands(RedisClient client) {
        return client.connect().sync();
    }
}

@Service
public class ProductSearchService {

    @Autowired
    private RediSearchCommands<String> search;

    public List<Product> searchProducts(String query, int page, int size) {
        SearchArgs<String> args = SearchArgs.<String>builder()
            .limit(page * size, size)
            .build();

        SearchReply<String> results = search.ftSearch("products-idx", query, args);
        return convertToProducts(results);
    }
}
```

### Reactive Programming

```java
// Using reactive commands
StatefulRedisConnection<String, String> connection = redisClient.connect();
RediSearchReactiveCommands<String> reactiveSearch = connection.reactive();

Mono<SearchReply<String>> searchMono = reactiveSearch.ftSearch("products-idx", "wireless");

searchMono.subscribe(results -> {
    System.out.println("Found " + results.getCount() + " results");
    results.getResults().forEach(result ->
        System.out.println("Product: " + result.getFields().get("title"))
    );
});
```

## Migration and Compatibility

### Upgrading from RediSearch 1.x

When migrating from older RediSearch versions:

1. **Query Dialect**: Use `DIALECT 2` for new features
2. **Vector Fields**: Available in RediSearch 2.4+
3. **JSON Support**: Requires RedisJSON module for versions of Redis before 8.0
4. **Aggregation Cursors**: Available in RediSearch 2.0+

```java
// Ensure compatibility with modern features
SearchArgs<String> modernArgs = SearchArgs.<String>builder()
    .dialect(QueryDialects.DIALECT2)  // Use latest dialect
    .build();
```
