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
 * Unit tests for {@link GeoshapeFieldArgs}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class GeoshapeFieldArgsTest {

    @Test
    void testDefaultGeoshapeFieldArgs() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("geometry").build();

        assertThat(field.getName()).isEqualTo("geometry");
        assertThat(field.getFieldType()).isEqualTo("GEOSHAPE");
        assertThat(field.getCoordinateSystem()).isEmpty();
        assertThat(field.getAs()).isEmpty();
        assertThat(field.isSortable()).isFalse();
        assertThat(field.isUnNormalizedForm()).isFalse();
        assertThat(field.isNoIndex()).isFalse();
        assertThat(field.isIndexEmpty()).isFalse();
        assertThat(field.isIndexMissing()).isFalse();
    }

    @Test
    void testGeoshapeFieldArgsWithSpherical() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("shape").spherical().build();

        assertThat(field.getName()).isEqualTo("shape");
        assertThat(field.getCoordinateSystem()).hasValue(GeoshapeFieldArgs.CoordinateSystem.SPHERICAL);
    }

    @Test
    void testGeoshapeFieldArgsWithFlat() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("polygon").flat().build();

        assertThat(field.getName()).isEqualTo("polygon");
        assertThat(field.getCoordinateSystem()).hasValue(GeoshapeFieldArgs.CoordinateSystem.FLAT);
    }

    @Test
    void testGeoshapeFieldArgsWithAlias() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("complex_geometry").as("geom").build();

        assertThat(field.getName()).isEqualTo("complex_geometry");
        assertThat(field.getAs()).hasValue("geom");
        assertThat(field.getFieldType()).isEqualTo("GEOSHAPE");
    }

    @Test
    void testGeoshapeFieldArgsWithSortable() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("sortable_shape").sortable().build();

        assertThat(field.getName()).isEqualTo("sortable_shape");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isFalse();
    }

    @Test
    void testGeoshapeFieldArgsWithAllOptions() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("comprehensive_geoshape").as("shape").flat()
                .sortable().unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        assertThat(field.getName()).isEqualTo("comprehensive_geoshape");
        assertThat(field.getAs()).hasValue("shape");
        assertThat(field.getCoordinateSystem()).hasValue(GeoshapeFieldArgs.CoordinateSystem.FLAT);
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testCoordinateSystemEnum() {
        assertThat(GeoshapeFieldArgs.CoordinateSystem.FLAT.name()).isEqualTo("FLAT");
        assertThat(GeoshapeFieldArgs.CoordinateSystem.SPHERICAL.name()).isEqualTo("SPHERICAL");
    }

    @Test
    void testGeoshapeFieldArgsBuildWithSpherical() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("spherical_shape").as("shape").spherical()
                .sortable().indexEmpty().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("spherical_shape");
        assertThat(argsString).contains("AS");
        assertThat(argsString).contains("shape");
        assertThat(argsString).contains("GEOSHAPE");
        assertThat(argsString).contains("SPHERICAL");
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).contains("INDEXEMPTY");
    }

    @Test
    void testGeoshapeFieldArgsBuildWithFlat() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("flat_shape").as("cartesian").flat()
                .sortable().unNormalizedForm().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("flat_shape");
        assertThat(argsString).contains("AS");
        assertThat(argsString).contains("cartesian");
        assertThat(argsString).contains("GEOSHAPE");
        assertThat(argsString).contains("FLAT");
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).contains("UNF");
    }

    @Test
    void testGeoshapeFieldArgsMinimalBuild() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("simple_geoshape").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("simple_geoshape");
        assertThat(argsString).contains("GEOSHAPE");
        assertThat(argsString).doesNotContain("AS");
        assertThat(argsString).doesNotContain("SPHERICAL");
        assertThat(argsString).doesNotContain("FLAT");
        assertThat(argsString).doesNotContain("SORTABLE");
        assertThat(argsString).doesNotContain("UNF");
        assertThat(argsString).doesNotContain("NOINDEX");
        assertThat(argsString).doesNotContain("INDEXEMPTY");
        assertThat(argsString).doesNotContain("INDEXMISSING");
    }

    @Test
    void testGeoshapeFieldArgsWithNoIndex() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("no_index_shape").noIndex().build();

        assertThat(field.getName()).isEqualTo("no_index_shape");
        assertThat(field.isNoIndex()).isTrue();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("NOINDEX");
        assertThat(argsString).doesNotContain("SORTABLE");
        assertThat(argsString).doesNotContain("INDEXEMPTY");
        assertThat(argsString).doesNotContain("INDEXMISSING");
    }

    @Test
    void testGeoshapeFieldArgsWithIndexEmpty() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("index_empty_shape").indexEmpty().build();

        assertThat(field.getName()).isEqualTo("index_empty_shape");
        assertThat(field.isIndexEmpty()).isTrue();
    }

    @Test
    void testGeoshapeFieldArgsWithIndexMissing() {
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("index_missing_shape").indexMissing()
                .build();

        assertThat(field.getName()).isEqualTo("index_missing_shape");
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testBuilderMethodChaining() {
        // Test that builder methods return the correct type for method chaining
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("chained_geoshape").as("chained_alias")
                .spherical().sortable().unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        assertThat(field.getName()).isEqualTo("chained_geoshape");
        assertThat(field.getAs()).hasValue("chained_alias");
        assertThat(field.getCoordinateSystem()).hasValue(GeoshapeFieldArgs.CoordinateSystem.SPHERICAL);
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testGeoshapeFieldArgsTypeSpecificBehavior() {
        // Test that geoshape fields have their specific arguments and not others
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("geoshape_field").flat().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        // Should contain geoshape-specific arguments
        assertThat(argsString).contains("GEOSHAPE");
        assertThat(argsString).contains("FLAT");
        // Should not contain text-specific, tag-specific, or numeric-specific arguments
        assertThat(argsString).doesNotContain("WEIGHT");
        assertThat(argsString).doesNotContain("NOSTEM");
        assertThat(argsString).doesNotContain("PHONETIC");
        assertThat(argsString).doesNotContain("SEPARATOR");
        assertThat(argsString).doesNotContain("CASESENSITIVE");
        assertThat(argsString).doesNotContain("WITHSUFFIXTRIE");
    }

    @Test
    void testGeoshapeFieldArgsInheritedMethods() {
        // Test that inherited methods from FieldArgs work correctly
        GeoshapeFieldArgs<String> field = GeoshapeFieldArgs.<String> builder().name("inherited_geoshape").noIndex().indexEmpty()
                .indexMissing().build();

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
