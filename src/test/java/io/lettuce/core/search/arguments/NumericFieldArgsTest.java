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
 * Unit tests for {@link NumericFieldArgs}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class NumericFieldArgsTest {

    @Test
    void testDefaultNumericFieldArgs() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("price").build();

        assertThat(field.getName()).isEqualTo("price");
        assertThat(field.getFieldType()).isEqualTo("NUMERIC");
        assertThat(field.getAs()).isEmpty();
        assertThat(field.isSortable()).isFalse();
        assertThat(field.isUnNormalizedForm()).isFalse();
        assertThat(field.isNoIndex()).isFalse();
        assertThat(field.isIndexEmpty()).isFalse();
        assertThat(field.isIndexMissing()).isFalse();
    }

    @Test
    void testNumericFieldArgsWithAlias() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("product_price").as("price").build();

        assertThat(field.getName()).isEqualTo("product_price");
        assertThat(field.getAs()).hasValue("price");
        assertThat(field.getFieldType()).isEqualTo("NUMERIC");
    }

    @Test
    void testNumericFieldArgsWithSortable() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("rating").sortable().build();

        assertThat(field.getName()).isEqualTo("rating");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isFalse();
    }

    @Test
    void testNumericFieldArgsWithSortableAndUnnormalized() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("score").sortable().unNormalizedForm()
                .build();

        assertThat(field.getName()).isEqualTo("score");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
    }

    @Test
    void testNumericFieldArgsWithNoIndex() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("internal_id").noIndex().build();

        assertThat(field.getName()).isEqualTo("internal_id");
        assertThat(field.isNoIndex()).isTrue();
    }

    @Test
    void testNumericFieldArgsWithIndexEmpty() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("optional_value").indexEmpty().build();

        assertThat(field.getName()).isEqualTo("optional_value");
        assertThat(field.isIndexEmpty()).isTrue();
    }

    @Test
    void testNumericFieldArgsWithIndexMissing() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("nullable_field").indexMissing().build();

        assertThat(field.getName()).isEqualTo("nullable_field");
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testNumericFieldArgsWithAllOptions() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("comprehensive_numeric").as("num").sortable()
                .unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        assertThat(field.getName()).isEqualTo("comprehensive_numeric");
        assertThat(field.getAs()).hasValue("num");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testNumericFieldArgsBuild() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("amount").as("total_amount").sortable()
                .unNormalizedForm().indexEmpty().indexMissing().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("amount");
        assertThat(argsString).contains("AS");
        assertThat(argsString).contains("total_amount");
        assertThat(argsString).contains("NUMERIC");
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).contains("UNF");
        assertThat(argsString).contains("INDEXEMPTY");
        assertThat(argsString).contains("INDEXMISSING");
    }

    @Test
    void testNumericFieldArgsMinimalBuild() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("simple_number").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("simple_number");
        assertThat(argsString).contains("NUMERIC");
        assertThat(argsString).doesNotContain("AS");
        assertThat(argsString).doesNotContain("SORTABLE");
        assertThat(argsString).doesNotContain("UNF");
        assertThat(argsString).doesNotContain("NOINDEX");
        assertThat(argsString).doesNotContain("INDEXEMPTY");
        assertThat(argsString).doesNotContain("INDEXMISSING");
    }

    @Test
    void testNumericFieldArgsSortableWithoutUnnormalized() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("sortable_number").sortable().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).doesNotContain("UNF"); // UNF should only appear with SORTABLE when explicitly set
    }

    @Test
    void testNumericFieldArgsWithNoIndexOnly() {
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("no_index_number").noIndex().build();

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
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("chained_numeric").as("chained_alias")
                .sortable().unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        assertThat(field.getName()).isEqualTo("chained_numeric");
        assertThat(field.getAs()).hasValue("chained_alias");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testNumericFieldArgsTypeSpecificBehavior() {
        // Test that numeric fields don't have type-specific arguments beyond common ones
        NumericFieldArgs<String> field = NumericFieldArgs.<String> builder().name("numeric_field").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        // Should only contain field name and type, no numeric-specific arguments
        assertThat(argsString).contains("numeric_field");
        assertThat(argsString).contains("NUMERIC");
        // Should not contain any text-specific or tag-specific arguments
        assertThat(argsString).doesNotContain("WEIGHT");
        assertThat(argsString).doesNotContain("NOSTEM");
        assertThat(argsString).doesNotContain("PHONETIC");
        assertThat(argsString).doesNotContain("SEPARATOR");
        assertThat(argsString).doesNotContain("CASESENSITIVE");
    }

}
