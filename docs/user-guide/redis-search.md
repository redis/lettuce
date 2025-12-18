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
RediSearchCommands<String, String> search = connection.sync();
```

### Creating Your First Index

```java
// Define searchable fields
List<FieldArgs<String>> fields = Arrays.asList(
    TextFieldArgs.<String>builder().name("title").build(),
    TextFieldArgs.<String>builder().name("content").build(),
    NumericFieldArgs.<String>builder().name("price").sortable().build(),
    TagFieldArgs.<String>builder().name("category").sortable().build()
);

// Create the index
String result = search.ftCreate("products-idx", fields);
// Returns: "OK"
```

### Adding Data

```java
// Add documents as Redis hashes
Map<String, String> product1 = Map.of(
    "title", "Wireless Headphones",
    "content", "High-quality wireless headphones with noise cancellation",
    "price", "199.99",
    "category", "electronics"
);
redis.hmset("product:1", product1);

Map<String, String> product2 = Map.of(
    "title", "Running Shoes",
    "content", "Comfortable running shoes for daily exercise",
    "price", "89.99", 
    "category", "sports"
);
redis.hmset("product:2", product2);
```

### Basic Search

```java
// Simple text search
SearchReply<String, String> results = search.ftSearch("products-idx", "wireless");

// Access results
System.out.println("Found " + results.getCount() + " documents");
for (SearchReply.SearchResult<String, String> result : results.getResults()) {
    System.out.println("Key: " + result.getKey());
    System.out.println("Title: " + result.getFields().get("title"));
}
```

## Field Types and Indexing

### Text Fields
Full-text searchable fields with stemming, phonetic matching, and scoring.

```java
TextFieldArgs<String> titleField = TextFieldArgs.<String>builder()
    .name("title")
    .weight(2.0)                                      // Boost importance in scoring
    .sortable()                                       // Enable sorting
    .noStem()                                         // Disable stemming
    .phonetic(TextFieldArgs.PhoneticMatcher.ENGLISH)  // Enable phonetic matching
    .build();
```

### Numeric Fields
For range queries and sorting on numeric values.

```java
NumericFieldArgs<String> priceField = NumericFieldArgs.<String>builder()
    .name("price")
    .sortable()         // Enable sorting
    .noIndex()          // Don't index for search, only for sorting
    .build();
```

### Tag Fields
For exact matching and faceted search.

```java
TagFieldArgs<String> categoryField = TagFieldArgs.<String>builder()
    .name("category")
    .separator(",")     // Custom separator for multiple tags
    .sortable()
    .build();
```

### Geospatial Fields
For location-based queries.

```java
GeoFieldArgs<String> locationField = GeoFieldArgs.<String>builder()
    .name("location")
    .build();
```

### Vector Fields
For semantic search and similarity matching.

```java
VectorFieldArgs<String> embeddingField = VectorFieldArgs.<String>builder()
    .name("embedding")
    .algorithm(VectorAlgorithm.FLAT)
    .type(VectorType.FLOAT32)
    .dimension(768)
    .distanceMetric(DistanceMetric.COSINE)
    .build();
```

## Advanced Index Configuration

### Index with Custom Settings

```java
CreateArgs<String, String> createArgs = CreateArgs.<String, String>builder()
    .on(CreateArgs.TargetType.HASH)            // Index HASH documents
    .withPrefix("product:")                    // Only index keys with this prefix
    .language("english")                       // Default language for text processing
    .languageField("lang")                     // Field containing document language
    .score(0.5)                                // Default document score
    .scoreField("popularity")                  // Field containing document score
    .maxTextFields()                           // Allow unlimited text fields
    .temporary(3600)                           // Auto-expire index after 1 hour
    .noOffsets()                               // Disable term offset storage
    .noHighlighting()                          // Disable highlighting
    .noFields()                                // Don't store field contents
    .noFreqs()                                 // Don't store term frequencies
    .stopwords("the", "a", "an")               // Custom stopwords
    .build();

String result = search.ftCreate("advanced-idx", createArgs, fields);
```

### JSON Document Indexing

```java
CreateArgs<String, String> jsonArgs = CreateArgs.<String, String>builder()
    .on(CreateArgs.TargetType.JSON)
    .prefix("user:")
    .build();

