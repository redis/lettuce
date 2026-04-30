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
 * Unit tests for {@link FieldArgs} and its concrete implementations.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class FieldArgsTest {

    /**
     * Concrete implementation of FieldArgs for testing purposes.
     */
    private static class TestFieldArgs<K> extends FieldArgs<K> {

        @Override
        public String getFieldType() {
            return "TEST";
        }

        @Override
        protected void buildTypeSpecificArgs(CommandArgs<K, ?> args) {
            // No type-specific arguments for test field
        }

        public static <K> Builder<K> builder() {
            return new Builder<>();
        }

        public static class Builder<K> extends FieldArgs.Builder<K, TestFieldArgs<K>, Builder<K>> {

            public Builder() {
                super(new TestFieldArgs<>());
            }

        }

    }

    @Test
    void testDefaultFieldArgs() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("test_field").build();

        assertThat(field.getName()).isEqualTo("test_field");
        assertThat(field.getAs()).isEmpty();
        assertThat(field.isSortable()).isFalse();
        assertThat(field.isUnNormalizedForm()).isFalse();
        assertThat(field.isNoIndex()).isFalse();
        assertThat(field.isIndexEmpty()).isFalse();
        assertThat(field.isIndexMissing()).isFalse();
        assertThat(field.getFieldType()).isEqualTo("TEST");
    }

    @Test
    void testFieldArgsWithAlias() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("complex_field_name").as("simple_alias").build();

        assertThat(field.getName()).isEqualTo("complex_field_name");
        assertThat(field.getAs()).hasValue("simple_alias");
    }

    @Test
    void testFieldArgsWithSortable() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("sortable_field").sortable().build();

        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isFalse();
    }

    @Test
    void testFieldArgsWithSortableAndUnnormalized() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("sortable_field").sortable().unNormalizedForm()
                .build();

        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
    }

    @Test
    void testFieldArgsWithNoIndex() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("no_index_field").noIndex().build();

        assertThat(field.isNoIndex()).isTrue();
    }

    @Test
    void testFieldArgsWithIndexEmpty() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("index_empty_field").indexEmpty().build();

        assertThat(field.isIndexEmpty()).isTrue();
    }

    @Test
    void testFieldArgsWithIndexMissing() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("index_missing_field").indexMissing().build();

        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testFieldArgsWithAllOptions() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("full_field").as("alias").sortable()
                .unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        assertThat(field.getName()).isEqualTo("full_field");
        assertThat(field.getAs()).hasValue("alias");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testFieldArgsBuild() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("test_field").as("alias").sortable()
                .unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("test_field");
        assertThat(argsString).contains("AS");
        assertThat(argsString).contains("alias");
        assertThat(argsString).contains("TEST"); // Field type
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).contains("UNF");
        assertThat(argsString).contains("NOINDEX");
        assertThat(argsString).contains("INDEXEMPTY");
        assertThat(argsString).contains("INDEXMISSING");
    }

    @Test
    void testFieldArgsMinimalBuild() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("simple_field").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("simple_field");
        assertThat(argsString).contains("TEST"); // Field type
        assertThat(argsString).doesNotContain("AS");
        assertThat(argsString).doesNotContain("SORTABLE");
        assertThat(argsString).doesNotContain("UNF");
        assertThat(argsString).doesNotContain("NOINDEX");
        assertThat(argsString).doesNotContain("INDEXEMPTY");
        assertThat(argsString).doesNotContain("INDEXMISSING");
    }

    @Test
    void testFieldArgsSortableWithoutUnnormalized() {
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("sortable_field").sortable().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).doesNotContain("UNF"); // UNF should only appear with SORTABLE
    }

    @Test
    void testBuilderMethodChaining() {
        // Test that builder methods return the correct type for method chaining
        TestFieldArgs<String> field = TestFieldArgs.<String> builder().name("chained_field").as("chained_alias").sortable()
                .unNormalizedForm().noIndex().indexEmpty().indexMissing().build();

        assertThat(field.getName()).isEqualTo("chained_field");
        assertThat(field.getAs()).hasValue("chained_alias");
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexEmpty()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

}
