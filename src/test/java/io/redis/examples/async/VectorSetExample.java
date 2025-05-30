// EXAMPLE: vecset_tutorial
package io.redis.examples.async;

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.vector.QuantizationType;

// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

import java.util.concurrent.CompletableFuture;

public class VectorSetExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisAsyncCommands<String, String> asyncCommands = connection.async();

            // REMOVE_START
            // Clean up any existing data
            asyncCommands.del("points", "quantSetQ8", "quantSetNoQ", "quantSetBin", "setNotReduced", "setReduced")
                    .toCompletableFuture().join();
            // REMOVE_END

            // STEP_START vadd
            CompletableFuture<Boolean> addPointA = asyncCommands.vadd("points", "pt:A", 1.0, 1.0).thenApply(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
                return result;
            }).toCompletableFuture();

            CompletableFuture<Boolean> addPointB = asyncCommands.vadd("points", "pt:B", -1.0, -1.0).thenApply(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
                return result;
            }).toCompletableFuture();

            CompletableFuture<Boolean> addPointC = asyncCommands.vadd("points", "pt:C", -1.0, 1.0).thenApply(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
                return result;
            }).toCompletableFuture();

            CompletableFuture<Boolean> addPointD = asyncCommands.vadd("points", "pt:D", 1.0, -1.0).thenApply(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
                return result;
            }).toCompletableFuture();

            CompletableFuture<Boolean> addPointE = asyncCommands.vadd("points", "pt:E", 1.0, 0.0).thenApply(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
                return result;
            }).toCompletableFuture();

            // Chain checkDataType after all vadd operations complete
            CompletableFuture<Void> vaddOperations = CompletableFuture
                    .allOf(addPointA, addPointB, addPointC, addPointD, addPointE)
                    .thenCompose(ignored -> asyncCommands.type("points")).thenAccept(result -> {
                        System.out.println(result); // >>> vectorset
                        // REMOVE_START
                        assertThat(result).isEqualTo("vectorset");
                        // REMOVE_END
                    }).toCompletableFuture();
            // STEP_END

            // STEP_START vcardvdim
            CompletableFuture<Void> getCardinality = asyncCommands.vcard("points").thenAccept(result -> {
                System.out.println(result); // >>> 5
                // REMOVE_START
                assertThat(result).isEqualTo(5L);
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> getDimensions = asyncCommands.vdim("points").thenAccept(result -> {
                System.out.println(result); // >>> 2
                // REMOVE_START
                assertThat(result).isEqualTo(2L);
                // REMOVE_END
            }).toCompletableFuture();
            // STEP_END

            // STEP_START vemb
            CompletableFuture<Void> getEmbeddingA = asyncCommands.vemb("points", "pt:A").thenAccept(result -> {
                System.out.println(result); // >>> [0.9999999403953552, 0.9999999403953552]
                // REMOVE_START
                assertThat(1.0 - result.get(0)).isLessThan(0.001);
                assertThat(1.0 - result.get(1)).isLessThan(0.001);
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> getEmbeddingB = asyncCommands.vemb("points", "pt:B").thenAccept(result -> {
                System.out.println(result); // >>> [-0.9999999403953552, -0.9999999403953552]
                // REMOVE_START
                assertThat(1.0 + result.get(0)).isLessThan(0.001);
                assertThat(1.0 + result.get(1)).isLessThan(0.001);
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> getEmbeddingC = asyncCommands.vemb("points", "pt:C").thenAccept(result -> {
                System.out.println(result); // >>> [-0.9999999403953552, 0.9999999403953552]
                // REMOVE_START
                assertThat(1.0 + result.get(0)).isLessThan(0.001);
                assertThat(1.0 - result.get(1)).isLessThan(0.001);
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> getEmbeddingD = asyncCommands.vemb("points", "pt:D").thenAccept(result -> {
                System.out.println(result); // >>> [0.9999999403953552, -0.9999999403953552]
                // REMOVE_START
                assertThat(1.0 - result.get(0)).isLessThan(0.001);
                assertThat(1.0 + result.get(1)).isLessThan(0.001);
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> getEmbeddingE = asyncCommands.vemb("points", "pt:E").thenAccept(result -> {
                System.out.println(result); // >>> [1.0, 0.0]
                // REMOVE_START
                assertThat(result.get(0)).isEqualTo(1.0);
                assertThat(result.get(1)).isEqualTo(0.0);
                // REMOVE_END
            }).toCompletableFuture();
            // STEP_END

            // STEP_START attr
            CompletableFuture<Void> setAttributeA = asyncCommands
                    .vsetattr("points", "pt:A", "{\"name\": \"Point A\", \"description\": \"First point added\"}")
                    .thenAccept(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).toCompletableFuture();

            CompletableFuture<Void> getAttributeA = asyncCommands.vgetattr("points", "pt:A").thenAccept(result -> {
                System.out.println(result); // >>> {"name": "Point A", "description": "First point added"}
                // REMOVE_START
                assertThat(result).contains("Point A");
                assertThat(result).contains("First point added");
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> clearAttributeA = asyncCommands.vsetattr("points", "pt:A", "").thenAccept(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> verifyAttributeCleared = asyncCommands.vgetattr("points", "pt:A").thenAccept(result -> {
                System.out.println(result); // >>> null
                // REMOVE_START
                assertThat(result).isNull();
                // REMOVE_END
            }).toCompletableFuture();
            // STEP_END

            // STEP_START vrem
            CompletableFuture<Void> addTempPointF = asyncCommands.vadd("points", "pt:F", 0.0, 0.0).thenAccept(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> checkCardinalityBefore = asyncCommands.vcard("points").thenAccept(result -> {
                System.out.println(result); // >>> 6
                // REMOVE_START
                assertThat(result).isEqualTo(6L);
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> removePointF = asyncCommands.vrem("points", "pt:F").thenAccept(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            }).toCompletableFuture();

            CompletableFuture<Void> checkCardinalityAfter = asyncCommands.vcard("points").thenAccept(result -> {
                System.out.println(result); // >>> 5
                // REMOVE_START
                assertThat(result).isEqualTo(5L);
                // REMOVE_END
            }).toCompletableFuture();
            // STEP_END

            // STEP_START vsim_basic
            CompletableFuture<Void> basicSimilaritySearch = asyncCommands.vsim("points", 0.9, 0.1).thenAccept(result -> {
                System.out.println(result); // >>> [pt:E, pt:A, pt:D, pt:C, pt:B]
                // REMOVE_START
                assertThat(result).containsExactly("pt:E", "pt:A", "pt:D", "pt:C", "pt:B");
                // REMOVE_END
            }).toCompletableFuture();
            // STEP_END

            // STEP_START vsim_options
            VSimArgs vsimArgs = new VSimArgs();
            vsimArgs.count(4L);
            CompletableFuture<Void> similaritySearchWithScore = asyncCommands.vsimWithScore("points", vsimArgs, "pt:A")
                    .thenAccept(result -> {
                        System.out.println(result); // >>> {pt:A=1.0, pt:E=0.8535534143447876, pt:D=0.5, pt:C=0.5}
                        // REMOVE_START
                        assertThat(result.get("pt:A")).isEqualTo(1.0);
                        assertThat(result.get("pt:C")).isEqualTo(0.5);
                        assertThat(result.get("pt:D")).isEqualTo(0.5);
                        assertThat(result.get("pt:E") - 0.85).isLessThan(0.005);
                        // REMOVE_END
                    }).toCompletableFuture();
            // STEP_END

            // STEP_START vsim_filter
            CompletableFuture<Void> filteredSimilaritySearch = asyncCommands
                    .vsetattr("points", "pt:A", "{\"size\": \"large\", \"price\": 18.99}").thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vsetattr("points", "pt:B", "{\"size\": \"large\", \"price\": 35.99}");
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vsetattr("points", "pt:C", "{\"size\": \"large\", \"price\": 25.99}");
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vsetattr("points", "pt:D", "{\"size\": \"small\", \"price\": 21.00}");
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vsetattr("points", "pt:E", "{\"size\": \"small\", \"price\": 17.75}");
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END

                        // Return elements in order of distance from point A whose size attribute is large.
                        VSimArgs filterArgs = new VSimArgs();
                        filterArgs.filter(".size == \"large\"");
                        return asyncCommands.vsim("points", filterArgs, "pt:A");
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> [pt:A, pt:C, pt:B]
                        // REMOVE_START
                        assertThat(result).containsExactly("pt:A", "pt:C", "pt:B");
                        // REMOVE_END

                        // Return elements in order of distance from point A whose size is large and price > 20.00.
                        VSimArgs filterArgs2 = new VSimArgs();
                        filterArgs2.filter(".size == \"large\" && .price > 20.00");
                        return asyncCommands.vsim("points", filterArgs2, "pt:A");
                    }).thenAccept(result -> {
                        System.out.println(result); // >>> [pt:C, pt:B]
                        // REMOVE_START
                        assertThat(result).containsExactly("pt:C", "pt:B");
                        // REMOVE_END
                    }).toCompletableFuture();
            // STEP_END
            // STEP_START add_quant
            VAddArgs q8Args = VAddArgs.Builder.quantizationType(QuantizationType.Q8);
            CompletableFuture<Void> quantizationOperations = asyncCommands
                    .vadd("quantSetQ8", "quantElement", q8Args, 1.262185, 1.958231).thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vemb("quantSetQ8", "quantElement");
                    }).thenCompose(result -> {
                        System.out.println("Q8: " + result); // >>> Q8: [1.2643694877624512, 1.958230972290039]

                        VAddArgs noQuantArgs = VAddArgs.Builder.quantizationType(QuantizationType.NO_QUANTIZATION);
                        return asyncCommands.vadd("quantSetNoQ", "quantElement", noQuantArgs, 1.262185, 1.958231);
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vemb("quantSetNoQ", "quantElement");
                    }).thenCompose(result -> {
                        System.out.println("NOQUANT: " + result); // >>> NOQUANT: [1.262184977531433, 1.958230972290039]

                        VAddArgs binArgs = VAddArgs.Builder.quantizationType(QuantizationType.BINARY);
                        return asyncCommands.vadd("quantSetBin", "quantElement", binArgs, 1.262185, 1.958231);
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vemb("quantSetBin", "quantElement");
                    }).thenAccept(result -> {
                        System.out.println("BIN: " + result); // >>> BIN: [1.0, 1.0]
                    }).toCompletableFuture();
            // STEP_END

            // STEP_START add_reduce
            // Create a list of 300 arbitrary values.
            Double[] values = new Double[300];
            for (int i = 0; i < 300; i++) {
                values[i] = (double) i / 299;
            }

            CompletableFuture<Void> dimensionalityReductionOperations = asyncCommands.vadd("setNotReduced", "element", values)
                    .thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vdim("setNotReduced");
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> 300
                        // REMOVE_START
                        assertThat(result).isEqualTo(300L);
                        // REMOVE_END
                        return asyncCommands.vadd("setReduced", 100, "element", values);
                    }).thenCompose(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                        return asyncCommands.vdim("setReduced");
                    }).thenAccept(result -> {
                        System.out.println(result); // >>> 100
                        // REMOVE_START
                        assertThat(result).isEqualTo(100L);
                        // REMOVE_END
                    }).toCompletableFuture();
            // STEP_END

            // Wait for all async operations to complete
            CompletableFuture.allOf(
                    // Vector addition operations (chained: parallel vadd + sequential checkDataType)
                    vaddOperations,
                    // Cardinality and dimension operations
                    getCardinality, getDimensions,
                    // Vector embedding retrieval operations
                    getEmbeddingA, getEmbeddingB, getEmbeddingC, getEmbeddingD, getEmbeddingE,
                    // Attribute operations
                    setAttributeA, getAttributeA, clearAttributeA, verifyAttributeCleared,
                    // Vector removal operations
                    addTempPointF, checkCardinalityBefore, removePointF, checkCardinalityAfter,
                    // Similarity search operations
                    basicSimilaritySearch, similaritySearchWithScore, filteredSimilaritySearch,
                    // Advanced operations
                    quantizationOperations, dimensionalityReductionOperations).join();

        } finally {
            redisClient.shutdown();
        }
    }

}