List<FieldArgs<String>> jsonFields = Arrays.asList(
    TextFieldArgs.<String>builder().name("$.name").as("name").build(),
    NumericFieldArgs.<String>builder().name("$.age").as("age").build(),
    TagFieldArgs.<String>builder().name("$.tags[*]").as("tags").build()
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
SearchArgs<String, String> searchArgs = SearchArgs.<String, String>builder()
    .limit(0, 10)                                              // Pagination: offset 0, limit 10
    .sortBy("price", SortDirection.ASC)                        // Sort by price ascending
    .returnFields("title", "price")                            // Only return specific fields
    .highlightFields("title", "content")                       // Highlight specific fields
    .highlightTags("<b>", "</b>")                              // Custom highlight tags
    .summarizeFields("content")                                // Summarize specific fields
    .summarizeFrags(3)                                         // Number of summary fragments
    .summarizeLen(50)                                          // Summary length
    .scorer(ScoringFunction.TF_IDF)                            // Scoring algorithm
    .explainScore()                                            // Include score explanation
    .withScores()                                              // Include document scores
    .noContent()                                               // Don't return document content
    .verbatim()                                                // Don't use stemming
    .noStopwords()                                             // Don't filter stopwords
    .withSortKeys()                                            // Include sort key values
    .inKeys("product:1", "product:2")                          // Search only specific keys
    .inFields("title", "content")                              // Search only specific fields
    .slop(2)                                                   // Allow term reordering
    .timeout(5000)                                             // Query timeout in milliseconds
    .params("category", "electronics")                         // Query parameters
    .dialect(QueryDialects.DIALECT2)                           // Query dialect version
    .build();

SearchReply<String, String> results = search.ftSearch("products-idx", "@title:$category", searchArgs);
```

## Vector Search

Vector search enables semantic similarity matching using machine learning embeddings.

### Creating a Vector Index

```java
List<FieldArgs<String>> vectorFields = Arrays.asList(
    TextFieldArgs.<String>builder().name("title").build(),
    VectorFieldArgs.<String>builder()
        .name("embedding")
        .algorithm(VectorAlgorithm.FLAT)      // or VectorAlgorithm.HNSW
        .type(VectorType.FLOAT32)
        .dimension(768)                       // Vector dimension
        .distanceMetric(DistanceMetric.COSINE) // COSINE, L2, or IP
        .initialCapacity(1000)                // Initial vector capacity
        .build()
);

search.ftCreate("semantic-idx", vectorFields);
```

### Adding Vector Data

```java
// Convert text to embeddings (using your ML model)
float[] embedding = textToEmbedding("wireless headphones");
String embeddingStr = Arrays.toString(embedding);

Map<String, String> doc = Map.of(
    "title", "Wireless Headphones",
    "embedding", embeddingStr
);
redis.hmset("doc:1", doc);
```

### Vector Similarity Search

```java
// Find similar documents using vector search
float[] queryVector = textToEmbedding("bluetooth audio device");
String vectorQuery = "*=>[KNN 10 @embedding $query_vec AS score]";

SearchArgs<String, String> vectorArgs = SearchArgs.<String, String>builder()
    .params("query_vec", Arrays.toString(queryVector))
    .sortBy("score", SortDirection.ASC)
    .returnFields("title", "score")
    .dialect(QueryDialects.DIALECT2)
    .build();

SearchReply<String, String> results = search.ftSearch("semantic-idx", vectorQuery, vectorArgs);
```

## Geospatial Search

Search for documents based on geographic location.

### Creating a Geo Index

```java
List<FieldArgs<String>> geoFields = Arrays.asList(
    TextFieldArgs.<String>builder().name("name").build(),
    GeoFieldArgs.<String>builder().name("location").build()
);

search.ftCreate("places-idx", geoFields);
```

### Adding Geo Data

```java
Map<String, String> place = Map.of(
    "name", "Central Park",
    "location", "40.7829,-73.9654"  // lat,lon format
);
redis.hmset("place:1", place);
```

### Geo Queries

```java
// Find places within radius
SearchArgs<String, String> geoArgs = SearchArgs.<String, String>builder()
    .geoFilter("location", 40.7829, -73.9654, 5, GeoUnit.KM)
    .build();

SearchReply<String, String> nearbyPlaces = search.ftSearch("places-idx", "*", geoArgs);

// Geo query in search string
SearchReply<String, String> results = search.ftSearch("places-idx",
    "@location:[40.7829 -73.9654 5 km]");
```

## Aggregations

Aggregations provide powerful analytics capabilities for processing search results.

### Basic Aggregation

```java
// Simple aggregation without pipeline operations
AggregationReply<String, String> results = search.ftAggregate("products-idx", "*");
```

### Advanced Aggregation Pipeline

```java
AggregateArgs<String, String> aggArgs = AggregateArgs.<String, String>builder()
    // Load specific fields
    .load("title").load("price").load("category")

    // Apply transformations
    .apply("@price * 0.9", "discounted_price")

    // Filter results
    .filter("@price > 50")

    // Group by category with reducers
    .groupBy(GroupBy.<String, String>of("category")
        .reduce(Reducer.<String, String>count().as("product_count"))
        .reduce(Reducer.<String, String>avg("@price").as("avg_price"))
        .reduce(Reducer.<String, String>sum("@price").as("total_value"))
        .reduce(Reducer.<String, String>min("@price").as("min_price"))
        .reduce(Reducer.<String, String>max("@price").as("max_price")))

    // Sort results
    .sortBy("avg_price", SortDirection.DESC)

    // Limit results
    .limit(0, 10)

    // Apply final transformations
    .apply("@total_value / @product_count", "calculated_avg")

    // Set query parameters
    .verbatim()
    .timeout(5000)
    .params("min_price", "50")
    .dialect(QueryDialects.DIALECT2)
    .build();

AggregationReply<String, String> aggResults = search.ftAggregate("products-idx", "*", aggArgs);

// Process aggregation results
for (SearchReply<String, String> reply : aggResults.getReplies()) {
    for (SearchReply.SearchResult<String, String> result : reply.getResults()) {
        System.out.println("Category: " + result.getFields().get("category"));
        System.out.println("Count: " + result.getFields().get("product_count"));
        System.out.println("Avg Price: " + result.getFields().get("avg_price"));
    }
}
```

### Dynamic and Re-entrant Pipelines

Redis aggregations support dynamic pipelines where operations can be repeated and applied in any order:

```java
AggregateArgs<String, String> complexPipeline = AggregateArgs.<String, String>builder()
    // First transformation
    .apply("@price * @quantity", "total_value")

    // First filter
    .filter("@total_value > 100")

    // First grouping
    .groupBy(GroupBy.<String, String>of("category")
        .reduce(Reducer.<String, String>sum("@total_value").as("category_revenue")))

    // First sort
    .sortBy("category_revenue", SortDirection.DESC)

    // Second transformation
    .apply("@category_revenue / 1000", "revenue_k")

    // Second filter
    .filter("@revenue_k > 5")

    // Second grouping (re-entrant)
    .groupBy(GroupBy.<String, String>of("revenue_k")
        .reduce(Reducer.<String, String>count().as("high_revenue_categories")))

    // Second sort (re-entrant)
    .sortBy("high_revenue_categories", SortDirection.DESC)

    .build();
```

### Cursor-based Aggregation

For large result sets, use cursors to process data in batches:

```java
AggregateArgs<String, String> cursorArgs = AggregateArgs.<String, String>builder()
    .groupBy(GroupBy.<String, String>of("category")
        .reduce(Reducer.<String, String>count().as("count")))
    .withCursor()
    .withCursor(1000, 300000)  // batch size: 1000, timeout: 5 minutes
    .build();

// Initial aggregation with cursor
AggregationReply<String, String> firstBatch = search.ftAggregate("products-idx", "*", cursorArgs);
long cursorId = firstBatch.getCursorId();

// Read subsequent batches
while (cursorId != 0) {
    AggregationReply<String, String> nextBatch = search.ftCursorread("products-idx", cursorId, 500);
    cursorId = nextBatch.getCursorId();

    // Process batch
    processResults(nextBatch);
}

// Clean up cursor when done
search.ftCursordel("products-idx", cursorId);
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
List<FieldArgs<String>> newFields = Arrays.asList(
    TagFieldArgs.<String>builder().name("brand").build(),
    NumericFieldArgs.<String>builder().name("rating").build()
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
SugAddArgs sugArgs = SugAddArgs.builder()
    .increment()    // Increment score if suggestion exists
    .payload("category:electronics")  // Additional metadata
    .build();

search.ftSugadd("autocomplete", "gaming headset", 0.7, sugArgs);
```

### Getting Suggestions

```java
// Basic suggestion retrieval
List<Suggestion<String>> suggestions = search.ftSugget("autocomplete", "head");

// Advanced suggestion options
SugGetArgs getArgs = SugGetArgs.builder()
    .fuzzy()        // Enable fuzzy matching
    .max(5)         // Limit to 5 suggestions
    .withScores()   // Include scores
    .withPayloads() // Include payloads
    .build();

List<Suggestion<String>> results = search.ftSugget("autocomplete", "head", getArgs);

for (Suggestion<String> suggestion : results) {
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
List<SpellCheckResult<String>> corrections = search.ftSpellcheck("products-idx", "wireles hedphones");

// Advanced spell check with options
SpellCheckArgs<String, String> spellArgs = SpellCheckArgs.<String, String>builder()
    .distance(2)                    // Maximum Levenshtein distance
    .terms("include", "dictionary") // Include terms from dictionary
    .terms("exclude", "stopwords")  // Exclude stopwords
    .dialect(QueryDialects.DIALECT2)
    .build();

List<SpellCheckResult<String>> results = search.ftSpellcheck("products-idx", "wireles hedphones", spellArgs);

for (SpellCheckResult<String> result : results) {
    System.out.println("Original: " + result.getTerm());
    for (SpellCheckResult.Suggestion<String> suggestion : result.getSuggestions()) {
        System.out.println("  Suggestion: " + suggestion.getValue() + " (score: " + suggestion.getScore() + ")");
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
SynUpdateArgs synArgs = SynUpdateArgs.builder()
    .skipInitialScan()  // Don't reindex existing documents
    .build();

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
ExplainArgs<String, String> explainArgs = ExplainArgs.<String, String>builder()
    .dialect(QueryDialects.DIALECT2)
    .build();

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
SearchReply<String, String> productResults = search.ftSearch("products-idx", "wireless");
SearchReply<String, String> reviewResults = search.ftSearch("reviews-idx", "wireless");

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
CreateArgs<String, String> conditionalArgs = CreateArgs.<String, String>builder()
    .on(CreateArgs.TargetType.HASH)
    .prefix("product:")
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
// Memory-optimized text field
TextFieldArgs<String> optimizedField = TextFieldArgs.<String>builder()
    .name("description")
    .noOffsets()    // Disable position tracking
    .noHL()         // Disable highlighting
    .noFreqs()      // Disable frequency tracking
    .build();

// Sort-only numeric field
NumericFieldArgs<String> sortField = NumericFieldArgs.<String>builder()
    .name("timestamp")
    .sortable()
    .noIndex()      // Don't index for search
    .build();
```

### Query Optimization

```java
// Use specific field searches instead of global search
search.ftSearch("idx", "@title:wireless");  // Better than "wireless"

// Use numeric ranges for better performance
search.ftSearch("idx", "@price:[100 200]"); // Better than "@price:>=100 @price:<=200"

// Limit result sets appropriately
SearchArgs<String, String> limitedArgs = SearchArgs.<String, String>builder()
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
    SearchReply<String, String> results = search.ftSearch("idx", "invalid:query[");
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
    public RediSearchCommands<String, String> rediSearchCommands(RedisClient client) {
        return client.connect().sync();
    }
}

@Service
public class ProductSearchService {

    @Autowired
    private RediSearchCommands<String, String> search;

    public List<Product> searchProducts(String query, int page, int size) {
        SearchArgs<String, String> args = SearchArgs.<String, String>builder()
            .limit(page * size, size)
            .build();

        SearchReply<String, String> results = search.ftSearch("products-idx", query, args);
        return convertToProducts(results);
    }
}
```

### Reactive Programming

```java
// Using reactive commands
StatefulRedisConnection<String, String> connection = redisClient.connect();
RediSearchReactiveCommands<String, String> reactiveSearch = connection.reactive();

Mono<SearchReply<String, String>> searchMono = reactiveSearch.ftSearch("products-idx", "wireless");

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
SearchArgs<String, String> modernArgs = SearchArgs.<String, String>builder()
    .dialect(QueryDialects.DIALECT2)  // Use latest dialect
    .build();
```
