/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.search.Suggestion;
import io.lettuce.core.search.SuggestionParser;

/**
 * Unit tests for {@link SuggestionParser}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class SuggestionParserUnitTests {

    private static final StringCodec CODEC = StringCodec.UTF8;

    @Test
    void shouldParseBasicSuggestions() {
        SuggestionParser parser = new SuggestionParser(false, false);
        ArrayComplexData data = new ArrayComplexData(3);
        data.storeObject(CODEC.encodeValue("suggestion1"));
        data.storeObject(CODEC.encodeValue("suggestion2"));
        data.storeObject(CODEC.encodeValue("suggestion3"));

        List<Suggestion> suggestions = parser.parse(data);

        assertThat(suggestions).hasSize(3);
        assertThat(suggestions.get(0).getValue()).isEqualTo("suggestion1");
        assertThat(suggestions.get(0).hasScore()).isFalse();
        assertThat(suggestions.get(0).hasPayload()).isFalse();
        assertThat(suggestions.get(1).getValue()).isEqualTo("suggestion2");
        assertThat(suggestions.get(2).getValue()).isEqualTo("suggestion3");
    }

    @Test
    void shouldParseSuggestionsWithScores() {
        SuggestionParser parser = new SuggestionParser(true, false);
        ArrayComplexData data = new ArrayComplexData(4);
        data.storeObject(CODEC.encodeValue("suggestion1"));
        data.store(1.5);
        data.storeObject(CODEC.encodeValue("suggestion2"));
        data.store(2.0);

        List<Suggestion> suggestions = parser.parse(data);

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
        SuggestionParser parser = new SuggestionParser(false, true);
        ArrayComplexData data = new ArrayComplexData(4);
        data.storeObject(CODEC.encodeValue("suggestion1"));
        data.storeObject(CODEC.encodeValue("payload1"));
        data.storeObject(CODEC.encodeValue("suggestion2"));
        data.storeObject(CODEC.encodeValue("payload2"));

        List<Suggestion> suggestions = parser.parse(data);

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
        SuggestionParser parser = new SuggestionParser(true, true);
        ArrayComplexData data = new ArrayComplexData(6);
        data.storeObject(CODEC.encodeValue("suggestion1"));
        data.store(1.5);
        data.storeObject(CODEC.encodeValue("payload1"));
        data.storeObject(CODEC.encodeValue("suggestion2"));
        data.store(2.0);
        data.storeObject(CODEC.encodeValue("payload2"));

        List<Suggestion> suggestions = parser.parse(data);

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
        SuggestionParser parser = new SuggestionParser(false, false);
        ArrayComplexData data = new ArrayComplexData(0);

        List<Suggestion> suggestions = parser.parse(data);
        assertThat(suggestions).isEmpty();
    }

    @Test
    void shouldThrowExceptionForNullData() {
        SuggestionParser parser = new SuggestionParser(false, false);

        List<Suggestion> suggestions = parser.parse(null);
        assertThat(suggestions).isEmpty();
    }

    @Test
    void shouldThrowExceptionForInvalidScoreFormat() {
        SuggestionParser parser = new SuggestionParser(true, false);
        ArrayComplexData data = new ArrayComplexData(3);
        data.store("suggestion1");
        data.store("suggestion2");
        data.store("suggestion3");

        List<Suggestion> suggestions = parser.parse(data);
        assertThat(suggestions).hasSize(0);
    }

    @Test
    void shouldThrowExceptionForInvalidPayloadFormat() {
        SuggestionParser parser = new SuggestionParser(false, true);
        ArrayComplexData data = new ArrayComplexData(3);
        data.store("suggestion1");
        data.store("payload1");
        data.store("suggestion2");

        List<Suggestion> suggestions = parser.parse(data);
        assertThat(suggestions).hasSize(0);
    }

    @Test
    void shouldThrowExceptionForInvalidScoreAndPayloadFormat() {
        SuggestionParser parser = new SuggestionParser(true, true);
        ArrayComplexData data = new ArrayComplexData(5);
        data.store("suggestion1");
        data.store(1.5);
        data.store("payload1");
        data.store("suggestion2");
        data.store(2.0);

        List<Suggestion> suggestions = parser.parse(data);
        assertThat(suggestions).hasSize(0);
    }

}
