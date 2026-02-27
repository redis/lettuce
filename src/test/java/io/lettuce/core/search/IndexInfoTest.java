/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link IndexInfo}.
 *
 * @author Julien Ruaux
 */
@Tag(UNIT_TEST)
class IndexInfoTest {

    @Test
    void testIndexInfoBasicProperties() {
        IndexInfo<String> info = new IndexInfo<>();
        info.setIndexName("myindex");
        info.setNumDocs(100L);
        info.setMaxDocId(150L);
        info.setNumTerms(500L);
        info.setNumRecords(1000L);

        assertThat(info.getIndexName()).isEqualTo("myindex");
        assertThat(info.getNumDocs()).isEqualTo(100L);
        assertThat(info.getMaxDocId()).isEqualTo(150L);
        assertThat(info.getNumTerms()).isEqualTo(500L);
        assertThat(info.getNumRecords()).isEqualTo(1000L);
    }

    @Test
    void testIndexInfoOptions() {
        IndexInfo<String> info = new IndexInfo<>();
        info.setNoOffsets();
        info.setNoFrequency();

        assertThat(info.isNoOffsets()).isTrue();
        assertThat(info.isNoFrequency()).isTrue();
        assertThat(info.isNoHighlight()).isFalse();
        assertThat(info.isNoFields()).isFalse();
        assertThat(info.isMaxTextFields()).isFalse();
        assertThat(info.isSkipInitialScan()).isFalse();
    }

    @Test
    void testIndexInfoDefinition() {
        IndexInfo<String> info = new IndexInfo<>();
        IndexInfo.IndexDefinition<String> definition = new IndexInfo.IndexDefinition<>();
        definition.setKeyType(IndexInfo.IndexDefinition.TargetType.HASH);
        definition.addPrefix("product:");
        definition.setDefaultScore(1.0);
        info.setIndexDefinition(definition);

        assertThat(info.getIndexDefinition()).isNotNull();
        assertThat(info.getIndexDefinition().getKeyType()).isEqualTo(IndexInfo.IndexDefinition.TargetType.HASH);
        assertThat(info.getIndexDefinition().getPrefixes()).containsExactly("product:");
        assertThat(info.getIndexDefinition().getDefaultScore()).isEqualTo(1.0);
    }

    @Test
    void testIndexInfoFields() {
        IndexInfo<String> info = new IndexInfo<>();

        IndexInfo.TextField textField = new IndexInfo.TextField("title", null, false, false, false, false, false, null, false,
                null, false, new HashMap<>());
        info.addField(textField);

        IndexInfo.NumericField numericField = new IndexInfo.NumericField("price", null, false, false, false, false, false,
                new HashMap<>());
        info.addField(numericField);

        assertThat(info.getFields()).hasSize(2);
        assertThat(info.getFields().get(0)).isInstanceOf(IndexInfo.TextField.class);
        assertThat(info.getFields().get(0).getIdentifier()).isEqualTo("title");
        assertThat(info.getFields().get(1)).isInstanceOf(IndexInfo.NumericField.class);
        assertThat(info.getFields().get(1).getIdentifier()).isEqualTo("price");
    }

    @Test
    void testIndexInfoDialectStatistics() {
        IndexInfo<String> info = new IndexInfo<>();
        IndexInfo.DialectStatistics dialectStats = new IndexInfo.DialectStatistics();
        dialectStats.setDialect1(10L);
        dialectStats.setDialect2(20L);
        dialectStats.setDialect3(30L);
        dialectStats.setDialect4(40L);
        info.setDialectStats(dialectStats);

        assertThat(info.getDialectStats()).isNotNull();
        assertThat(info.getDialectStats().getDialect1()).isEqualTo(10L);
        assertThat(info.getDialectStats().getDialect2()).isEqualTo(20L);
        assertThat(info.getDialectStats().getDialect3()).isEqualTo(30L);
        assertThat(info.getDialectStats().getDialect4()).isEqualTo(40L);
    }

    @Test
    void testIndexInfoImmutability() {
        IndexInfo<String> info = new IndexInfo<>();
        info.setNoOffsets();
        IndexInfo.IndexDefinition<String> definition = new IndexInfo.IndexDefinition<>();
        definition.setKeyType(IndexInfo.IndexDefinition.TargetType.HASH);
        info.setIndexDefinition(definition);

        // Verify collections are unmodifiable
        assertThatThrownBy(() -> info.getIndexDefinition().getPrefixes().add("new_prefix"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testIndexInfoToString() {
        IndexInfo<String> info = new IndexInfo<>();
        info.setIndexName("myindex");
        info.setNumDocs(100L);
        info.setMaxDocId(150L);
        info.setNumTerms(500L);
        info.setNumRecords(1000L);

        String str = info.toString();
        assertThat(str).contains("myindex").contains("100").contains("150").contains("500").contains("1000");
    }

    @Test
    void testIndexInfoObjectsInitialized() {
        IndexInfo<String> info = new IndexInfo<>();

        // Verify all nested objects are initialized (not null)
        assertThat(info.getIndexDefinition()).isNotNull();
        assertThat(info.getSizeStats()).isNotNull();
        assertThat(info.getIndexingStats()).isNotNull();
        assertThat(info.getGcStats()).isNotNull();
        assertThat(info.getCursorStats()).isNotNull();
        assertThat(info.getDialectStats()).isNotNull();
        assertThat(info.getIndexErrors()).isNotNull();
        assertThat(info.getFieldStatistics()).isNotNull();
    }

}
