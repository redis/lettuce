/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.search.Suggestion;
import io.lettuce.core.search.SuggestionParser;

/**
 * Unit tests for {@link SuggestionParser}.
 *
 * @author Tihomir Mateev
 */
class SuggestionParserUnitTests {

    @Test
    void shouldParseBasicSuggestions() {
        SuggestionParser<String> parser = new SuggestionParser<>(false, false);
        ArrayComplexData data = new ArrayComplexData(3);
        data.store("suggestion1");
        data.store("suggestion2");
        data.store("suggestion3");

        List<Suggestion<String>> suggestions = parser.parse(data);

        assertThat(suggestions).hasSize(3);
        assertThat(suggestions.get(0).getValue()).isEqualTo("suggestion1");
        assertThat(suggestions.get(0).hasScore()).isFalse();
        assertThat(suggestions.get(0).hasPayload()).isFalse();
        assertThat(suggestions.get(1).getValue()).isEqualTo("suggestion2");
        assertThat(suggestions.get(2).getValue()).isEqualTo("suggestion3");
    }

    @Test
    void shouldParseSuggestionsWithScores() {
        SuggestionParser<String> parser = new SuggestionParser<>(true, false);
        ArrayComplexData data = new ArrayComplexData(4);
        data.store("suggestion1");
        data.store("1.5");
        data.store("suggestion2");
        data.store("2.0");

        List<Suggestion<String>> suggestions = parser.parse(data);

        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.get(0).getValue()).isEqualTo("suggestion1");
        assertThat(suggestions.get(0).hasScore()).isTrue();
        assertThat(suggestions.get(0).getScore()).isEqualTo(1.5);
        assertThat(suggestions.get(0).hasPayload()).isFalse();
        assertThat(suggestions.get(1).getValue()).isEqualTo("suggestion2");
        assertThat(suggestions.get(1).getScore()).isEqualTo(2.0);
    }

    @Test
    void shouldParseSuggestionsWithPayloads() {
        SuggestionParser<String> parser = new SuggestionParser<>(false, true);
        ArrayComplexData data = new ArrayComplexData(4);
        data.store("suggestion1");
        data.store("payload1");
        data.store("suggestion2");
        data.store("payload2");

        List<Suggestion<String>> suggestions = parser.parse(data);

        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.get(0).getValue()).isEqualTo("suggestion1");
        assertThat(suggestions.get(0).hasScore()).isFalse();
        assertThat(suggestions.get(0).hasPayload()).isTrue();
        assertThat(suggestions.get(0).getPayload()).isEqualTo("payload1");
        assertThat(suggestions.get(1).getValue()).isEqualTo("suggestion2");
        assertThat(suggestions.get(1).getPayload()).isEqualTo("payload2");
    }

    @Test
    void shouldParseSuggestionsWithScoresAndPayloads() {
        SuggestionParser<String> parser = new SuggestionParser<>(true, true);
        ArrayComplexData data = new ArrayComplexData(6);
        data.store("suggestion1");
        data.store("1.5");
        data.store("payload1");
        data.store("suggestion2");
        data.store("2.0");
        data.store("payload2");

        List<Suggestion<String>> suggestions = parser.parse(data);

        assertThat(suggestions).hasSize(2);
        assertThat(suggestions.get(0).getValue()).isEqualTo("suggestion1");
        assertThat(suggestions.get(0).hasScore()).isTrue();
        assertThat(suggestions.get(0).getScore()).isEqualTo(1.5);
        assertThat(suggestions.get(0).hasPayload()).isTrue();
        assertThat(suggestions.get(0).getPayload()).isEqualTo("payload1");
        assertThat(suggestions.get(1).getValue()).isEqualTo("suggestion2");
        assertThat(suggestions.get(1).getScore()).isEqualTo(2.0);
        assertThat(suggestions.get(1).getPayload()).isEqualTo("payload2");
    }

    @Test
    void shouldHandleEmptyList() {
        SuggestionParser<String> parser = new SuggestionParser<>(false, false);
        ArrayComplexData data = new ArrayComplexData(0);

        List<Suggestion<String>> suggestions = parser.parse(data);

        assertThat(suggestions).isEmpty();
    }

    @Test
    void shouldThrowExceptionForNullData() {
        SuggestionParser<String> parser = new SuggestionParser<>(false, false);

        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed while parsing FT.SUGGET: data must not be null");
    }

    @Test
    void shouldThrowExceptionForInvalidScoreFormat() {
        SuggestionParser<String> parser = new SuggestionParser<>(true, false);
        ArrayComplexData data = new ArrayComplexData(3);
        data.store("suggestion1");
        data.store("suggestion2");
        data.store("suggestion3");

        assertThatThrownBy(() -> parser.parse(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed while parsing FT.SUGGET with WITHSCORES: expected even number of elements");
    }

    @Test
    void shouldThrowExceptionForInvalidPayloadFormat() {
        SuggestionParser<String> parser = new SuggestionParser<>(false, true);
        ArrayComplexData data = new ArrayComplexData(3);
        data.store("suggestion1");
        data.store("payload1");
        data.store("suggestion2");

        assertThatThrownBy(() -> parser.parse(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed while parsing FT.SUGGET with WITHPAYLOADS: expected even number of elements");
    }

    @Test
    void shouldThrowExceptionForInvalidScoreAndPayloadFormat() {
        SuggestionParser<String> parser = new SuggestionParser<>(true, true);
        ArrayComplexData data = new ArrayComplexData(5);
        data.store("suggestion1");
        data.store("1.5");
        data.store("payload1");
        data.store("suggestion2");
        data.store("2.0");

        assertThatThrownBy(() -> parser.parse(data))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed while parsing FT.SUGGET with WITHSCORES and WITHPAYLOADS: expected multiple of 3 elements");
    }

}
