/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.Test;

import io.lettuce.core.search.SpellCheckResult;
import io.lettuce.core.search.SpellCheckResultParser;

/**
 * Unit tests for {@link SpellCheckResultParser}.
 *
 * @author Tihomir Mateev
 */
class SpellCheckResultParserUnitTests {

    @Test
    void shouldParseEmptySpellCheckResult() {
        SpellCheckResultParser<String, String> parser = new SpellCheckResultParser<>(StringCodec.UTF8);
        ArrayComplexData data = new ArrayComplexData(0);

        SpellCheckResult<String> result = parser.parse(data);

        assertThat(result.hasMisspelledTerms()).isFalse();
        assertThat(result.getMisspelledTermCount()).isEqualTo(0);
        assertThat(result.getMisspelledTerms()).isEmpty();
    }

    @Test
    void shouldParseSingleMisspelledTermWithOneSuggestion() {
        SpellCheckResultParser<String, String> parser = new SpellCheckResultParser<>(StringCodec.UTF8);
        ArrayComplexData data = new ArrayComplexData(1);

        // Create the nested structure for a single misspelled term
        ArrayComplexData termArray = new ArrayComplexData(3);
        termArray.store("TERM");
        termArray.store("reids");

        // Create suggestions array with one suggestion
        ArrayComplexData suggestionsArray = new ArrayComplexData(1);
        ArrayComplexData suggestion = new ArrayComplexData(2);
        suggestion.store("0.7");
        suggestion.store("redis");
        suggestionsArray.storeObject(suggestion);

        termArray.storeObject(suggestionsArray);
        data.storeObject(termArray);

        SpellCheckResult<String> result = parser.parse(data);

        assertThat(result.hasMisspelledTerms()).isTrue();
        assertThat(result.getMisspelledTermCount()).isEqualTo(1);

        SpellCheckResult.MisspelledTerm<String> misspelledTerm = result.getMisspelledTerms().get(0);
        assertThat(misspelledTerm.getTerm()).isEqualTo("reids");
        assertThat(misspelledTerm.hasSuggestions()).isTrue();
        assertThat(misspelledTerm.getSuggestionCount()).isEqualTo(1);

        SpellCheckResult.Suggestion<String> suggestionResult = misspelledTerm.getSuggestions().get(0);
        assertThat(suggestionResult.getScore()).isEqualTo(0.7);
        assertThat(suggestionResult.getSuggestion()).isEqualTo("redis");
    }

    @Test
    void shouldParseMultipleMisspelledTermsWithMultipleSuggestions() {
        SpellCheckResultParser<String, String> parser = new SpellCheckResultParser<>(StringCodec.UTF8);
        ArrayComplexData data = new ArrayComplexData(2);

        // First misspelled term
        ArrayComplexData term1Array = new ArrayComplexData(3);
        term1Array.store("TERM");
        term1Array.store("reids");

        ArrayComplexData suggestions1Array = new ArrayComplexData(2);

        ArrayComplexData suggestion1_1 = new ArrayComplexData(2);
        suggestion1_1.store("0.7");
        suggestion1_1.store("redis");
        suggestions1Array.storeObject(suggestion1_1);

        ArrayComplexData suggestion1_2 = new ArrayComplexData(2);
        suggestion1_2.store("0.5");
        suggestion1_2.store("reads");
        suggestions1Array.storeObject(suggestion1_2);

        term1Array.storeObject(suggestions1Array);
        data.storeObject(term1Array);

        // Second misspelled term
        ArrayComplexData term2Array = new ArrayComplexData(3);
        term2Array.store("TERM");
        term2Array.store("serch");

        ArrayComplexData suggestions2Array = new ArrayComplexData(2);

        ArrayComplexData suggestion2_1 = new ArrayComplexData(2);
        suggestion2_1.store("0.8");
        suggestion2_1.store("search");
        suggestions2Array.storeObject(suggestion2_1);

        ArrayComplexData suggestion2_2 = new ArrayComplexData(2);
        suggestion2_2.store("0.6");
        suggestion2_2.store("serve");
        suggestions2Array.storeObject(suggestion2_2);

        term2Array.storeObject(suggestions2Array);
        data.storeObject(term2Array);

        SpellCheckResult<String> result = parser.parse(data);

        assertThat(result.hasMisspelledTerms()).isTrue();
        assertThat(result.getMisspelledTermCount()).isEqualTo(2);

        // Check first misspelled term
        SpellCheckResult.MisspelledTerm<String> misspelledTerm1 = result.getMisspelledTerms().get(0);
        assertThat(misspelledTerm1.getTerm()).isEqualTo("reids");
        assertThat(misspelledTerm1.hasSuggestions()).isTrue();
        assertThat(misspelledTerm1.getSuggestionCount()).isEqualTo(2);

        SpellCheckResult.Suggestion<String> suggestion1_1Result = misspelledTerm1.getSuggestions().get(0);
        assertThat(suggestion1_1Result.getScore()).isEqualTo(0.7);
        assertThat(suggestion1_1Result.getSuggestion()).isEqualTo("redis");

        SpellCheckResult.Suggestion<String> suggestion1_2Result = misspelledTerm1.getSuggestions().get(1);
        assertThat(suggestion1_2Result.getScore()).isEqualTo(0.5);
        assertThat(suggestion1_2Result.getSuggestion()).isEqualTo("reads");

        // Check second misspelled term
        SpellCheckResult.MisspelledTerm<String> misspelledTerm2 = result.getMisspelledTerms().get(1);
        assertThat(misspelledTerm2.getTerm()).isEqualTo("serch");
        assertThat(misspelledTerm2.hasSuggestions()).isTrue();
        assertThat(misspelledTerm2.getSuggestionCount()).isEqualTo(2);

        SpellCheckResult.Suggestion<String> suggestion2_1Result = misspelledTerm2.getSuggestions().get(0);
        assertThat(suggestion2_1Result.getScore()).isEqualTo(0.8);
        assertThat(suggestion2_1Result.getSuggestion()).isEqualTo("search");

        SpellCheckResult.Suggestion<String> suggestion2_2Result = misspelledTerm2.getSuggestions().get(1);
        assertThat(suggestion2_2Result.getScore()).isEqualTo(0.6);
        assertThat(suggestion2_2Result.getSuggestion()).isEqualTo("serve");
    }

