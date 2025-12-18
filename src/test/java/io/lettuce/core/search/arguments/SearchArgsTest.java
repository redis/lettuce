/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link SearchArgs}.
 *
 * @author Tihomir Mateev
 */
class SearchArgsTest {

    @Test
    void testDefaultSearchArgs() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().build();

        assertThat(args.isNoContent()).isFalse();
        assertThat(args.isWithScores()).isFalse();
        assertThat(args.isWithSortKeys()).isFalse();
    }

    @Test
    void testSearchArgsWithOptions() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().noContent().withScores().withSortKeys()
                .verbatim().build();

        assertThat(args.isNoContent()).isTrue();
        assertThat(args.isWithScores()).isTrue();
        assertThat(args.isWithSortKeys()).isTrue();
    }

    @Test
    void testSearchArgsWithFields() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().inKey("key1").inKey("key2").inField("field1")
                .inField("field2").returnField("title").returnField("content", "text").build();

        // Test that the args can be built without errors
        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        // The command args should contain the appropriate keywords
        String argsString = commandArgs.toString();
        assertThat(argsString).contains("INKEYS");
        assertThat(argsString).contains("INFIELDS");
        assertThat(argsString).contains("RETURN");
    }

    @Test
    void testSearchArgsWithLimitAndTimeout() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().limit(10, 20).timeout(Duration.ofSeconds(5))
                .slop(2).inOrder().build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("LIMIT");
        assertThat(argsString).contains("TIMEOUT");
        assertThat(argsString).contains("SLOP");
        assertThat(argsString).contains("INORDER");
    }

    @Test
    void testSearchArgsWithLanguageAndScoring() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().language(DocumentLanguage.ENGLISH)
                .scorer(ScoringFunction.TF_IDF).build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("LANGUAGE");
        assertThat(argsString).contains("SCORER");
    }

    @Test
    void testSearchArgsWithParams() {
        SearchArgs<String, String> args = SearchArgs.<String, String> builder().param("param1", "value1")
                .param("param2", "value2").dialect(QueryDialects.DIALECT3).build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("PARAMS");
        assertThat(argsString).contains("DIALECT");
        assertThat(argsString).contains("3"); // DIALECT3
    }

    @Test
    void testSearchArgsWithSortBy() {
        SortByArgs<String> sortBy = SortByArgs.<String> builder().attribute("score").descending().build();

        SearchArgs<String, String> args = SearchArgs.<String, String> builder().sortBy(sortBy).build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("SORTBY");
    }

    @Test
    void testSearchArgsWithHighlightAndSummarize() {
        HighlightArgs<String, String> highlight = HighlightArgs.<String, String> builder().field("title").tags("<b>", "</b>")
                .build();

        SearchArgs<String, String> args = SearchArgs.<String, String> builder().highlightArgs(highlight)
                .summarizeField("content").summarizeFragments(3).summarizeLen(100).summarizeSeparator("...").build();

        CommandArgs<String, String> commandArgs = new CommandArgs<>(StringCodec.UTF8);
        args.build(commandArgs);

        String argsString = commandArgs.toString();
        assertThat(argsString).contains("HIGHLIGHT");
        assertThat(argsString).contains("SUMMARIZE");
    }

}
