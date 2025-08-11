/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link VectorFieldArgs}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class VectorFieldArgsTest {

    @Test
    void testDefaultVectorFieldArgs() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("embedding").build();

        assertThat(field.getName()).isEqualTo("embedding");
        assertThat(field.getFieldType()).isEqualTo("VECTOR");
        assertThat(field.getAlgorithm()).isEmpty();
        assertThat(field.getAttributes()).isEmpty();
        assertThat(field.getAs()).isEmpty();
        assertThat(field.isSortable()).isFalse();
        assertThat(field.isUnNormalizedForm()).isFalse();
        assertThat(field.isNoIndex()).isFalse();
        assertThat(field.isIndexEmpty()).isFalse();
        assertThat(field.isIndexMissing()).isFalse();
    }

    @Test
    void testVectorFieldArgsWithFlat() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").flat().build();

        assertThat(field.getName()).isEqualTo("vector");
        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.FLAT);
    }

    @Test
    void testVectorFieldArgsWithHnsw() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").hnsw().build();

        assertThat(field.getName()).isEqualTo("vector");
        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.HNSW);
    }

    @Test
    void testVectorFieldArgsWithType() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector")
                .type(VectorFieldArgs.VectorType.FLOAT32).build();

        assertThat(field.getAttributes()).containsEntry("TYPE", "FLOAT32");
    }

    @Test
    void testVectorFieldArgsWithDimensions() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").dimensions(128).build();

        assertThat(field.getAttributes()).containsEntry("DIM", 128);
    }

    @Test
    void testVectorFieldArgsWithDistanceMetric() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector")
                .distanceMetric(VectorFieldArgs.DistanceMetric.COSINE).build();

        assertThat(field.getAttributes()).containsEntry("DISTANCE_METRIC", "COSINE");
    }

    @Test
    void testVectorFieldArgsWithCustomAttribute() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").attribute("INITIAL_CAP", 1000)
                .build();

        assertThat(field.getAttributes()).containsEntry("INITIAL_CAP", 1000);
    }

    @Test
    void testVectorFieldArgsWithMultipleAttributes() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").attribute("BLOCK_SIZE", 512)
                .attribute("M", 16).attribute("EF_CONSTRUCTION", 200).build();

        assertThat(field.getAttributes()).containsEntry("BLOCK_SIZE", 512);
        assertThat(field.getAttributes()).containsEntry("M", 16);
        assertThat(field.getAttributes()).containsEntry("EF_CONSTRUCTION", 200);
    }

    @Test
    void testVectorFieldArgsWithAllFlatOptions() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("flat_vector").as("vector").flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(256).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .attribute("INITIAL_CAP", 2000).attribute("BLOCK_SIZE", 1024).sortable().build();

        assertThat(field.getName()).isEqualTo("flat_vector");
        assertThat(field.getAs()).hasValue("vector");
        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.FLAT);
        assertThat(field.getAttributes()).containsEntry("TYPE", "FLOAT32");
        assertThat(field.getAttributes()).containsEntry("DIM", 256);
        assertThat(field.getAttributes()).containsEntry("DISTANCE_METRIC", "L2");
        assertThat(field.getAttributes()).containsEntry("INITIAL_CAP", 2000);
        assertThat(field.getAttributes()).containsEntry("BLOCK_SIZE", 1024);
        assertThat(field.isSortable()).isTrue();
    }

    @Test
    void testVectorFieldArgsWithAllHnswOptions() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("hnsw_vector").as("vector").hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT64).dimensions(512).distanceMetric(VectorFieldArgs.DistanceMetric.IP)
                .attribute("INITIAL_CAP", 5000).attribute("M", 32).attribute("EF_CONSTRUCTION", 400).attribute("EF_RUNTIME", 20)
                .attribute("EPSILON", 0.005).sortable().build();

        assertThat(field.getName()).isEqualTo("hnsw_vector");
        assertThat(field.getAs()).hasValue("vector");
        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.HNSW);
        assertThat(field.getAttributes()).containsEntry("TYPE", "FLOAT64");
        assertThat(field.getAttributes()).containsEntry("DIM", 512);
        assertThat(field.getAttributes()).containsEntry("DISTANCE_METRIC", "IP");
        assertThat(field.getAttributes()).containsEntry("INITIAL_CAP", 5000);
        assertThat(field.getAttributes()).containsEntry("M", 32);
        assertThat(field.getAttributes()).containsEntry("EF_CONSTRUCTION", 400);
        assertThat(field.getAttributes()).containsEntry("EF_RUNTIME", 20);
        assertThat(field.getAttributes()).containsEntry("EPSILON", 0.005);
        assertThat(field.isSortable()).isTrue();
    }

    @Test
    void testVectorTypeEnum() {
        assertThat(VectorFieldArgs.VectorType.FLOAT32.name()).isEqualTo("FLOAT32");
        assertThat(VectorFieldArgs.VectorType.FLOAT64.name()).isEqualTo("FLOAT64");
    }

    @Test
    void testDistanceMetricEnum() {
        assertThat(VectorFieldArgs.DistanceMetric.L2.name()).isEqualTo("L2");
        assertThat(VectorFieldArgs.DistanceMetric.IP.name()).isEqualTo("IP");
        assertThat(VectorFieldArgs.DistanceMetric.COSINE.name()).isEqualTo("COSINE");
    }

    @Test
    void testAlgorithmEnum() {
        assertThat(VectorFieldArgs.Algorithm.FLAT.name()).isEqualTo("FLAT");
        assertThat(VectorFieldArgs.Algorithm.HNSW.name()).isEqualTo("HNSW");
        assertThat(VectorFieldArgs.Algorithm.SVS_VAMANA.toString()).isEqualTo("SVS-VAMANA");
    }

    @Test
    void testVectorFieldArgsBuildFlat() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("test_vector").as("vector").flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(128).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .attribute("INITIAL_CAP", 1000).attribute("BLOCK_SIZE", 512).sortable().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("test_vector");
        assertThat(argsString).contains("AS");
        assertThat(argsString).contains("vector");
        assertThat(argsString).contains("VECTOR");
        assertThat(argsString).contains("FLAT");
        assertThat(argsString).contains("TYPE");
        assertThat(argsString).contains("FLOAT32");
        assertThat(argsString).contains("DIM");
        assertThat(argsString).contains("128");
        assertThat(argsString).contains("DISTANCE_METRIC");
        assertThat(argsString).contains("COSINE");
        assertThat(argsString).contains("INITIAL_CAP");
        assertThat(argsString).contains("1000");
        assertThat(argsString).contains("BLOCK_SIZE");
        assertThat(argsString).contains("512");
        assertThat(argsString).contains("SORTABLE");
    }

    @Test
    void testVectorFieldArgsBuildHnsw() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("hnsw_test").hnsw()
                .type(VectorFieldArgs.VectorType.FLOAT64).dimensions(256).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .attribute("M", 16).attribute("EF_CONSTRUCTION", 200).attribute("EF_RUNTIME", 10).attribute("EPSILON", 0.01)
                .build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("hnsw_test");
        assertThat(argsString).contains("VECTOR");
        assertThat(argsString).contains("HNSW");
        assertThat(argsString).contains("TYPE");
        assertThat(argsString).contains("FLOAT64");
        assertThat(argsString).contains("DIM");
        assertThat(argsString).contains("256");
        assertThat(argsString).contains("DISTANCE_METRIC");
        assertThat(argsString).contains("L2");
        assertThat(argsString).contains("M");
        assertThat(argsString).contains("16");
        assertThat(argsString).contains("EF_CONSTRUCTION");
        assertThat(argsString).contains("200");
        assertThat(argsString).contains("EF_RUNTIME");
        assertThat(argsString).contains("10");
        assertThat(argsString).contains("EPSILON");
        assertThat(argsString).contains("0.01");
    }

    @Test
    void testVectorFieldArgsMinimalBuild() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("simple_vector").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("simple_vector");
        assertThat(argsString).contains("VECTOR");
        assertThat(argsString).doesNotContain("AS");
        assertThat(argsString).doesNotContain("FLAT");
        assertThat(argsString).doesNotContain("HNSW");
        assertThat(argsString).doesNotContain("TYPE");
        assertThat(argsString).doesNotContain("DIM");
        assertThat(argsString).doesNotContain("DISTANCE_METRIC");
        assertThat(argsString).doesNotContain("SORTABLE");
    }

    @Test
    void testBuilderMethodChaining() {
        // Test that builder methods return the correct type for method chaining
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("chained_vector").as("chained_alias").flat()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(64).distanceMetric(VectorFieldArgs.DistanceMetric.IP)
                .attribute("INITIAL_CAP", 500).attribute("BLOCK_SIZE", 256).sortable().noIndex().build();

        assertThat(field.getName()).isEqualTo("chained_vector");
        assertThat(field.getAs()).hasValue("chained_alias");
        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.FLAT);
        assertThat(field.getAttributes()).containsEntry("TYPE", "FLOAT32");
        assertThat(field.getAttributes()).containsEntry("DIM", 64);
        assertThat(field.getAttributes()).containsEntry("DISTANCE_METRIC", "IP");
        assertThat(field.getAttributes()).containsEntry("INITIAL_CAP", 500);
        assertThat(field.getAttributes()).containsEntry("BLOCK_SIZE", 256);
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
    }

    @Test
    void testVectorFieldArgsWithSvsVamana() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").svsVamana().build();

        assertThat(field.getName()).isEqualTo("vector");
        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.SVS_VAMANA);
    }

    @Test
    void testSvsVamanaWithCompression() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("compressed_vector").svsVamana()
                .attribute("COMPRESSION", "LVQ").build();

        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.SVS_VAMANA);
        assertThat(field.getAttributes()).containsEntry("COMPRESSION", "LVQ");
    }

    @Test
    void testSvsVamanaWithLeanVecCompression() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("leanvec_vector").svsVamana()
                .attribute("COMPRESSION", "LEANVEC").build();

        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.SVS_VAMANA);
        assertThat(field.getAttributes()).containsEntry("COMPRESSION", "LEANVEC");
    }

    @Test
    void testSvsVamanaWithConstructionWindowSize() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").svsVamana()
                .attribute("CONSTRUCTION_WINDOW_SIZE", 128).build();

        assertThat(field.getAttributes()).containsEntry("CONSTRUCTION_WINDOW_SIZE", 128);
    }

    @Test
    void testSvsVamanaWithGraphMaxDegree() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").svsVamana()
                .attribute("GRAPH_MAX_DEGREE", 64).build();

        assertThat(field.getAttributes()).containsEntry("GRAPH_MAX_DEGREE", 64);
    }

    @Test
    void testSvsVamanaWithSearchWindowSize() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("vector").svsVamana()
                .attribute("SEARCH_WINDOW_SIZE", 100).build();

        assertThat(field.getAttributes()).containsEntry("SEARCH_WINDOW_SIZE", 100);
    }

    @Test
    void testSvsVamanaWithAllOptions() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("svs_vector").as("vector").svsVamana()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(384).distanceMetric(VectorFieldArgs.DistanceMetric.COSINE)
                .attribute("COMPRESSION", "LVQ").attribute("CONSTRUCTION_WINDOW_SIZE", 256).attribute("GRAPH_MAX_DEGREE", 64)
                .attribute("SEARCH_WINDOW_SIZE", 128).sortable().build();

        assertThat(field.getName()).isEqualTo("svs_vector");
        assertThat(field.getAs()).hasValue("vector");
        assertThat(field.getAlgorithm()).hasValue(VectorFieldArgs.Algorithm.SVS_VAMANA);
        assertThat(field.getAttributes()).containsEntry("TYPE", "FLOAT32");
        assertThat(field.getAttributes()).containsEntry("DIM", 384);
        assertThat(field.getAttributes()).containsEntry("DISTANCE_METRIC", "COSINE");
        assertThat(field.getAttributes()).containsEntry("COMPRESSION", "LVQ");
        assertThat(field.getAttributes()).containsEntry("CONSTRUCTION_WINDOW_SIZE", 256);
        assertThat(field.getAttributes()).containsEntry("GRAPH_MAX_DEGREE", 64);
        assertThat(field.getAttributes()).containsEntry("SEARCH_WINDOW_SIZE", 128);
        assertThat(field.isSortable()).isTrue();
    }

    @Test
    void testVectorFieldArgsBuildSvsVamana() {
        VectorFieldArgs<String> field = VectorFieldArgs.<String> builder().name("svs_test").svsVamana()
                .type(VectorFieldArgs.VectorType.FLOAT32).dimensions(128).distanceMetric(VectorFieldArgs.DistanceMetric.L2)
                .attribute("COMPRESSION", "LVQ").attribute("CONSTRUCTION_WINDOW_SIZE", 256).attribute("GRAPH_MAX_DEGREE", 64)
                .attribute("SEARCH_WINDOW_SIZE", 128).build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("svs_test");
        assertThat(argsString).contains("VECTOR");
        assertThat(argsString).contains("SVS-VAMANA");
        assertThat(argsString).contains("18"); // 9 attributes * 2
        assertThat(argsString).contains("TYPE");
        assertThat(argsString).contains("FLOAT32");
        assertThat(argsString).contains("DIM");
        assertThat(argsString).contains("128");
        assertThat(argsString).contains("DISTANCE_METRIC");
        assertThat(argsString).contains("L2");
        assertThat(argsString).contains("COMPRESSION");
        assertThat(argsString).contains("LVQ");
        assertThat(argsString).contains("CONSTRUCTION_WINDOW_SIZE");
        assertThat(argsString).contains("256");
        assertThat(argsString).contains("GRAPH_MAX_DEGREE");
        assertThat(argsString).contains("64");
        assertThat(argsString).contains("SEARCH_WINDOW_SIZE");
    }

}
