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

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.search.SynonymMapParser;

/**
 * Unit tests for {@link SynonymMapParser}.
 *
 * @author Tihomir Mateev
 */
class SynonymMapParserUnitTests {

    private final StringCodec codec = StringCodec.UTF8;

    private final SynonymMapParser<String, String> parser = new SynonymMapParser<>(codec);

    @Test
    void shouldParseResp2Format() {
        // RESP2: ["term1", ["synonym1", "synonym2"], "term2", ["synonym3"]]
        ComplexData data = new ArrayComplexData(4);

        // Add term1
        data.store("term1");

        // Add synonyms for term1
        ComplexData synonyms1 = new ArrayComplexData(2);
        synonyms1.store("synonym1");
        synonyms1.store("synonym2");
        data.storeObject(synonyms1);

        // Add term2
        data.store("term2");

        // Add synonyms for term2
        ComplexData synonyms2 = new ArrayComplexData(1);
        synonyms2.store("synonym3");
        data.storeObject(synonyms2);

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).hasSize(2);
        assertThat(result.get("term1")).containsExactly("synonym1", "synonym2");
        assertThat(result.get("term2")).containsExactly("synonym3");
    }

    @Test
    void shouldParseResp3Format() {
        // RESP3: {"term1": ["synonym1", "synonym2"], "term2": ["synonym3"]}
        ComplexData data = new MapComplexData(2);

        // Add term1 and its synonyms
        data.store("term1");
        ComplexData synonyms1 = new ArrayComplexData(2);
        synonyms1.store("synonym1");
        synonyms1.store("synonym2");
        data.storeObject(synonyms1);

        // Add term2 and its synonyms
        data.store("term2");
        ComplexData synonyms2 = new ArrayComplexData(1);
        synonyms2.store("synonym3");
        data.storeObject(synonyms2);

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).hasSize(2);
        assertThat(result.get("term1")).containsExactly("synonym1", "synonym2");
        assertThat(result.get("term2")).containsExactly("synonym3");
    }

    @Test
    void shouldHandleEmptyResp2() {
        ComplexData data = new ArrayComplexData(0);

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyResp3() {
        ComplexData data = new MapComplexData(0);

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleSingleSynonymResp2() {
        // RESP2: ["term1", "synonym1"] (single synonym, not in array)
        ComplexData data = new ArrayComplexData(2);
        data.store("term1");
        data.store("synonym1");

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).hasSize(1);
        assertThat(result.get("term1")).containsExactly("synonym1");
    }

    @Test
    void shouldHandleSingleSynonymResp3() {
        // RESP3: {"term1": "synonym1"} (single synonym, not in array)
        ComplexData data = new MapComplexData(1);
        data.store("term1");
        data.store("synonym1");

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).hasSize(1);
        assertThat(result.get("term1")).containsExactly("synonym1");
    }

    @Test
    void shouldThrowExceptionForNullData() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed while parsing FT.SYNDUMP: data must not be null");
    }

    @Test
    void shouldHandleStringValues() {
        // Test with already decoded string values (e.g., in test scenarios)
        ComplexData data = new ArrayComplexData(2);
        data.storeObject("term1");
        data.storeObject("synonym1");

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).hasSize(1);
        assertThat(result.get("term1")).containsExactly("synonym1");
    }

}