    @Test
    void shouldThrowExceptionForNullData() {
        SpellCheckResultParser<String, String> parser = new SpellCheckResultParser<>(StringCodec.UTF8);

        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed while parsing FT.SPELLCHECK: data must not be null");
    }

    @Test
    void shouldThrowExceptionForInvalidTermFormat() {
        SpellCheckResultParser<String, String> parser = new SpellCheckResultParser<>(StringCodec.UTF8);
        ArrayComplexData data = new ArrayComplexData(1);

        // Create an invalid term array with only 2 elements (missing suggestions)
        ArrayComplexData termArray = new ArrayComplexData(2);
        termArray.store("TERM");
        termArray.store("reids");
        data.storeObject(termArray);

        assertThatThrownBy(() -> parser.parse(data)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed while parsing FT.SPELLCHECK: each term element must have 3 parts");
    }

    @Test
    void shouldThrowExceptionForInvalidTermMarker() {
        SpellCheckResultParser<String, String> parser = new SpellCheckResultParser<>(StringCodec.UTF8);
        ArrayComplexData data = new ArrayComplexData(1);

        // Create a term array with invalid marker (not "TERM")
        ArrayComplexData termArray = new ArrayComplexData(3);
        termArray.store("INVALID");
        termArray.store("reids");
        termArray.storeObject(new ArrayComplexData(0));
        data.storeObject(termArray);

        assertThatThrownBy(() -> parser.parse(data)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed while parsing FT.SPELLCHECK: expected 'TERM' marker, got: INVALID");
    }

    @Test
    void shouldThrowExceptionForInvalidSuggestionFormat() {
        SpellCheckResultParser<String, String> parser = new SpellCheckResultParser<>(StringCodec.UTF8);
        ArrayComplexData data = new ArrayComplexData(1);

        // Create a term array with invalid suggestion (only 1 element instead of 2)
        ArrayComplexData termArray = new ArrayComplexData(3);
        termArray.store("TERM");
        termArray.store("reids");

        ArrayComplexData suggestionsArray = new ArrayComplexData(1);
        ArrayComplexData invalidSuggestion = new ArrayComplexData(1);
        invalidSuggestion.store("0.7");
        suggestionsArray.storeObject(invalidSuggestion);

        termArray.storeObject(suggestionsArray);
        data.storeObject(termArray);

        assertThatThrownBy(() -> parser.parse(data)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed while parsing FT.SPELLCHECK: each suggestion must have 2 parts [score, suggestion]");
    }

}
