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
 * Unit tests for {@link TextFieldArgs}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class TextFieldArgsTest {

    @Test
    void testDefaultTextFieldArgs() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("title").build();

        assertThat(field.getName()).isEqualTo("title");
        assertThat(field.getFieldType()).isEqualTo("TEXT");
        assertThat(field.getWeight()).isEmpty();
        assertThat(field.isNoStem()).isFalse();
        assertThat(field.getPhonetic()).isEmpty();
        assertThat(field.isWithSuffixTrie()).isFalse();
    }

    @Test
    void testTextFieldArgsWithWeight() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("title").weight(2L).build();

        assertThat(field.getWeight()).hasValue(2L);
    }

    @Test
    void testTextFieldArgsWithNoStem() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("title").noStem().build();

        assertThat(field.isNoStem()).isTrue();
    }

    @Test
    void testTextFieldArgsWithPhonetic() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("title")
                .phonetic(TextFieldArgs.PhoneticMatcher.ENGLISH).build();

        assertThat(field.getPhonetic()).hasValue(TextFieldArgs.PhoneticMatcher.ENGLISH);
    }

    @Test
    void testTextFieldArgsWithSuffixTrie() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("title").withSuffixTrie().build();

        assertThat(field.isWithSuffixTrie()).isTrue();
    }

    @Test
    void testTextFieldArgsWithAllOptions() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("content").as("text_content").weight(2L).noStem()
                .phonetic(TextFieldArgs.PhoneticMatcher.FRENCH).withSuffixTrie().sortable().build();

        assertThat(field.getName()).isEqualTo("content");
        assertThat(field.getAs()).hasValue("text_content");
        assertThat(field.getWeight()).hasValue(2L);
        assertThat(field.isNoStem()).isTrue();
        assertThat(field.getPhonetic()).hasValue(TextFieldArgs.PhoneticMatcher.FRENCH);
        assertThat(field.isWithSuffixTrie()).isTrue();
        assertThat(field.isSortable()).isTrue();
    }

    @Test
    void testPhoneticMatcherValues() {
        assertThat(TextFieldArgs.PhoneticMatcher.ENGLISH.getMatcher()).isEqualTo("dm:en");
        assertThat(TextFieldArgs.PhoneticMatcher.FRENCH.getMatcher()).isEqualTo("dm:fr");
        assertThat(TextFieldArgs.PhoneticMatcher.PORTUGUESE.getMatcher()).isEqualTo("dm:pt");
        assertThat(TextFieldArgs.PhoneticMatcher.SPANISH.getMatcher()).isEqualTo("dm:es");
    }

    @Test
    void testTextFieldArgsBuild() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("description").as("desc").weight(3L).noStem()
                .phonetic(TextFieldArgs.PhoneticMatcher.SPANISH).withSuffixTrie().sortable().unNormalizedForm().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("description");
        assertThat(argsString).contains("AS");
        assertThat(argsString).contains("desc");
        assertThat(argsString).contains("TEXT");
        assertThat(argsString).contains("WEIGHT");
        assertThat(argsString).contains("3");
        assertThat(argsString).contains("NOSTEM");
        assertThat(argsString).contains("PHONETIC");
        assertThat(argsString).contains("dm:es");
        assertThat(argsString).contains("WITHSUFFIXTRIE");
        assertThat(argsString).contains("SORTABLE");
        assertThat(argsString).contains("UNF");
    }

    @Test
    void testTextFieldArgsMinimalBuild() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("simple_text").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("simple_text");
        assertThat(argsString).contains("TEXT");
        assertThat(argsString).doesNotContain("WEIGHT");
        assertThat(argsString).doesNotContain("NOSTEM");
        assertThat(argsString).doesNotContain("PHONETIC");
        assertThat(argsString).doesNotContain("WITHSUFFIXTRIE");
        assertThat(argsString).doesNotContain("SORTABLE");
    }

    @Test
    void testTextFieldArgsWithWeightOnly() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("weighted_field").weight(1L).build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("WEIGHT");
        assertThat(argsString).contains("1");
        assertThat(argsString).doesNotContain("NOSTEM");
        assertThat(argsString).doesNotContain("PHONETIC");
        assertThat(argsString).doesNotContain("WITHSUFFIXTRIE");
    }

    @Test
    void testTextFieldArgsWithPhoneticOnly() {
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("phonetic_field")
                .phonetic(TextFieldArgs.PhoneticMatcher.PORTUGUESE).build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        field.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("PHONETIC");
        assertThat(argsString).contains("dm:pt");
        assertThat(argsString).doesNotContain("WEIGHT");
        assertThat(argsString).doesNotContain("NOSTEM");
        assertThat(argsString).doesNotContain("WITHSUFFIXTRIE");
    }

    @Test
    void testBuilderMethodChaining() {
        // Test that builder methods return the correct type for method chaining
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("chained_field").weight(2L).noStem()
                .phonetic(TextFieldArgs.PhoneticMatcher.ENGLISH).withSuffixTrie().sortable().as("alias").build();

        assertThat(field.getName()).isEqualTo("chained_field");
        assertThat(field.getAs()).hasValue("alias");
        assertThat(field.getWeight()).hasValue(2L);
        assertThat(field.isNoStem()).isTrue();
        assertThat(field.getPhonetic()).hasValue(TextFieldArgs.PhoneticMatcher.ENGLISH);
        assertThat(field.isWithSuffixTrie()).isTrue();
        assertThat(field.isSortable()).isTrue();
    }

    @Test
    void testTextFieldArgsInheritedMethods() {
        // Test that inherited methods from FieldArgs work correctly
        TextFieldArgs<String> field = TextFieldArgs.<String> builder().name("inherited_field").noIndex().indexEmpty()
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
