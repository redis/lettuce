# Redis Vector Sets support in Lettuce

Lettuce supports [Redis Vector Sets](https://redis.io/docs/latest/develop/data-types/vector-sets/) starting from [Lettuce 6.7.0.RELEASE](https://github.com/redis/lettuce/releases/tag/6.7.0.RELEASE).

Redis Vector Sets are a new data type designed for efficient vector similarity search. Inspired by Redis sorted sets, vector sets store elements with associated high-dimensional vectors instead of scores, enabling fast similarity queries for AI and machine learning applications.

!!! INFO
    Vector Sets are currently in preview and available in Redis 8 Community Edition. The API may undergo changes in future releases based on community feedback.

!!! WARNING
    Vector Sets commands are marked as `@Experimental` in Lettuce 6.7 and may undergo API changes in future releases. The underlying Redis Vector Sets functionality is stable and production-ready.

## Core Concepts

Vector Sets extend the concept of Redis sorted sets by:

- **Vector Storage**: Elements are associated with high-dimensional vectors instead of numeric scores
- **Similarity Search**: Find elements most similar to a query vector or existing element
- **Quantization**: Automatic vector compression to optimize memory usage
- **Filtering**: Associate JSON attributes with elements for filtered similarity search
- **Dimensionality Reduction**: Reduce vector dimensions using random projection

## Getting Started

### Basic Setup

```java
RedisURI redisURI = RedisURI.Builder.redis("localhost").withPort(6379).build();
RedisClient redisClient = RedisClient.create(redisURI);
StatefulRedisConnection<String, String> connection = redisClient.connect();
RedisVectorSetCommands<String, String> vectorSet = connection.sync();
```

### Creating Your First Vector Set

```java
// Add vectors to a vector set (creates the set if it doesn't exist)
Boolean result1 = vectorSet.vadd("points", "pt:A", 1.0, 1.0);
Boolean result2 = vectorSet.vadd("points", "pt:B", -1.0, -1.0);
Boolean result3 = vectorSet.vadd("points", "pt:C", -1.0, 1.0);
Boolean result4 = vectorSet.vadd("points", "pt:D", 1.0, -1.0);
Boolean result5 = vectorSet.vadd("points", "pt:E", 1.0, 0.0);

System.out.println("Added 5 points to vector set");
```

### Basic Vector Set Operations

```java
// Get the number of elements in the vector set
Long cardinality = vectorSet.vcard("points");
System.out.println("Vector set contains: " + cardinality + " elements");

// Get the dimensionality of vectors in the set
Long dimensions = vectorSet.vdim("points");
System.out.println("Vector dimensionality: " + dimensions);

// Check if the key is a vector set
String type = redis.type("points");
System.out.println("Data type: " + type); // Returns "vectorset"
```

## Vector Operations

### Adding Vectors

```java
// Basic vector addition
Boolean added = vectorSet.vadd("embeddings", "doc:1", 0.1, 0.2, 0.3, 0.4);

// Add vector with specific dimensionality
Boolean addedWithDim = vectorSet.vadd("embeddings", 4, "doc:2", 0.5, 0.6, 0.7, 0.8);

// Add vector with advanced options
VAddArgs args = VAddArgs.Builder
    .quantizationType(QuantizationType.Q8)
    .build();
Boolean addedWithArgs = vectorSet.vadd("embeddings", 4, "doc:3", args, 0.9, 1.0, 1.1, 1.2);
```

### Retrieving Vectors

```java
// Get the approximate vector for an element
List<Double> vector = vectorSet.vemb("points", "pt:A");
System.out.println("Vector for pt:A: " + vector);

// Get raw vector data (more efficient for large vectors)
RawVector rawVector = vectorSet.vembRaw("points", "pt:A");
```

### Removing Vectors

```java
// Remove an element from the vector set
Boolean removed = vectorSet.vrem("points", "pt:A");
System.out.println("Element removed: " + removed);
```

## Vector Similarity Search

### Basic Similarity Search

```java
// Find elements most similar to a query vector
List<String> similar = vectorSet.vsim("points", 0.9, 0.1);
System.out.println("Most similar elements: " + similar);

// Find elements similar to an existing element
List<String> similarToElement = vectorSet.vsim("points", "pt:A");
System.out.println("Elements similar to pt:A: " + similarToElement);
```

### Advanced Similarity Search

```java
// Similarity search with scores and options
VSimArgs simArgs = VSimArgs.Builder
    .count(5)                    // Return top 5 results
    .explorationFactor(200)      // Search exploration factor
    .build();

Map<String, Double> resultsWithScores = vectorSet.vsimWithScore("points", simArgs, 0.9, 0.1);
resultsWithScores.forEach((element, score) ->
    System.out.println(element + ": " + score));
```

### Similarity Cutoff with EPSILON

Use EPSILON to apply a maximum distance cutoff so that only sufficiently similar results are returned.
The cutoff is defined as a distance threshold epsilon in [0.0, 1.0]; results must have similarity ≥ 1 − epsilon.
Smaller epsilon values yield fewer, more similar results.

```java
VSimArgs simArgs = VSimArgs.Builder
    .count(10)
    .epsilon(0.2)            // distance cutoff; results have similarity >= 0.8
    .build();

Map<String, Double> results = vectorSet.vsimWithScore("points", simArgs, 0.9, 0.1);
```


### Including Attributes in Results with WITHATTRIBS

Attributes are included by using the API variant that emits WITHATTRIBS. Use vsimWithScoreWithAttribs(...) to obtain scores and attributes per element.

```java
VSimArgs args = VSimArgs.Builder
    .count(10)
    .epsilon(0.2)
    .build();

Map<String, VSimScoreAttribs> results = vectorSet.vsimWithScoreWithAttribs("points", args, 0.9, 0.1);
results.forEach((element, sa) -> {
    double score = sa.getScore();
    String attrs = sa.getAttributes();
    System.out.println(element + ": score=" + score + ", attrs=" + attrs);
});
```

Note: WITHATTRIBS requires a Redis version that supports returning attributes (Redis 8.2+). Methods are marked @Experimental and subject to change.


## Element Attributes and Filtering

### Setting and Getting Attributes

```java
// Set JSON attributes for an element
String attributes = "{\"category\": \"electronics\", \"price\": 299.99, \"brand\": \"TechCorp\"}";
Boolean attrSet = vectorSet.vsetattr("products", "item:1", attributes);

// Get attributes for an element
String retrievedAttrs = vectorSet.vgetattr("products", "item:1");
System.out.println("Attributes: " + retrievedAttrs);

// Clear all attributes for an element
Boolean cleared = vectorSet.vClearAttributes("products", "item:1");
```

### Filtered Similarity Search

```java
// Add elements with attributes
vectorSet.vadd("products", "laptop:1", 0.1, 0.2, 0.3);
vectorSet.vsetattr("products", "laptop:1", "{\"category\": \"electronics\", \"price\": 999.99}");

vectorSet.vadd("products", "phone:1", 0.4, 0.5, 0.6);
vectorSet.vsetattr("products", "phone:1", "{\"category\": \"electronics\", \"price\": 599.99}");

vectorSet.vadd("products", "book:1", 0.7, 0.8, 0.9);
vectorSet.vsetattr("products", "book:1", "{\"category\": \"books\", \"price\": 29.99}");

// Search with attribute filtering
VSimArgs filterArgs = VSimArgs.Builder
    .filter(".category == \"electronics\" && .price > 500")
    .count(10)
    .build();

List<String> filteredResults = vectorSet.vsim("products", filterArgs, 0.2, 0.3, 0.4);
System.out.println("Filtered results: " + filteredResults);
```

## Advanced Features

### Quantization Options

Vector Sets support different quantization methods to optimize memory usage:

```java
// No quantization (highest precision, most memory)
VAddArgs noQuantArgs = VAddArgs.Builder
    .quantizationType(QuantizationType.NO_QUANTIZATION)
    .build();
vectorSet.vadd("precise_vectors", "element:1", noQuantArgs, 1.262185, 1.958231);

// 8-bit quantization (default, good balance)
VAddArgs q8Args = VAddArgs.Builder
    .quantizationType(QuantizationType.Q8)
    .build();
vectorSet.vadd("balanced_vectors", "element:1", q8Args, 1.262185, 1.958231);

// Binary quantization (lowest memory, fastest search)
VAddArgs binaryArgs = VAddArgs.Builder
    .quantizationType(QuantizationType.BINARY)
    .build();
vectorSet.vadd("binary_vectors", "element:1", binaryArgs, 1.262185, 1.958231);

// Compare the results
List<Double> precise = vectorSet.vemb("precise_vectors", "element:1");
List<Double> balanced = vectorSet.vemb("balanced_vectors", "element:1");
List<Double> binary = vectorSet.vemb("binary_vectors", "element:1");

System.out.println("Precise: " + precise);
System.out.println("Balanced: " + balanced);
System.out.println("Binary: " + binary);
```

### Dimensionality Reduction

```java
// Create a high-dimensional vector (300 dimensions)
Double[] highDimVector = new Double[300];
for (int i = 0; i < 300; i++) {
    highDimVector[i] = (double) i / 299;
}

// Add without reduction
vectorSet.vadd("full_vectors", "element:1", highDimVector);
Long fullDim = vectorSet.vdim("full_vectors");
System.out.println("Full dimensions: " + fullDim); // 300

// Add with dimensionality reduction to 100 dimensions
vectorSet.vadd("reduced_vectors", 100, "element:1", highDimVector);
Long reducedDim = vectorSet.vdim("reduced_vectors");
System.out.println("Reduced dimensions: " + reducedDim); // 100
```

### Random Sampling

```java
// Get random elements from the vector set
List<String> randomElements = vectorSet.vrandmember("points", 3);
System.out.println("Random elements: " + randomElements);
```

## Vector Set Metadata and Inspection

### Getting Vector Set Information

```java
// Get detailed information about the vector set
VectorMetadata metadata = vectorSet.vinfo("points");
System.out.println("Vector set metadata: " + metadata);

// Get links/connections for HNSW graph structure
List<String> links = vectorSet.vlinks("points", "pt:A");
System.out.println("Graph links for pt:A: " + links);
```

## Real-World Use Cases

### Semantic Search Application

```java
public class SemanticSearchService {
    private final RedisVectorSetCommands<String, String> vectorSet;

    public SemanticSearchService(RedisVectorSetCommands<String, String> vectorSet) {
        this.vectorSet = vectorSet;
    }

    // Add document with embedding and metadata
    public void addDocument(String docId, double[] embedding, String title, String category) {
        // Add vector to set
        vectorSet.vadd("documents", docId, embedding);

        // Add metadata as attributes
        String attributes = String.format(
            "{\"title\": \"%s\", \"category\": \"%s\", \"timestamp\": %d}",
            title, category, System.currentTimeMillis()
        );
        vectorSet.vsetattr("documents", docId, attributes);
    }

    // Search for similar documents with optional filtering
    public List<String> searchSimilar(double[] queryEmbedding, String categoryFilter, int limit) {
        VSimArgs args = VSimArgs.Builder
            .count(limit)
            .filter(categoryFilter != null ? ".category == \"" + categoryFilter + "\"" : null)
            .build();

        return vectorSet.vsim("documents", args, queryEmbedding);
    }

    // Get document details
    public DocumentInfo getDocument(String docId) {
        List<Double> embedding = vectorSet.vemb("documents", docId);
        String attributes = vectorSet.vgetattr("documents", docId);
        return new DocumentInfo(docId, embedding, attributes);
    }
}
```

### Recommendation System

```java
public class RecommendationEngine {
    private final RedisVectorSetCommands<String, String> vectorSet;

    public RecommendationEngine(RedisVectorSetCommands<String, String> vectorSet) {
        this.vectorSet = vectorSet;
    }

    // Add user profile with preferences vector
    public void addUserProfile(String userId, double[] preferencesVector, Map<String, Object> profile) {
        // Use quantization for memory efficiency
        VAddArgs args = VAddArgs.Builder
            .quantizationType(QuantizationType.Q8)
            .build();

        vectorSet.vadd("user_profiles", userId, args, preferencesVector);

        // Store user metadata
        String attributes = convertToJson(profile);
        vectorSet.vsetattr("user_profiles", userId, attributes);
    }

    // Find similar users for collaborative filtering
    public List<String> findSimilarUsers(String userId, int count) {
        VSimArgs args = VSimArgs.Builder
            .count(count + 1) // +1 to exclude the user themselves
            .build();

        List<String> similar = vectorSet.vsim("user_profiles", args, userId);
        similar.remove(userId); // Remove the user from their own recommendations
        return similar;
    }

    // Get recommendations based on user similarity
    public Map<String, Double> getRecommendations(String userId) {
        VSimArgs args = VSimArgs.Builder
            .count(10)
            .build();

        return vectorSet.vsimWithScore("user_profiles", args, userId);
    }

    private String convertToJson(Map<String, Object> data) {
        // Convert map to JSON string (implementation depends on your JSON library)
        return "{}"; // Placeholder
    }
}
```

### Image Similarity Search

```java
public class ImageSearchService {
    private final RedisVectorSetCommands<String, String> vectorSet;

    public ImageSearchService(RedisVectorSetCommands<String, String> vectorSet) {
        this.vectorSet = vectorSet;
    }

    // Add image with feature vector and metadata
    public void indexImage(String imageId, float[] featureVector, String category,
                          int width, int height, String format) {
        // Convert float array to Double array
        Double[] vector = Arrays.stream(featureVector)
            .mapToDouble(f -> (double) f)
            .boxed()
            .toArray(Double[]::new);

        // Use binary quantization for fast similarity search
        VAddArgs args = VAddArgs.Builder
            .quantizationType(QuantizationType.BINARY)
            .build();

        vectorSet.vadd("images", imageId, args, vector);

        // Store image metadata
        String attributes = String.format(
            "{\"category\": \"%s\", \"width\": %d, \"height\": %d, \"format\": \"%s\"}",
            category, width, height, format
        );
        vectorSet.vsetattr("images", imageId, attributes);
    }

    // Find visually similar images
    public List<SimilarImage> findSimilarImages(String imageId, String categoryFilter, int limit) {
        VSimArgs.Builder argsBuilder = VSimArgs.Builder.count(limit);

        if (categoryFilter != null) {
            argsBuilder.filter(".category == \"" + categoryFilter + "\"");
        }

        Map<String, Double> results = vectorSet.vsimWithScore("images", argsBuilder.build(), imageId);

        return results.entrySet().stream()
            .map(entry -> new SimilarImage(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    public static class SimilarImage {
        public final String imageId;
        public final double similarity;

        public SimilarImage(String imageId, double similarity) {
            this.imageId = imageId;
            this.similarity = similarity;
        }
    }
}
```

## Performance Optimization

### Memory Optimization

```java
// Choose appropriate quantization based on your needs
public class VectorSetOptimizer {

    // For high-precision applications (e.g., scientific computing)
    public void addHighPrecisionVector(RedisVectorSetCommands<String, String> vectorSet,
                                     String key, String element, double[] vector) {
        VAddArgs args = VAddArgs.Builder
            .quantizationType(QuantizationType.NO_QUANTIZATION)
            .build();
        vectorSet.vadd(key, element, args, vector);
    }

    // For balanced performance and memory usage (recommended default)
    public void addBalancedVector(RedisVectorSetCommands<String, String> vectorSet,
                                String key, String element, double[] vector) {
        VAddArgs args = VAddArgs.Builder
            .quantizationType(QuantizationType.Q8)
            .build();
        vectorSet.vadd(key, element, args, vector);
    }

    // For maximum speed and minimum memory (e.g., large-scale similarity search)
    public void addFastVector(RedisVectorSetCommands<String, String> vectorSet,
                            String key, String element, double[] vector) {
        VAddArgs args = VAddArgs.Builder
            .quantizationType(QuantizationType.BINARY)
            .build();
        vectorSet.vadd(key, element, args, vector);
    }
}
```

### Search Performance Tuning

```java
// Optimize similarity search performance
public class SearchOptimizer {

    // For high-recall searches (more thorough but slower)
    public List<String> highRecallSearch(RedisVectorSetCommands<String, String> vectorSet,
                                       String key, double[] query, int count) {
        VSimArgs args = VSimArgs.Builder
            .count(count)
            .explorationFactor(500) // Higher exploration for better recall
            .build();
        return vectorSet.vsim(key, args, query);
    }

    // For fast searches (lower recall but much faster)
    public List<String> fastSearch(RedisVectorSetCommands<String, String> vectorSet,
                                 String key, double[] query, int count) {
        VSimArgs args = VSimArgs.Builder
            .count(count)
            .explorationFactor(50) // Lower exploration for speed
            .build();
        return vectorSet.vsim(key, args, query);
    }

    // Batch similarity searches for efficiency
    public Map<String, List<String>> batchSearch(RedisVectorSetCommands<String, String> vectorSet,
                                                String key, List<double[]> queries, int count) {
        Map<String, List<String>> results = new HashMap<>();

        VSimArgs args = VSimArgs.Builder
            .count(count)
            .build();

        for (int i = 0; i < queries.size(); i++) {
            String queryId = "query_" + i;
            List<String> similar = vectorSet.vsim(key, args, queries.get(i));
            results.put(queryId, similar);
        }

        return results;
    }
}
```

## Error Handling and Best Practices

### Common Error Scenarios

```java
public class VectorSetErrorHandler {

    public void handleCommonErrors(RedisVectorSetCommands<String, String> vectorSet) {
        try {
            // Attempt to add vector to non-existent key
            vectorSet.vadd("my_vectors", "element:1", 1.0, 2.0, 3.0);

        } catch (RedisCommandExecutionException e) {
            if (e.getMessage().contains("WRONGTYPE")) {
                System.err.println("Key exists but is not a vector set");
                // Handle type mismatch
            } else if (e.getMessage().contains("dimension mismatch")) {
                System.err.println("Vector dimension doesn't match existing vectors");
                // Handle dimension mismatch
            }
        }

        try {
            // Attempt to get vector from non-existent element
            List<Double> vector = vectorSet.vemb("my_vectors", "non_existent");
            if (vector == null || vector.isEmpty()) {
                System.out.println("Element not found in vector set");
            }

        } catch (RedisCommandExecutionException e) {
            System.err.println("Error retrieving vector: " + e.getMessage());
        }
    }

    // Validate vector dimensions before adding
    public boolean addVectorSafely(RedisVectorSetCommands<String, String> vectorSet,
                                  String key, String element, double[] vector) {
        try {
            // Check if key exists and get its dimensions
            Long existingDim = vectorSet.vdim(key);
            if (existingDim != null && existingDim != vector.length) {
                System.err.println("Dimension mismatch: expected " + existingDim +
                                 ", got " + vector.length);
                return false;
            }

            vectorSet.vadd(key, element, vector);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to add vector: " + e.getMessage());
            return false;
        }
    }
}
```

### Best Practices

```java
public class VectorSetBestPractices {

    // 1. Use appropriate quantization for your use case
    public void chooseQuantization() {
        // High precision needed (scientific, financial)
        VAddArgs highPrecision = VAddArgs.Builder
            .quantizationType(QuantizationType.NO_QUANTIZATION)
            .build();

        // Balanced performance (most applications)
        VAddArgs balanced = VAddArgs.Builder
            .quantizationType(QuantizationType.Q8)
            .build();

        // Maximum speed/minimum memory (large scale)
        VAddArgs fast = VAddArgs.Builder
            .quantizationType(QuantizationType.BINARY)
            .build();
    }

    // 2. Batch operations for better performance
    public void batchOperations(RedisVectorSetCommands<String, String> vectorSet) {
        // Instead of individual adds, batch them
        List<VectorData> vectors = loadVectorData();

        for (VectorData data : vectors) {
            vectorSet.vadd("batch_vectors", data.id, data.vector);
            if (!data.attributes.isEmpty()) {
                vectorSet.vsetattr("batch_vectors", data.id, data.attributes);
            }
        }
    }

    // 3. Use meaningful element names
    public void useDescriptiveNames(RedisVectorSetCommands<String, String> vectorSet) {
        // Good: descriptive, hierarchical naming
        vectorSet.vadd("products", "electronics:laptop:dell:xps13", 0.1, 0.2, 0.3);
        vectorSet.vadd("users", "user:12345:preferences", 0.4, 0.5, 0.6);

        // Avoid: generic, non-descriptive names
        // vectorSet.vadd("data", "item1", 0.1, 0.2, 0.3);
    }

    // 4. Monitor vector set size and performance
    public void monitorVectorSet(RedisVectorSetCommands<String, String> vectorSet, String key) {
        Long cardinality = vectorSet.vcard(key);
        Long dimensions = vectorSet.vdim(key);

        System.out.println("Vector set '" + key + "' stats:");
        System.out.println("  Elements: " + cardinality);
        System.out.println("  Dimensions: " + dimensions);
        System.out.println("  Estimated memory: " + estimateMemoryUsage(cardinality, dimensions));
    }

    private String estimateMemoryUsage(Long elements, Long dimensions) {
        // Rough estimation for Q8 quantization
        long bytesPerVector = dimensions * 1; // 1 byte per dimension for Q8
        long totalBytes = elements * bytesPerVector;
        return String.format("~%.2f MB", totalBytes / (1024.0 * 1024.0));
    }

    private List<VectorData> loadVectorData() {
        // Placeholder for loading vector data
        return new ArrayList<>();
    }

    private static class VectorData {
        String id;
        double[] vector;
        String attributes;
    }
}
```

## Integration Examples

### Spring Boot Integration

```java
@Configuration
public class VectorSetConfig {

    @Bean
    public RedisClient redisClient() {
        return RedisClient.create("redis://localhost:6379");
    }

    @Bean
    public RedisVectorSetCommands<String, String> vectorSetCommands(RedisClient client) {
        return client.connect().sync();
    }
}

@Service
public class VectorSearchService {

    @Autowired
    private RedisVectorSetCommands<String, String> vectorSet;

    public void addDocument(String docId, double[] embedding, Map<String, Object> metadata) {
        vectorSet.vadd("documents", docId, embedding);

        if (!metadata.isEmpty()) {
            String attributes = convertToJson(metadata);
            vectorSet.vsetattr("documents", docId, attributes);
        }
    }

    public List<String> searchSimilar(double[] query, int limit) {
        VSimArgs args = VSimArgs.Builder.count(limit).build();
        return vectorSet.vsim("documents", args, query);
    }

    private String convertToJson(Map<String, Object> metadata) {
        // Use your preferred JSON library (Jackson, Gson, etc.)
        return "{}"; // Placeholder
    }
}
```

### Reactive Programming

```java
public class ReactiveVectorService {

    private final RedisVectorSetReactiveCommands<String, String> reactiveVectorSet;

    public ReactiveVectorService(RedisClient client) {
        this.reactiveVectorSet = client.connect().reactive();
    }

    public Mono<Boolean> addVectorAsync(String key, String element, double[] vector) {
        return reactiveVectorSet.vadd(key, element, vector);
    }

    public Flux<String> searchSimilarAsync(String key, double[] query, int count) {
        VSimArgs args = VSimArgs.Builder.count(count).build();
        return reactiveVectorSet.vsim(key, args, query);
    }

    public Mono<Map<String, Double>> searchWithScoresAsync(String key, String element, int count) {
        VSimArgs args = VSimArgs.Builder.count(count).build();
        return reactiveVectorSet.vsimWithScore(key, args, element);
    }
}
```

## Migration and Compatibility

### Migrating from Other Vector Databases

```java
public class VectorMigrationService {

    // Migrate from external vector database to Redis Vector Sets
    public void migrateVectors(List<VectorRecord> externalVectors,
                              RedisVectorSetCommands<String, String> vectorSet) {
        String targetKey = "migrated_vectors";

        for (VectorRecord record : externalVectors) {
            // Add vector with appropriate quantization
            VAddArgs args = VAddArgs.Builder
                .quantizationType(QuantizationType.Q8) // Balance of speed and precision
                .build();

            vectorSet.vadd(targetKey, record.getId(), args, record.getVector());

            // Migrate metadata as attributes
            if (record.getMetadata() != null) {
                String attributes = convertMetadataToJson(record.getMetadata());
                vectorSet.vsetattr(targetKey, record.getId(), attributes);
            }
        }

        System.out.println("Migrated " + externalVectors.size() + " vectors to Redis Vector Sets");
    }

    // Validate migration by comparing similarity results
    public void validateMigration(String originalQuery, List<String> expectedResults,
                                RedisVectorSetCommands<String, String> vectorSet) {
        // Perform similarity search on migrated data
        VSimArgs args = VSimArgs.Builder
            .count(expectedResults.size())
            .build();

        // Assuming originalQuery is converted to vector format
        double[] queryVector = convertQueryToVector(originalQuery);
        List<String> actualResults = vectorSet.vsim("migrated_vectors", args, queryVector);

        // Compare results (implementation depends on your validation criteria)
        boolean isValid = validateResults(expectedResults, actualResults);
        System.out.println("Migration validation: " + (isValid ? "PASSED" : "FAILED"));
    }

    private String convertMetadataToJson(Map<String, Object> metadata) {
        // Convert metadata to JSON string
        return "{}"; // Placeholder
    }

    private double[] convertQueryToVector(String query) {
        // Convert query to vector using your embedding model
        return new double[]{0.0}; // Placeholder
    }

    private boolean validateResults(List<String> expected, List<String> actual) {
        // Implement your validation logic
        return true; // Placeholder
    }

    private static class VectorRecord {
        private String id;
        private double[] vector;
        private Map<String, Object> metadata;

        // Getters and setters
        public String getId() { return id; }
        public double[] getVector() { return vector; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}
```
