/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.search.IndexInfo;
import io.lettuce.core.search.IndexInfoParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link IndexInfoParser}.
 * 
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class IndexInfoParserUnitTests {

    private IndexInfoParser<String, String> parser;

    @BeforeEach
    void setUp() {
        parser = new IndexInfoParser<>(StringCodec.UTF8);
    }

    @Test
    void shouldRejectNullCodec() {
        assertThatThrownBy(() -> new IndexInfoParser<>(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Codec must not be null");
    }

    @Test
    void shouldReturnEmptyIndexInfoForNullData() {
        IndexInfo<String> result = parser.parse(null);
        assertThat(result).isNotNull();
        assertThat(result.getIndexName()).isNull();
    }

    @Test
    void shouldParseResp2BasicIndexInfo() {
        ComplexData input = createResp2BasicIndexInfo();
        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexName()).isEqualTo("test-index");
        assertThat(result.getNumDocs()).isEqualTo(100L);
        assertThat(result.getMaxDocId()).isEqualTo(150L);
        assertThat(result.getNumTerms()).isEqualTo(500L);
        assertThat(result.getNumRecords()).isEqualTo(1000L);
    }

    @Test
    void shouldParseResp3BasicIndexInfo() {
        ComplexData input = createResp3BasicIndexInfo();
        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexName()).isEqualTo("test-index");
        assertThat(result.getNumDocs()).isEqualTo(100L);
        assertThat(result.getMaxDocId()).isEqualTo(150L);
        assertThat(result.getNumTerms()).isEqualTo(500L);
        assertThat(result.getNumRecords()).isEqualTo(1000L);
    }

    @Test
    void shouldParseIndexOptions() {
        ComplexData options = new ArrayComplexData(6);
        options.store("NOOFFSETS");
        options.store("NOHL");
        options.store("NOFIELDS");
        options.store("NOFREQS");
        options.store("MAXTEXTFIELDS");
        options.store("SKIPINITIALSCAN");

        ComplexData input = new ArrayComplexData(2);
        input.store("index_options");
        input.storeObject(options);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.isNoOffsets()).isTrue();
        assertThat(result.isNoHighlight()).isTrue();
        assertThat(result.isNoFields()).isTrue();
        assertThat(result.isNoFrequency()).isTrue();
        assertThat(result.isMaxTextFields()).isTrue();
        assertThat(result.isSkipInitialScan()).isTrue();
    }

    @Test
    void shouldParseIndexDefinitionResp2() {
        ComplexData definition = new ArrayComplexData(8);
        definition.store("key_type");
        definition.store("HASH");
        definition.store("prefixes");
        ComplexData prefixes = new ArrayComplexData(2);
        prefixes.store("doc:");
        prefixes.store("user:");
        definition.storeObject(prefixes);
        definition.store("default_score");
        definition.store("1.0");
        definition.store("filter");
        definition.store("@status=='active'");

        ComplexData input = new ArrayComplexData(2);
        input.store("index_definition");
        input.storeObject(definition);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexDefinition()).isNotNull();
        assertThat(result.getIndexDefinition().getKeyType()).isEqualTo(IndexInfo.IndexDefinition.TargetType.HASH);
        assertThat(result.getIndexDefinition().getPrefixes()).containsExactly("doc:", "user:");
        assertThat(result.getIndexDefinition().getDefaultScore()).isEqualTo(1.0);
        assertThat(result.getIndexDefinition().getFilter()).isEqualTo("@status=='active'");
    }

    @Test
    void shouldParseIndexDefinitionResp3() {
        ComplexData prefixes = new ArrayComplexData(1);
        prefixes.store("product:");

        ComplexData definition = new MapComplexData(3);
        definition.store("key_type");
        definition.store("JSON");
        definition.store("prefixes");
        definition.storeObject(prefixes);
        definition.store("default_language");
        definition.store("english");

        ComplexData input = new MapComplexData(1);
        input.store("index_definition");
        input.storeObject(definition);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexDefinition().getKeyType()).isEqualTo(IndexInfo.IndexDefinition.TargetType.JSON);
        assertThat(result.getIndexDefinition().getPrefixes()).containsExactly("product:");
        assertThat(result.getIndexDefinition().getDefaultLanguage()).isEqualTo("english");
    }

    @Test
    void shouldParseTextFieldResp2() {
        ComplexData field = new ArrayComplexData(14);
        field.store("identifier");
        field.store("title");
        field.store("attribute");
        field.store("title");
        field.store("type");
        field.store("TEXT");
        field.store("WEIGHT");
        field.store("2.0");
        field.store("NOSTEM");
        field.store("NOSTEM");
        field.store("SORTABLE");
        field.store("SORTABLE");
        field.store("WITHSUFFIXTRIE");
        field.store("WITHSUFFIXTRIE");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0)).isInstanceOf(IndexInfo.TextField.class);
        IndexInfo.TextField<String> textField = (IndexInfo.TextField<String>) result.getFields().get(0);
        assertThat(textField.getIdentifier()).isEqualTo("title");
        assertThat(textField.getType()).isEqualTo(IndexInfo.Field.FieldType.TEXT);
        assertThat(textField.getWeight()).isEqualTo(2.0);
        assertThat(textField.isNoStem()).isTrue();
        assertThat(textField.isSortable()).isTrue();
        assertThat(textField.isWithSuffixTrie()).isTrue();
    }

    @Test
    void shouldParseNumericFieldResp3() {
        ComplexData field = new MapComplexData(4);
        field.store("identifier");
        field.store("price");
        field.store("attribute");
        field.store("price");
        field.store("type");
        field.store("NUMERIC");
        field.store("SORTABLE");
        field.store("SORTABLE");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new MapComplexData(1);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0)).isInstanceOf(IndexInfo.NumericField.class);
        IndexInfo.NumericField<String> numericField = (IndexInfo.NumericField<String>) result.getFields().get(0);
        assertThat(numericField.getIdentifier()).isEqualTo("price");
        assertThat(numericField.getType()).isEqualTo(IndexInfo.Field.FieldType.NUMERIC);
        assertThat(numericField.isSortable()).isTrue();
    }

    @Test
    void shouldParseTagField() {
        ComplexData field = new ArrayComplexData(12);
        field.store("identifier");
        field.store("tags");
        field.store("attribute");
        field.store("tags");
        field.store("type");
        field.store("TAG");
        field.store("SEPARATOR");
        field.store(",");
        field.store("CASESENSITIVE");
        field.store("CASESENSITIVE");
        field.store("WITHSUFFIXTRIE");
        field.store("WITHSUFFIXTRIE");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0)).isInstanceOf(IndexInfo.TagField.class);
        IndexInfo.TagField<String> tagField = (IndexInfo.TagField<String>) result.getFields().get(0);
        assertThat(tagField.getIdentifier()).isEqualTo("tags");
        assertThat(tagField.getType()).isEqualTo(IndexInfo.Field.FieldType.TAG);
        assertThat(tagField.getSeparator()).isEqualTo(",");
        assertThat(tagField.isCaseSensitive()).isTrue();
        assertThat(tagField.isWithSuffixTrie()).isTrue();
    }

    @Test
    void shouldParseGeoField() {
        ComplexData field = new ArrayComplexData(6);
        field.store("identifier");
        field.store("location");
        field.store("attribute");
        field.store("location");
        field.store("type");
        field.store("GEO");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0)).isInstanceOf(IndexInfo.GeoField.class);
        assertThat(result.getFields().get(0).getType()).isEqualTo(IndexInfo.Field.FieldType.GEO);
    }

    @Test
    void shouldParseGeoshapeField() {
        ComplexData field = new ArrayComplexData(8);
        field.store("identifier");
        field.store("shape");
        field.store("attribute");
        field.store("shape");
        field.store("type");
        field.store("GEOSHAPE");
        field.store("COORD_SYSTEM");
        field.store("SPHERICAL");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0)).isInstanceOf(IndexInfo.GeoshapeField.class);
        IndexInfo.GeoshapeField<String> geoshapeField = (IndexInfo.GeoshapeField<String>) result.getFields().get(0);
        assertThat(geoshapeField.getCoordinateSystem()).isEqualTo(IndexInfo.GeoshapeField.CoordinateSystem.SPHERICAL);
    }

    @Test
    void shouldParseVectorField() {
        ComplexData field = new ArrayComplexData(10);
        field.store("identifier");
        field.store("embedding");
        field.store("attribute");
        field.store("embedding");
        field.store("type");
        field.store("VECTOR");
        field.store("ALGORITHM");
        field.store("HNSW");
        field.store("DIM");
        field.store(128L);

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0)).isInstanceOf(IndexInfo.VectorField.class);
        IndexInfo.VectorField<String> vectorField = (IndexInfo.VectorField<String>) result.getFields().get(0);
        assertThat(vectorField.getAlgorithm()).isEqualTo(IndexInfo.VectorField.Algorithm.HNSW);
        assertThat(vectorField.getAttributes()).containsEntry("DIM", 128L);
    }

    @Test
    void shouldParseSizeStatistics() {
        ComplexData input = new ArrayComplexData(10);
        input.store("inverted_sz_mb");
        input.store(1.5);
        input.store("vector_index_sz_mb");
        input.store(2.5);
        input.store("total_inverted_index_blocks");
        input.store(100L);
        input.store("doc_table_size_mb");
        input.store(0.5);
        input.store("total_index_memory_sz_mb");
        input.store(5.0);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getSizeStats().getInvertedSizeMb()).isEqualTo(1.5);
        assertThat(result.getSizeStats().getVectorIndexSizeMb()).isEqualTo(2.5);
        assertThat(result.getSizeStats().getTotalInvertedIndexBlocks()).isEqualTo(100L);
        assertThat(result.getSizeStats().getDocTableSizeMb()).isEqualTo(0.5);
        assertThat(result.getSizeStats().getTotalIndexMemorySizeMb()).isEqualTo(5.0);
    }

    @Test
    void shouldParseIndexingStatistics() {
        ComplexData input = new ArrayComplexData(8);
        input.store("hash_indexing_failures");
        input.store(5L);
        input.store("total_indexing_time");
        input.store(123.45);
        input.store("indexing");
        input.store(1L);
        input.store("percent_indexed");
        input.store(99.5);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexingStats().getHashIndexingFailures()).isEqualTo(5L);
        assertThat(result.getIndexingStats().getTotalIndexingTime()).isEqualTo(123.45);
        assertThat(result.getIndexingStats().isIndexing()).isTrue();
        assertThat(result.getIndexingStats().getPercentIndexed()).isEqualTo(99.5);
    }

    @Test
    void shouldParseGcStatisticsResp2() {
        ComplexData gcStats = new ArrayComplexData(8);
        gcStats.store("bytes_collected");
        gcStats.store(1024L);
        gcStats.store("total_ms_run");
        gcStats.store(50.5);
        gcStats.store("total_cycles");
        gcStats.store(10L);
        gcStats.store("average_cycle_time_ms");
        gcStats.store(5.05);

        ComplexData input = new ArrayComplexData(2);
        input.store("gc_stats");
        input.storeObject(gcStats);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getGcStats().getBytesCollected()).isEqualTo(1024L);
        assertThat(result.getGcStats().getTotalMsRun()).isEqualTo(50.5);
        assertThat(result.getGcStats().getTotalCycles()).isEqualTo(10L);
        assertThat(result.getGcStats().getAverageCycleTimeMs()).isEqualTo(5.05);
    }

    @Test
    void shouldParseGcStatisticsResp3() {
        ComplexData gcStats = new MapComplexData(3);
        gcStats.store("bytes_collected");
        gcStats.store(2048L);
        gcStats.store("total_cycles");
        gcStats.store(20L);
        gcStats.store("gc_blocks_denied");
        gcStats.store(3L);

        ComplexData input = new MapComplexData(1);
        input.store("gc_stats");
        input.storeObject(gcStats);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getGcStats().getBytesCollected()).isEqualTo(2048L);
        assertThat(result.getGcStats().getTotalCycles()).isEqualTo(20L);
        assertThat(result.getGcStats().getGcBlocksDenied()).isEqualTo(3L);
    }

    @Test
    void shouldParseCursorStatistics() {
        ComplexData cursorStats = new ArrayComplexData(8);
        cursorStats.store("global_idle");
        cursorStats.store(5L);
        cursorStats.store("global_total");
        cursorStats.store(10L);
        cursorStats.store("index_capacity");
        cursorStats.store(128L);
        cursorStats.store("index_total");
        cursorStats.store(3L);

        ComplexData input = new ArrayComplexData(2);
        input.store("cursor_stats");
        input.storeObject(cursorStats);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getCursorStats().getGlobalIdle()).isEqualTo(5L);
        assertThat(result.getCursorStats().getGlobalTotal()).isEqualTo(10L);
        assertThat(result.getCursorStats().getIndexCapacity()).isEqualTo(128L);
        assertThat(result.getCursorStats().getIndexTotal()).isEqualTo(3L);
    }

    @Test
    void shouldParseDialectStatistics() {
        ComplexData dialectStats = new ArrayComplexData(8);
        dialectStats.store("dialect_1");
        dialectStats.store(10L);
        dialectStats.store("dialect_2");
        dialectStats.store(20L);
        dialectStats.store("dialect_3");
        dialectStats.store(30L);
        dialectStats.store("dialect_4");
        dialectStats.store(40L);

        ComplexData input = new ArrayComplexData(2);
        input.store("dialect_stats");
        input.storeObject(dialectStats);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getDialectStats().getDialect1()).isEqualTo(10L);
        assertThat(result.getDialectStats().getDialect2()).isEqualTo(20L);
        assertThat(result.getDialectStats().getDialect3()).isEqualTo(30L);
        assertThat(result.getDialectStats().getDialect4()).isEqualTo(40L);
    }

    @Test
    void shouldParseIndexErrors() {
        ComplexData indexErrors = new ArrayComplexData(6);
        indexErrors.store("indexing failures");
        indexErrors.store(5L);
        indexErrors.store("last indexing error");
        indexErrors.store("Document is too large");
        indexErrors.store("last indexing error key");
        indexErrors.store("doc:12345");

        ComplexData input = new ArrayComplexData(2);
        input.store("Index Errors");
        input.storeObject(indexErrors);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexErrors().getIndexingFailures()).isEqualTo(5L);
        assertThat(result.getIndexErrors().getLastIndexingError()).isEqualTo("Document is too large");
        assertThat(result.getIndexErrors().getLastIndexingErrorKey()).isEqualTo("doc:12345");
    }

    @Test
    void shouldParseFieldStatistics() {
        ComplexData fieldErrors = new ArrayComplexData(4);
        fieldErrors.store("indexing failures");
        fieldErrors.store(2L);
        fieldErrors.store("last indexing error");
        fieldErrors.store("Invalid field value");

        ComplexData fieldStat = new ArrayComplexData(6);
        fieldStat.store("identifier");
        fieldStat.store("$.name");
        fieldStat.store("attribute");
        fieldStat.store("name");
        fieldStat.store("Index Errors");
        fieldStat.storeObject(fieldErrors);

        ComplexData fieldStatsList = new ArrayComplexData(1);
        fieldStatsList.storeObject(fieldStat);

        ComplexData input = new ArrayComplexData(2);
        input.store("field statistics");
        input.storeObject(fieldStatsList);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFieldStatistics()).hasSize(1);
        IndexInfo.FieldErrorStatistics stats = result.getFieldStatistics().get(0);
        assertThat(stats.getIdentifier()).isEqualTo("$.name");
        assertThat(stats.getAttribute()).isEqualTo("name");
        assertThat(stats.getErrors()).isNotNull();
        assertThat(stats.getErrors().getIndexingFailures()).isEqualTo(2L);
        assertThat(stats.getErrors().getLastIndexingError()).isEqualTo("Invalid field value");
    }

    @Test
    void shouldHandleUnknownFieldType() {
        ComplexData field = new ArrayComplexData(6);
        field.store("identifier");
        field.store("custom_field");
        field.store("attribute");
        field.store("custom_field");
        field.store("type");
        field.store("UNKNOWN_TYPE");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        // Unknown types default to NumericField
        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0)).isInstanceOf(IndexInfo.NumericField.class);
    }

    @Test
    void shouldHandleFieldWithFlags() {
        ComplexData flags = new ArrayComplexData(2);
        flags.store("SORTABLE");
        flags.store("NOINDEX");

        ComplexData field = new MapComplexData(4);
        field.store("identifier");
        field.store("title");
        field.store("type");
        field.store("TEXT");
        field.store("flags");
        field.storeObject(flags);

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new MapComplexData(1);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0).isSortable()).isTrue();
        assertThat(result.getFields().get(0).isNoIndex()).isTrue();
    }

    @Test
    void shouldHandleByteBufferValues() {
        ComplexData input = new ArrayComplexData(4);
        input.store("index_name");
        input.storeObject(ByteBuffer.wrap("byte-buffer-index".getBytes(StandardCharsets.UTF_8)));
        input.store("num_docs");
        input.storeObject(ByteBuffer.wrap("42".getBytes(StandardCharsets.UTF_8)));

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexName()).isEqualTo("byte-buffer-index");
        assertThat(result.getNumDocs()).isEqualTo(42L);
    }

    @Test
    void shouldHandleAdditionalFields() {
        ComplexData input = new ArrayComplexData(4);
        input.store("index_name");
        input.store("test");
        input.store("unknown_field");
        input.store("unknown_value");

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getAdditionalFields()).containsKey("unknown_field");
        assertThat(result.getAdditionalFields().get("unknown_field")).isEqualTo("unknown_value");
    }

    @Test
    void shouldHandleVectorFieldWithSvsVamanaAlgorithm() {
        ComplexData field = new ArrayComplexData(8);
        field.store("identifier");
        field.store("vec");
        field.store("attribute");
        field.store("vec");
        field.store("type");
        field.store("VECTOR");
        field.store("ALGORITHM");
        field.store("SVS-VAMANA");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        IndexInfo.VectorField<String> vectorField = (IndexInfo.VectorField<String>) result.getFields().get(0);
        assertThat(vectorField.getAlgorithm()).isEqualTo(IndexInfo.VectorField.Algorithm.SVS_VAMANA);
    }

    @Test
    void shouldHandleIncompletePair() {
        // Odd number of elements in RESP2 format
        ComplexData input = new ArrayComplexData(3);
        input.store("index_name");
        input.store("test");
        input.store("orphan_key"); // No value for this key

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexName()).isEqualTo("test");
    }

    @Test
    void shouldHandleFieldWithSortableAndFlag() {
        // Test the special case where SORTABLE has a flag value like UNF
        ComplexData field = new ArrayComplexData(8);
        field.store("identifier");
        field.store("text_field");
        field.store("type");
        field.store("TEXT");
        field.store("SORTABLE");
        field.store("UNF");
        field.store("NOSTEM");
        field.store("NOSTEM");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        IndexInfo.TextField<String> textField = (IndexInfo.TextField<String>) result.getFields().get(0);
        assertThat(textField.isSortable()).isTrue();
        assertThat(textField.isUnNormalizedForm()).isTrue();
        assertThat(textField.isNoStem()).isTrue();
    }

    @Test
    void shouldParseMoreSizeStatistics() {
        ComplexData input = new ArrayComplexData(16);
        input.store("offset_vectors_sz_mb");
        input.store(0.25);
        input.store("sortable_values_size_mb");
        input.store(0.3);
        input.store("key_table_size_mb");
        input.store(0.15);
        input.store("geoshapes_sz_mb");
        input.store(0.1);
        input.store("records_per_doc_avg");
        input.store(2.5);
        input.store("bytes_per_record_avg");
        input.store(64.0);
        input.store("offsets_per_term_avg");
        input.store(1.5);
        input.store("offset_bits_per_record_avg");
        input.store(8.0);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getSizeStats().getOffsetVectorsSizeMb()).isEqualTo(0.25);
        assertThat(result.getSizeStats().getSortableValuesSizeMb()).isEqualTo(0.3);
        assertThat(result.getSizeStats().getKeyTableSizeMb()).isEqualTo(0.15);
        assertThat(result.getSizeStats().getGeoshapesSizeMb()).isEqualTo(0.1);
        assertThat(result.getSizeStats().getRecordsPerDocAvg()).isEqualTo(2.5);
        assertThat(result.getSizeStats().getBytesPerRecordAvg()).isEqualTo(64.0);
        assertThat(result.getSizeStats().getOffsetsPerTermAvg()).isEqualTo(1.5);
        assertThat(result.getSizeStats().getOffsetBitsPerRecordAvg()).isEqualTo(8.0);
    }

    @Test
    void shouldParseAdditionalIndexingStatistics() {
        ComplexData input = new ArrayComplexData(4);
        input.store("number_of_uses");
        input.store(42L);
        input.store("cleaning");
        input.store(0L);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexingStats().getNumberOfUses()).isEqualTo(42L);
        assertThat(result.getIndexingStats().isCleaning()).isFalse();
    }

    @Test
    void shouldParseGcStatisticsWithLastRunTime() {
        ComplexData gcStats = new ArrayComplexData(4);
        gcStats.store("last_run_time_ms");
        gcStats.store(12.5);
        gcStats.store("gc_numeric_trees_missed");
        gcStats.store(2L);

        ComplexData input = new ArrayComplexData(2);
        input.store("gc_stats");
        input.storeObject(gcStats);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getGcStats().getLastRunTimeMs()).isEqualTo(12.5);
        assertThat(result.getGcStats().getGcNumericTreesMissed()).isEqualTo(2L);
    }

    @Test
    void shouldParseIndexDefinitionWithLanguageAndScoreFields() {
        ComplexData definition = new ArrayComplexData(8);
        definition.store("key_type");
        definition.store("HASH");
        definition.store("language_field");
        definition.store("lang");
        definition.store("score_field");
        definition.store("doc_score");
        definition.store("payload_field");
        definition.store("payload");

        ComplexData input = new ArrayComplexData(2);
        input.store("index_definition");
        input.storeObject(definition);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexDefinition().getLanguageField()).isEqualTo("lang");
        assertThat(result.getIndexDefinition().getScoreField()).isEqualTo("doc_score");
        assertThat(result.getIndexDefinition().getPayloadField()).isEqualTo("payload");
    }

    @Test
    void shouldHandleTextFieldWithPhonetic() {
        ComplexData field = new ArrayComplexData(10);
        field.store("identifier");
        field.store("name");
        field.store("attribute");
        field.store("name");
        field.store("type");
        field.store("TEXT");
        field.store("PHONETIC");
        field.store("dm:en");
        field.store("INDEXEMPTY");
        field.store("INDEXEMPTY");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        IndexInfo.TextField<String> textField = (IndexInfo.TextField<String>) result.getFields().get(0);
        assertThat(textField.getPhonetic()).isEqualTo("dm:en");
        assertThat(textField.isIndexEmpty()).isTrue();
    }

    @Test
    void shouldHandleFieldWithIndexMissing() {
        ComplexData field = new ArrayComplexData(8);
        field.store("identifier");
        field.store("optional_field");
        field.store("attribute");
        field.store("optional_field");
        field.store("type");
        field.store("TAG");
        field.store("INDEXMISSING");
        field.store("INDEXMISSING");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(1);
        assertThat(result.getFields().get(0).isIndexMissing()).isTrue();
    }

    @Test
    void shouldHandleGeoshapeFieldWithFlatCoordinateSystem() {
        ComplexData field = new ArrayComplexData(8);
        field.store("identifier");
        field.store("region");
        field.store("attribute");
        field.store("region");
        field.store("type");
        field.store("GEOSHAPE");
        field.store("COORD_SYSTEM");
        field.store("FLAT");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        IndexInfo.GeoshapeField<String> geoshapeField = (IndexInfo.GeoshapeField<String>) result.getFields().get(0);
        assertThat(geoshapeField.getCoordinateSystem()).isEqualTo(IndexInfo.GeoshapeField.CoordinateSystem.FLAT);
    }

    @Test
    void shouldHandleVectorFieldWithFlatAlgorithm() {
        ComplexData field = new ArrayComplexData(8);
        field.store("identifier");
        field.store("vec");
        field.store("attribute");
        field.store("vec");
        field.store("type");
        field.store("VECTOR");
        field.store("ALGORITHM");
        field.store("FLAT");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        IndexInfo.VectorField<String> vectorField = (IndexInfo.VectorField<String>) result.getFields().get(0);
        assertThat(vectorField.getAlgorithm()).isEqualTo(IndexInfo.VectorField.Algorithm.FLAT);
    }

    @Test
    void shouldHandleMultipleFields() {
        ComplexData textField = new ArrayComplexData(6);
        textField.store("identifier");
        textField.store("title");
        textField.store("attribute");
        textField.store("title");
        textField.store("type");
        textField.store("TEXT");

        ComplexData numericField = new ArrayComplexData(6);
        numericField.store("identifier");
        numericField.store("price");
        numericField.store("attribute");
        numericField.store("price");
        numericField.store("type");
        numericField.store("NUMERIC");

        ComplexData geoField = new ArrayComplexData(6);
        geoField.store("identifier");
        geoField.store("location");
        geoField.store("attribute");
        geoField.store("location");
        geoField.store("type");
        geoField.store("GEO");

        ComplexData attributes = new ArrayComplexData(3);
        attributes.storeObject(textField);
        attributes.storeObject(numericField);
        attributes.storeObject(geoField);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFields()).hasSize(3);
        assertThat(result.getFields().get(0)).isInstanceOf(IndexInfo.TextField.class);
        assertThat(result.getFields().get(1)).isInstanceOf(IndexInfo.NumericField.class);
        assertThat(result.getFields().get(2)).isInstanceOf(IndexInfo.GeoField.class);
    }

    @Test
    void shouldHandleTagOverheadSizeStats() {
        ComplexData input = new ArrayComplexData(4);
        input.store("tag_overhead_sz_mb");
        input.store(0.05);
        input.store("text_overhead_sz_mb");
        input.store(0.08);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getSizeStats().getTagOverheadSizeMb()).isEqualTo(0.05);
        assertThat(result.getSizeStats().getTextOverheadSizeMb()).isEqualTo(0.08);
    }

    @Test
    void shouldHandleFieldStatisticsResp3() {
        ComplexData fieldErrors = new MapComplexData(2);
        fieldErrors.store("indexing failures");
        fieldErrors.store(3L);
        fieldErrors.store("last indexing error key");
        fieldErrors.store("doc:999");

        ComplexData fieldStat = new MapComplexData(3);
        fieldStat.store("identifier");
        fieldStat.store("$.title");
        fieldStat.store("attribute");
        fieldStat.store("title");
        fieldStat.store("Index Errors");
        fieldStat.storeObject(fieldErrors);

        ComplexData fieldStatsList = new ArrayComplexData(1);
        fieldStatsList.storeObject(fieldStat);

        ComplexData input = new MapComplexData(1);
        input.store("field statistics");
        input.storeObject(fieldStatsList);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getFieldStatistics()).hasSize(1);
        assertThat(result.getFieldStatistics().get(0).getErrors().getIndexingFailures()).isEqualTo(3L);
        assertThat(result.getFieldStatistics().get(0).getErrors().getLastIndexingErrorKey()).isEqualTo("doc:999");
    }

    @Test
    void shouldHandleUnknownCoordinateSystem() {
        ComplexData field = new ArrayComplexData(8);
        field.store("identifier");
        field.store("shape");
        field.store("attribute");
        field.store("shape");
        field.store("type");
        field.store("GEOSHAPE");
        field.store("COORD_SYSTEM");
        field.store("UNKNOWN_SYSTEM");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        IndexInfo.GeoshapeField<String> geoshapeField = (IndexInfo.GeoshapeField<String>) result.getFields().get(0);
        assertThat(geoshapeField.getCoordinateSystem()).isNull();
    }

    @Test
    void shouldHandleUnknownVectorAlgorithm() {
        ComplexData field = new ArrayComplexData(8);
        field.store("identifier");
        field.store("vec");
        field.store("attribute");
        field.store("vec");
        field.store("type");
        field.store("VECTOR");
        field.store("ALGORITHM");
        field.store("UNKNOWN_ALG");

        ComplexData attributes = new ArrayComplexData(1);
        attributes.storeObject(field);

        ComplexData input = new ArrayComplexData(2);
        input.store("attributes");
        input.storeObject(attributes);

        IndexInfo<String> result = parser.parse(input);

        IndexInfo.VectorField<String> vectorField = (IndexInfo.VectorField<String>) result.getFields().get(0);
        assertThat(vectorField.getAlgorithm()).isNull();
    }

    @Test
    void shouldHandleUnknownKeyType() {
        ComplexData definition = new ArrayComplexData(2);
        definition.store("key_type");
        definition.store("UNKNOWN_TYPE");

        ComplexData input = new ArrayComplexData(2);
        input.store("index_definition");
        input.storeObject(definition);

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getIndexDefinition().getKeyType()).isNull();
    }

    @Test
    void shouldParseNumericStringsAsDouble() {
        ComplexData input = new ArrayComplexData(2);
        input.store("inverted_sz_mb");
        input.store("3.14159");

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getSizeStats().getInvertedSizeMb()).isEqualTo(3.14159);
    }

    @Test
    void shouldHandleInvalidNumericValue() {
        ComplexData input = new ArrayComplexData(2);
        input.store("num_docs");
        input.store("not_a_number");

        IndexInfo<String> result = parser.parse(input);

        assertThat(result.getNumDocs()).isEqualTo(0L);
    }

    // Helper methods
    private ComplexData createResp2BasicIndexInfo() {
        ComplexData input = new ArrayComplexData(10);
        input.store("index_name");
        input.store("test-index");
        input.store("num_docs");
        input.store(100L);
        input.store("max_doc_id");
        input.store(150L);
        input.store("num_terms");
        input.store(500L);
        input.store("num_records");
        input.store(1000L);
        return input;
    }

    private ComplexData createResp3BasicIndexInfo() {
        ComplexData input = new MapComplexData(5);
        input.store("index_name");
        input.store("test-index");
        input.store("num_docs");
        input.store(100L);
        input.store("max_doc_id");
        input.store(150L);
        input.store("num_terms");
        input.store(500L);
        input.store("num_records");
        input.store(1000L);
        return input;
    }

}
