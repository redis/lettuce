/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link CreateArgs}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class CreateArgsTest {

    @Test
    void testDefaultCreateArgs() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().build();

        assertThat(args.getOn()).hasValue(CreateArgs.TargetType.HASH);
        assertThat(args.getPrefixes()).isEmpty();
        assertThat(args.getFilter()).isEmpty();
        assertThat(args.getDefaultLanguage()).isEmpty();
        assertThat(args.getLanguageField()).isEmpty();
        assertThat(args.getDefaultScore()).isEmpty();
        assertThat(args.getScoreField()).isEmpty();
        assertThat(args.getPayloadField()).isEmpty();
        assertThat(args.isMaxTextFields()).isFalse();
        assertThat(args.getTemporary()).isEmpty();
        assertThat(args.isNoOffsets()).isFalse();
        assertThat(args.isNoHighlight()).isFalse();
        assertThat(args.isNoFields()).isFalse();
        assertThat(args.isNoFrequency()).isFalse();
        assertThat(args.isSkipInitialScan()).isFalse();
        assertThat(args.getStopWords()).isEmpty();
    }

    @Test
    void testCreateArgsWithTargetType() {
        CreateArgs<String, String> hashArgs = CreateArgs.<String, String> builder().on(CreateArgs.TargetType.HASH).build();
        assertThat(hashArgs.getOn()).hasValue(CreateArgs.TargetType.HASH);

        CreateArgs<String, String> jsonArgs = CreateArgs.<String, String> builder().on(CreateArgs.TargetType.JSON).build();
        assertThat(jsonArgs.getOn()).hasValue(CreateArgs.TargetType.JSON);
    }

    @Test
    void testCreateArgsWithPrefixes() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().withPrefix("blog:").withPrefix("post:")
                .withPrefix("article:").build();

        assertThat(args.getPrefixes()).containsExactly("blog:", "post:", "article:");
    }

    @Test
    void testCreateArgsWithFilter() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().filter("@status:published").build();

        assertThat(args.getFilter()).hasValue("@status:published");
    }

    @Test
    void testCreateArgsWithLanguageSettings() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().defaultLanguage(DocumentLanguage.ENGLISH)
                .languageField("lang").build();

        assertThat(args.getDefaultLanguage()).hasValue(DocumentLanguage.ENGLISH);
        assertThat(args.getLanguageField()).hasValue("lang");
    }

    @Test
    void testCreateArgsWithScoreSettings() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().defaultScore(0.5).scoreField("score").build();

        assertThat(args.getDefaultScore()).hasValue(0.5);
        assertThat(args.getScoreField()).hasValue("score");
    }

    @Test
    void testCreateArgsWithPayloadField() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().payloadField("payload").build();

        assertThat(args.getPayloadField()).hasValue("payload");
    }

    @Test
    void testCreateArgsWithFlags() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().maxTextFields().noOffsets().noHighlighting()
                .noFields().noFrequency().skipInitialScan().build();

        assertThat(args.isMaxTextFields()).isTrue();
        assertThat(args.isNoOffsets()).isTrue();
        assertThat(args.isNoHighlight()).isTrue();
        assertThat(args.isNoFields()).isTrue();
        assertThat(args.isNoFrequency()).isTrue();
        assertThat(args.isSkipInitialScan()).isTrue();
    }

    @Test
    void testCreateArgsWithTemporary() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().temporary(3600).build();

        assertThat(args.getTemporary()).hasValue(3600L);
    }

    @Test
    void testCreateArgsWithStopWords() {
        List<String> stopWords = Arrays.asList("the", "and", "or", "but");
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().stopWords(stopWords).build();

        assertThat(args.getStopWords()).hasValue(stopWords);
    }

    @Test
    void testCreateArgsWithEmptyStopWords() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().stopWords(Arrays.asList()).build();

        assertThat(args.getStopWords()).hasValue(Arrays.asList());
    }

    @Test
    void testCreateArgsBuild() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().on(CreateArgs.TargetType.JSON)
                .withPrefix("blog:").withPrefix("post:").filter("@status:published").defaultLanguage(DocumentLanguage.FRENCH)
                .languageField("lang").defaultScore(0.8).scoreField("score").payloadField("payload").maxTextFields()
                .temporary(7200).noOffsets().noHighlighting().noFields().noFrequency().skipInitialScan()
                .stopWords(Arrays.asList("le", "la", "et")).build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("JSON");
        assertThat(argsString).contains("PREFIX");
        assertThat(argsString).contains("2");
        assertThat(argsString).contains("FILTER");
        assertThat(argsString).contains("LANGUAGE");
        assertThat(argsString).contains("french");
        assertThat(argsString).contains("LANGUAGE_FIELD");
        assertThat(argsString).contains("SCORE");
        assertThat(argsString).contains("0.8");
        assertThat(argsString).contains("SCORE_FIELD");
        assertThat(argsString).contains("PAYLOAD_FIELD");
        assertThat(argsString).contains("MAXTEXTFIELDS");
        assertThat(argsString).contains("TEMPORARY");
        assertThat(argsString).contains("7200");
        assertThat(argsString).contains("NOOFFSETS");
        assertThat(argsString).contains("NOHL");
        assertThat(argsString).contains("NOFIELDS");
        assertThat(argsString).contains("NOFREQS");
        assertThat(argsString).contains("SKIPINITIALSCAN");
        assertThat(argsString).contains("STOPWORDS");
        assertThat(argsString).contains("3");
        assertThat(argsString).contains("le");
        assertThat(argsString).contains("la");
        assertThat(argsString).contains("et");

    }

    @Test
    void testCreateArgsMinimalBuild() {
        CreateArgs<String, String> args = CreateArgs.<String, String> builder().withPrefix("test:").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("HASH"); // Default target type
        assertThat(argsString).contains("PREFIX");
        assertThat(argsString).contains("1");
        assertThat(argsString).doesNotContain("FILTER");
        assertThat(argsString).doesNotContain("LANGUAGE");
        assertThat(argsString).doesNotContain("SCORE");
        assertThat(argsString).doesNotContain("TEMPORARY");
        assertThat(argsString).doesNotContain("STOPWORDS");
    }

    @Test
    void testTargetTypeEnum() {
        assertThat(CreateArgs.TargetType.HASH.name()).isEqualTo("HASH");
        assertThat(CreateArgs.TargetType.JSON.name()).isEqualTo("JSON");
    }

}
