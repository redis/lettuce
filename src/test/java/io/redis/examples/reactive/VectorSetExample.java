// EXAMPLE: vecset_tutorial
package io.redis.examples.reactive;

import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.vector.QuantizationType;

// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
// REMOVE_END

import reactor.core.publisher.Mono;

public class VectorSetExample {

    // REMOVE_START
    @Test
    // REMOVE_END
    public void run() {
        RedisClient redisClient = RedisClient.create("redis://localhost:6379");

        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisReactiveCommands<String, String> reactiveCommands = connection.reactive();

            // REMOVE_START
            // Clean up any existing data
            Mono<Void> cleanup = reactiveCommands
                    .del("points", "quantSetQ8", "quantSetNoQ", "quantSetBin", "setNotReduced", "setReduced").then();
            cleanup.block();
            // REMOVE_END

            // STEP_START vadd
            Mono<Boolean> addPointA = reactiveCommands.vadd("points", "pt:A", 1.0, 1.0).doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<Boolean> addPointB = reactiveCommands.vadd("points", "pt:B", -1.0, -1.0).doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<Boolean> addPointC = reactiveCommands.vadd("points", "pt:C", -1.0, 1.0).doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<Boolean> addPointD = reactiveCommands.vadd("points", "pt:D", 1.0, -1.0).doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<Boolean> addPointE = reactiveCommands.vadd("points", "pt:E", 1.0, 0.0).doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<Void> vaddOperations = Mono.when(addPointA, addPointB, addPointC, addPointD, addPointE)
                    .then(reactiveCommands.type("points")).doOnNext(result -> {
                        System.out.println(result); // >>> vectorset
                        // REMOVE_START
                        assertThat(result).isEqualTo("vectorset");
                        // REMOVE_END
                    }).then();
            // STEP_END

            // STEP_START vcardvdim
            Mono<Long> getCardinality = reactiveCommands.vcard("points").doOnNext(result -> {
                System.out.println(result); // >>> 5
                // REMOVE_START
                assertThat(result).isEqualTo(5L);
                // REMOVE_END
            });

            Mono<Long> getDimensions = reactiveCommands.vdim("points").doOnNext(result -> {
                System.out.println(result); // >>> 2
                // REMOVE_START
                assertThat(result).isEqualTo(2L);
                // REMOVE_END
            });
            // STEP_END

            // STEP_START vemb
            Mono<java.util.List<Double>> getEmbeddingA = reactiveCommands.vemb("points", "pt:A").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [0.9999999403953552, 0.9999999403953552]
                        // REMOVE_START
                        assertThat(1.0 - result.get(0)).isLessThan(0.001);
                        assertThat(1.0 - result.get(1)).isLessThan(0.001);
                        // REMOVE_END
                    });

            Mono<java.util.List<Double>> getEmbeddingB = reactiveCommands.vemb("points", "pt:B").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [-0.9999999403953552, -0.9999999403953552]
                        // REMOVE_START
                        assertThat(1.0 + result.get(0)).isLessThan(0.001);
                        assertThat(1.0 + result.get(1)).isLessThan(0.001);
                        // REMOVE_END
                    });

            Mono<java.util.List<Double>> getEmbeddingC = reactiveCommands.vemb("points", "pt:C").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [-0.9999999403953552, 0.9999999403953552]
                        // REMOVE_START
                        assertThat(1.0 + result.get(0)).isLessThan(0.001);
                        assertThat(1.0 - result.get(1)).isLessThan(0.001);
                        // REMOVE_END
                    });

            Mono<java.util.List<Double>> getEmbeddingD = reactiveCommands.vemb("points", "pt:D").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [0.9999999403953552, -0.9999999403953552]
                        // REMOVE_START
                        assertThat(1.0 - result.get(0)).isLessThan(0.001);
                        assertThat(1.0 + result.get(1)).isLessThan(0.001);
                        // REMOVE_END
                    });

            Mono<java.util.List<Double>> getEmbeddingE = reactiveCommands.vemb("points", "pt:E").collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [1.0, 0.0]
                        // REMOVE_START
                        assertThat(result.get(0)).isEqualTo(1.0);
                        assertThat(result.get(1)).isEqualTo(0.0);
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START attr
            Mono<Boolean> setAttributeA = reactiveCommands
                    .vsetattr("points", "pt:A", "{\"name\": \"Point A\", \"description\": \"First point added\"}")
                    .doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    });

            Mono<String> getAttributeA = reactiveCommands.vgetattr("points", "pt:A").doOnNext(result -> {
                System.out.println(result); // >>> {"name": "Point A", "description": "First point added"}
                // REMOVE_START
                assertThat(result).contains("Point A");
                assertThat(result).contains("First point added");
                // REMOVE_END
            });

            Mono<Boolean> clearAttributeA = reactiveCommands.vsetattr("points", "pt:A", "").doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<String> verifyAttributeCleared = reactiveCommands.vgetattr("points", "pt:A").doOnNext(result -> {
                System.out.println(result); // >>> null
                // REMOVE_START
                assertThat(result).isNull();
                // REMOVE_END
            });
            // STEP_END

            // STEP_START vrem
            Mono<Boolean> addTempPointF = reactiveCommands.vadd("points", "pt:F", 0.0, 0.0).doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<Long> checkCardinalityBefore = reactiveCommands.vcard("points").doOnNext(result -> {
                System.out.println(result); // >>> 6
                // REMOVE_START
                assertThat(result).isEqualTo(6L);
                // REMOVE_END
            });

            Mono<Boolean> removePointF = reactiveCommands.vrem("points", "pt:F").doOnNext(result -> {
                System.out.println(result); // >>> true
                // REMOVE_START
                assertThat(result).isTrue();
                // REMOVE_END
            });

            Mono<Long> checkCardinalityAfter = reactiveCommands.vcard("points").doOnNext(result -> {
                System.out.println(result); // >>> 5
                // REMOVE_START
                assertThat(result).isEqualTo(5L);
                // REMOVE_END
            });
            // STEP_END

            // STEP_START vsim_basic
            Mono<java.util.List<String>> basicSimilaritySearch = reactiveCommands.vsim("points", 0.9, 0.1).collectList()
                    .doOnNext(result -> {
                        System.out.println(result); // >>> [pt:E, pt:A, pt:D, pt:C, pt:B]
                        // REMOVE_START
                        assertThat(result).containsExactly("pt:E", "pt:A", "pt:D", "pt:C", "pt:B");
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START vsim_options
            VSimArgs vsimArgs = new VSimArgs();
            vsimArgs.count(4L);
            Mono<java.util.Map<String, Double>> similaritySearchWithScore = reactiveCommands
                    .vsimWithScore("points", vsimArgs, "pt:A").doOnNext(result -> {
                        System.out.println(result); // >>> {pt:A=1.0, pt:E=0.8535534143447876, pt:D=0.5, pt:C=0.5}
                        // REMOVE_START
                        assertThat(result.get("pt:A")).isEqualTo(1.0);
                        assertThat(result.get("pt:C")).isEqualTo(0.5);
                        assertThat(result.get("pt:D")).isEqualTo(0.5);
                        assertThat(result.get("pt:E") - 0.85).isLessThan(0.005);
                        // REMOVE_END
                    });
            // STEP_END

            // STEP_START vsim_filter
            Mono<Void> filteredSimilaritySearch = reactiveCommands
                    .vsetattr("points", "pt:A", "{\"size\": \"large\", \"price\": 18.99}").doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vsetattr("points", "pt:B", "{\"size\": \"large\", \"price\": 35.99}"))
                    .doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vsetattr("points", "pt:C", "{\"size\": \"large\", \"price\": 25.99}"))
                    .doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vsetattr("points", "pt:D", "{\"size\": \"small\", \"price\": 21.00}"))
                    .doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vsetattr("points", "pt:E", "{\"size\": \"small\", \"price\": 17.75}"))
                    .doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> {
                        // Return elements in order of distance from point A whose size attribute is large.
                        VSimArgs filterArgs = new VSimArgs();
                        filterArgs.filter(".size == \"large\"");
                        return reactiveCommands.vsim("points", filterArgs, "pt:A").collectList();
                    }).doOnNext(result -> {
                        System.out.println(result); // >>> [pt:A, pt:C, pt:B]
                        // REMOVE_START
                        assertThat(result).containsExactly("pt:A", "pt:C", "pt:B");
                        // REMOVE_END
                    }).flatMap(result -> {
                        // Return elements in order of distance from point A whose size is large and price > 20.00.
                        VSimArgs filterArgs2 = new VSimArgs();
                        filterArgs2.filter(".size == \"large\" && .price > 20.00");
                        return reactiveCommands.vsim("points", filterArgs2, "pt:A").collectList();
                    }).doOnNext(result -> {
                        System.out.println(result); // >>> [pt:C, pt:B]
                        // REMOVE_START
                        assertThat(result).containsExactly("pt:C", "pt:B");
                        // REMOVE_END
                    }).then();
            // STEP_END

            // STEP_START add_quant
            VAddArgs q8Args = VAddArgs.Builder.quantizationType(QuantizationType.Q8);
            Mono<Void> quantizationOperations = reactiveCommands.vadd("quantSetQ8", "quantElement", q8Args, 1.262185, 1.958231)
                    .doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vemb("quantSetQ8", "quantElement").collectList()).doOnNext(result -> {
                        System.out.println("Q8: " + result); // >>> Q8: [1.2643694877624512, 1.958230972290039]
                    }).flatMap(result -> {
                        VAddArgs noQuantArgs = VAddArgs.Builder.quantizationType(QuantizationType.NO_QUANTIZATION);
                        return reactiveCommands.vadd("quantSetNoQ", "quantElement", noQuantArgs, 1.262185, 1.958231);
                    }).doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vemb("quantSetNoQ", "quantElement").collectList())
                    .doOnNext(result -> {
                        System.out.println("NOQUANT: " + result); // >>> NOQUANT: [1.262184977531433, 1.958230972290039]
                    }).flatMap(result -> {
                        VAddArgs binArgs = VAddArgs.Builder.quantizationType(QuantizationType.BINARY);
                        return reactiveCommands.vadd("quantSetBin", "quantElement", binArgs, 1.262185, 1.958231);
                    }).doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vemb("quantSetBin", "quantElement").collectList())
                    .doOnNext(result -> {
                        System.out.println("BIN: " + result); // >>> BIN: [1.0, 1.0]
                    }).then();
            // STEP_END

            // STEP_START add_reduce
            // Create a list of 300 arbitrary values.
            Double[] values = new Double[300];
            for (int i = 0; i < 300; i++) {
                values[i] = (double) i / 299;
            }

            Mono<Void> dimensionalityReductionOperations = reactiveCommands.vadd("setNotReduced", "element", values)
                    .doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vdim("setNotReduced")).doOnNext(result -> {
                        System.out.println(result); // >>> 300
                        // REMOVE_START
                        assertThat(result).isEqualTo(300L);
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vadd("setReduced", 100, "element", values)).doOnNext(result -> {
                        System.out.println(result); // >>> true
                        // REMOVE_START
                        assertThat(result).isTrue();
                        // REMOVE_END
                    }).flatMap(result -> reactiveCommands.vdim("setReduced")).doOnNext(result -> {
                        System.out.println(result); // >>> 100
                        // REMOVE_START
                        assertThat(result).isEqualTo(100L);
                        // REMOVE_END
                    }).then();
            // STEP_END

            // Wait for all reactive operations to complete
            Mono.when(
                    // Vector addition operations (chained sequentially)
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
                    quantizationOperations, dimensionalityReductionOperations).block();

        } finally {
            redisClient.shutdown();
        }
    }

}
