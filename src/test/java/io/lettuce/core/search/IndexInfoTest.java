/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link IndexInfo}.
 *
 * @author Julien Ruaux
 */
class IndexInfoTest {

    @Test
    void testIndexInfoBasicProperties() {
        IndexInfo info = new IndexInfo();
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
        IndexInfo info = new IndexInfo();
        info.addIndexOption("NOOFFSETS");
        info.addIndexOption("NOFREQS");

        assertThat(info.getIndexOptions()).containsExactly("NOOFFSETS", "NOFREQS");
    }

    @Test
    void testIndexInfoDefinition() {
        IndexInfo info = new IndexInfo();
        info.putIndexDefinition("key_type", "HASH");
        info.putIndexDefinition("prefixes", "product:");
        info.putIndexDefinition("default_score", 1.0);

        assertThat(info.getIndexDefinition()).containsEntry("key_type", "HASH").containsEntry("prefixes", "product:")
                .containsEntry("default_score", 1.0);
    }

    @Test
    void testIndexInfoAttributes() {
        IndexInfo info = new IndexInfo();

        Map<String, Object> attr1 = new HashMap<>();
        attr1.put("identifier", "title");
        attr1.put("type", "TEXT");
        info.addAttribute(attr1);

        Map<String, Object> attr2 = new HashMap<>();
        attr2.put("identifier", "price");
        attr2.put("type", "NUMERIC");
        info.addAttribute(attr2);

        assertThat(info.getAttributes()).hasSize(2);
        assertThat(info.getAttributes().get(0)).containsEntry("identifier", "title").containsEntry("type", "TEXT");
        assertThat(info.getAttributes().get(1)).containsEntry("identifier", "price").containsEntry("type", "NUMERIC");
    }

    @Test
    void testIndexInfoSizeStatistics() {
        IndexInfo info = new IndexInfo();
        info.putSizeStatistic("inverted_sz_mb", 2.5);
        info.putSizeStatistic("doc_table_size_mb", 1.2);

        assertThat(info.getSizeStatistics()).containsEntry("inverted_sz_mb", 2.5).containsEntry("doc_table_size_mb", 1.2);
    }

    @Test
    void testIndexInfoIndexingStatistics() {
        IndexInfo info = new IndexInfo();
        info.putIndexingStatistic("indexing", 0);
        info.putIndexingStatistic("percent_indexed", 1.0);
        info.putIndexingStatistic("hash_indexing_failures", 0);

        assertThat(info.getIndexingStatistics()).containsEntry("indexing", 0).containsEntry("percent_indexed", 1.0)
                .containsEntry("hash_indexing_failures", 0);
    }

    @Test
    void testIndexInfoGcStatistics() {
        IndexInfo info = new IndexInfo();
        info.putGcStatistic("bytes_collected", 1024L);
        info.putGcStatistic("total_cycles", 5L);

        assertThat(info.getGcStatistics()).containsEntry("bytes_collected", 1024L).containsEntry("total_cycles", 5L);
    }

    @Test
    void testIndexInfoCursorStatistics() {
        IndexInfo info = new IndexInfo();
        info.putCursorStatistic("global_idle", 0);
        info.putCursorStatistic("global_total", 0);

        assertThat(info.getCursorStatistics()).containsEntry("global_idle", 0).containsEntry("global_total", 0);
    }

    @Test
    void testIndexInfoDialectStatistics() {
        IndexInfo info = new IndexInfo();
        info.putDialectStatistic("dialect_1", 10L);
        info.putDialectStatistic("dialect_2", 20L);

        assertThat(info.getDialectStatistics()).containsEntry("dialect_1", 10L).containsEntry("dialect_2", 20L);
    }

    @Test
    void testIndexInfoImmutability() {
        IndexInfo info = new IndexInfo();
        info.addIndexOption("NOOFFSETS");
        info.putIndexDefinition("key_type", "HASH");
        info.putSizeStatistic("inverted_sz_mb", 2.5);

        // Verify collections are unmodifiable
        assertThatThrownBy(() -> info.getIndexOptions().add("ANOTHER")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> info.getIndexDefinition().put("new_key", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> info.getSizeStatistics().put("new_stat", 1.0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testIndexInfoToString() {
        IndexInfo info = new IndexInfo();
        info.setIndexName("myindex");
        info.setNumDocs(100L);
        info.setMaxDocId(150L);
        info.setNumTerms(500L);
        info.setNumRecords(1000L);

        String str = info.toString();
        assertThat(str).contains("myindex").contains("100").contains("150").contains("500").contains("1000");
    }

}
