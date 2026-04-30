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
 * Unit tests for {@link TagFieldArgs}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class TagFieldArgsTest {

    @Test
    void testDefaultTagFieldArgs() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("category").build();

        assertThat(field.getName()).isEqualTo("category");
        assertThat(field.getFieldType()).isEqualTo("TAG");
        assertThat(field.getSeparator()).isEmpty();
        assertThat(field.isCaseSensitive()).isFalse();
        assertThat(field.isWithSuffixTrie()).isFalse();
    }

    @Test
    void testTagFieldArgsWithSeparator() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("tags").separator("|").build();

        assertThat(field.getName()).isEqualTo("tags");
        assertThat(field.getSeparator()).hasValue("|");
    }

    @Test
    void testTagFieldArgsWithCaseSensitive() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("status").caseSensitive().build();

        assertThat(field.getName()).isEqualTo("status");
        assertThat(field.isCaseSensitive()).isTrue();
    }

    @Test
    void testTagFieldArgsWithSuffixTrie() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("keywords").withSuffixTrie().build();

        assertThat(field.getName()).isEqualTo("keywords");
        assertThat(field.isWithSuffixTrie()).isTrue();
    }

    @Test
    void testTagFieldArgsWithAllOptions() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("complex_tags").as("tags").separator(";")
                .caseSensitive().withSuffixTrie().sortable().unNormalizedForm().build();

        assertThat(field.getName()).isEqualTo("complex_tags");
        assertThat(field.getAs()).hasValue("tags");
        assertThat(field.getSeparator()).hasValue(";");
        assertThat(field.isCaseSensitive()).isTrue();
        assertThat(field.isWithSuffixTrie()).isTrue();
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isUnNormalizedForm()).isTrue();
    }

    @Test
    void testTagFieldArgsBuild() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("labels").as("tag_labels").separator(",")
                .caseSensitive().withSuffixTrie().sortable().indexEmpty().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("labels");
        assertThat(argsString).contains("AS");
        assertThat(argsString).contains("tag_labels");
        assertThat(argsString).contains("TAG");
        assertThat(argsString).contains("SEPARATOR");
        assertThat(argsString).contains(",");
        assertThat(argsString).contains("CASESENSITIVE");
        assertThat(argsString).contains("WITHSUFFIXTRIE");
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).contains("INDEXEMPTY");
    }

    @Test
    void testTagFieldArgsMinimalBuild() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("simple_tag").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("simple_tag");
        assertThat(argsString).contains("TAG");
        assertThat(argsString).doesNotContain("SEPARATOR");
        assertThat(argsString).doesNotContain("CASESENSITIVE");
        assertThat(argsString).doesNotContain("WITHSUFFIXTRIE");
        assertThat(argsString).doesNotContain("SORTABLE");
    }

    @Test
    void testTagFieldArgsWithSeparatorOnly() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("pipe_separated").separator("|").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("SEPARATOR");
        assertThat(argsString).contains("|");
        assertThat(argsString).doesNotContain("CASESENSITIVE");
        assertThat(argsString).doesNotContain("WITHSUFFIXTRIE");
    }

    @Test
    void testTagFieldArgsWithCaseSensitiveOnly() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("case_sensitive_tag").caseSensitive().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("CASESENSITIVE");
        assertThat(argsString).doesNotContain("SEPARATOR");
        assertThat(argsString).doesNotContain("WITHSUFFIXTRIE");
    }

    @Test
    void testTagFieldArgsWithSuffixTrieOnly() {
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("suffix_trie_tag").withSuffixTrie().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("WITHSUFFIXTRIE");
        assertThat(argsString).doesNotContain("SEPARATOR");
        assertThat(argsString).doesNotContain("CASESENSITIVE");
    }

    @Test
    void testTagFieldArgsWithCustomSeparators() {
        // Test various separator characters
        TagFieldArgs<String> commaField = TagFieldArgs.<String> builder().name("comma_tags").separator(",").build();
        TagFieldArgs<String> pipeField = TagFieldArgs.<String> builder().name("pipe_tags").separator("|").build();
        TagFieldArgs<String> semicolonField = TagFieldArgs.<String> builder().name("semicolon_tags").separator(";").build();
        TagFieldArgs<String> spaceField = TagFieldArgs.<String> builder().name("space_tags").separator(" ").build();

        assertThat(commaField.getSeparator()).hasValue(",");
        assertThat(pipeField.getSeparator()).hasValue("|");
        assertThat(semicolonField.getSeparator()).hasValue(";");
        assertThat(spaceField.getSeparator()).hasValue(" ");
    }

    @Test
    void testBuilderMethodChaining() {
        // Test that builder methods return the correct type for method chaining
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("chained_tag").as("chained_alias").separator(":")
                .caseSensitive().withSuffixTrie().sortable().noIndex().indexMissing().build();

        assertThat(field.getName()).isEqualTo("chained_tag");
        assertThat(field.getAs()).hasValue("chained_alias");
        assertThat(field.getSeparator()).hasValue(":");
        assertThat(field.isCaseSensitive()).isTrue();
        assertThat(field.isWithSuffixTrie()).isTrue();
        assertThat(field.isSortable()).isTrue();
        assertThat(field.isNoIndex()).isTrue();
        assertThat(field.isIndexMissing()).isTrue();
    }

    @Test
    void testTagFieldArgsInheritedMethods() {
        // Test that inherited methods from FieldArgs work correctly
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("inherited_tag").noIndex().indexEmpty().indexMissing()
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

    @Test
    void testTagFieldArgsTypeSpecificBehavior() {
        // Test that tag fields have their specific arguments and not others
        TagFieldArgs<String> field = TagFieldArgs.<String> builder().name("tag_field").separator(",").caseSensitive()
                .withSuffixTrie().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        // Should contain tag-specific arguments
        assertThat(argsString).contains("TAG");
        assertThat(argsString).contains("SEPARATOR");
        assertThat(argsString).contains("CASESENSITIVE");
        assertThat(argsString).contains("WITHSUFFIXTRIE");
        // Should not contain text-specific or numeric-specific arguments
        assertThat(argsString).doesNotContain("WEIGHT");
        assertThat(argsString).doesNotContain("NOSTEM");
        assertThat(argsString).doesNotContain("PHONETIC");
    }

}
