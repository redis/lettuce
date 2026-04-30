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
 * Unit tests for {@link GeoFieldArgs}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class GeoFieldArgsTest {

    @Test
    void testDefaultGeoFieldArgs() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("location").build();

        assertThat(field.getName()).isEqualTo("location");
        assertThat(field.getFieldType()).isEqualTo("GEO");
        assertThat(field.getAs()).isEmpty();
        assertThat(field.isSortable()).isFalse();
        assertThat(field.isUnNormalizedForm()).isFalse();
        assertThat(field.isNoIndex()).isFalse();
        assertThat(field.isIndexEmpty()).isFalse();
        assertThat(field.isIndexMissing()).isFalse();
    }

    @Test
    void testGeoFieldArgsWithAlias() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("coordinates").as("location").build();

        assertThat(field.getName()).isEqualTo("coordinates");
        assertThat(field.getAs()).hasValue("location");
        assertThat(field.getFieldType()).isEqualTo("GEO");
    }

    @Test
    void testGeoFieldArgsWithSortable() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("position").sortable().build();

        assertThat(field.getName()).isEqualTo("position");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isFalse();
    }

    @Test
    void testGeoFieldArgsWithSortableAndUnnormalized() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("geo_point").sortable().unNormalizedForm().build();

        assertThat(field.getName()).isEqualTo("geo_point");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
    }

    @Test
    void testGeoFieldArgsWithNoIndex() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("internal_location").noIndex().build();

        assertThat(field.getName()).isEqualTo("internal_location");
        assertThat(field.isNoIndex()).isTrue();
    }

    @Test
    void testGeoFieldArgsWithIndexEmpty() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("optional_location").indexEmpty().build();

        assertThat(field.getName()).isEqualTo("optional_location");
        assertThat(field.isIndexEmpty()).isTrue();
    }

    @Test
    void testGeoFieldArgsWithIndexMissing() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("nullable_location").indexMissing().build();

        assertThat(field.getName()).isEqualTo("nullable_location");
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testGeoFieldArgsWithAllOptions() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("comprehensive_geo").as("geo").sortable()
                .unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        assertThat(field.getName()).isEqualTo("comprehensive_geo");
        assertThat(field.getAs()).hasValue("geo");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testGeoFieldArgsBuild() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("store_location").as("location").sortable()
                .unNormalizedForm().indexEmpty().indexMissing().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("store_location");
        assertThat(argsString).contains("AS");
        assertThat(argsString).contains("location");
        assertThat(argsString).contains("GEO");
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).contains("UNF");
        assertThat(argsString).contains("INDEXEMPTY");
        assertThat(argsString).contains("INDEXMISSING");
    }

    @Test
    void testGeoFieldArgsMinimalBuild() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("simple_geo").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("simple_geo");
        assertThat(argsString).contains("GEO");
        assertThat(argsString).doesNotContain("AS");
        assertThat(argsString).doesNotContain("SORTABLE");
        assertThat(argsString).doesNotContain("UNF");
        assertThat(argsString).doesNotContain("NOINDEX");
        assertThat(argsString).doesNotContain("INDEXEMPTY");
        assertThat(argsString).doesNotContain("INDEXMISSING");
    }

    @Test
    void testGeoFieldArgsSortableWithoutUnnormalized() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("sortable_geo").sortable().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).doesNotContain("UNF"); // UNF should only appear with SORTABLE when explicitly set
    }

    @Test
    void testGeoFieldArgsWithNoIndexOnly() {
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("no_index_geo").noIndex().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("NOINDEX");
        assertThat(argsString).doesNotContain("SORTABLE");
        assertThat(argsString).doesNotContain("INDEXEMPTY");
        assertThat(argsString).doesNotContain("INDEXMISSING");
    }

    @Test
    void testBuilderMethodChaining() {
        // Test that builder methods return the correct type for method chaining
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("chained_geo").as("chained_alias").sortable()
                .unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        assertThat(field.getName()).isEqualTo("chained_geo");
        assertThat(field.getAs()).hasValue("chained_alias");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testGeoFieldArgsTypeSpecificBehavior() {
        // Test that geo fields don't have type-specific arguments beyond common ones
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("geo_field").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        // Should only contain field name and type, no geo-specific arguments
        assertThat(argsString).contains("geo_field");
        assertThat(argsString).contains("GEO");
        // Should not contain any text-specific, tag-specific, or numeric-specific arguments
        assertThat(argsString).doesNotContain("WEIGHT");
        assertThat(argsString).doesNotContain("NOSTEM");
        assertThat(argsString).doesNotContain("PHONETIC");
        assertThat(argsString).doesNotContain("SEPARATOR");
        assertThat(argsString).doesNotContain("CASESENSITIVE");
        assertThat(argsString).doesNotContain("WITHSUFFIXTRIE");
    }

    @Test
    void testGeoFieldArgsInheritedMethods() {
        // Test that inherited methods from FieldArgs work correctly
        GeoFieldArgs<String> field = GeoFieldArgs.<String> builder().name("inherited_geo").noIndex().indexEmpty().indexMissing()
                .build();

        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("NOINDEX");
        assertThat(argsString).contains("INDEXEMPTY");
        assertThat(argsString).contains("INDEXMISSING");
    }

}
