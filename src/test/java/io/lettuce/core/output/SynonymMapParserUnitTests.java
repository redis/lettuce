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

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.search.SynonymMapParser;

/**
 * Unit tests for {@link SynonymMapParser}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class SynonymMapParserUnitTests {

    private final StringCodec codec = StringCodec.UTF8;

    private final SynonymMapParser<String, String> parser = new SynonymMapParser<>(codec);

    @Test
    void shouldParseResp2Format() {

        // RESP2: ["term1", ["synonym1", "synonym2"], "term2", ["synonym3"]]
        ComplexData data = new ArrayComplexData(4);

        // Add term1
        data.storeObject(StringCodec.UTF8.encodeKey("term1"));

        // Add synonyms for term1
        ComplexData synonyms1 = new ArrayComplexData(2);
        synonyms1.storeObject(StringCodec.UTF8.encodeKey("synonym1"));
        synonyms1.storeObject(StringCodec.UTF8.encodeKey("synonym2"));
        data.storeObject(synonyms1);

        // Add term2
        data.storeObject(StringCodec.UTF8.encodeKey("term2"));

        // Add synonyms for term2
        ComplexData synonyms2 = new ArrayComplexData(1);
        synonyms2.storeObject(StringCodec.UTF8.encodeKey("synonym3"));
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
        data.storeObject(StringCodec.UTF8.encodeKey("term1"));
        ComplexData synonyms1 = new ArrayComplexData(2);
        synonyms1.storeObject(StringCodec.UTF8.encodeKey("synonym1"));
        synonyms1.storeObject(StringCodec.UTF8.encodeKey("synonym2"));
        data.storeObject(synonyms1);

        // Add term2 and its synonyms
        data.storeObject(StringCodec.UTF8.encodeKey("term2"));
        ComplexData synonyms2 = new ArrayComplexData(1);
        synonyms2.storeObject(StringCodec.UTF8.encodeKey("synonym3"));
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
        ComplexData data = new ArrayComplexData(1);
        data.storeObject(StringCodec.UTF8.encodeKey("term1"));
        ComplexData synonymData = new ArrayComplexData(1);
        synonymData.storeObject(StringCodec.UTF8.encodeKey("synonym1"));
        data.storeObject(synonymData);

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).hasSize(1);
        assertThat(result.get("term1")).containsExactly("synonym1");
    }

    @Test
    void shouldHandleSingleSynonymResp3() {
        // RESP3: {"term1": "synonym1"} (single synonym, not in array)
        ComplexData data = new MapComplexData(1);
        data.storeObject(StringCodec.UTF8.encodeKey("term1"));
        ComplexData synonymData = new ArrayComplexData(1);
        synonymData.storeObject(StringCodec.UTF8.encodeKey("synonym1"));
        data.storeObject(synonymData);

        Map<String, List<String>> result = parser.parse(data);

        assertThat(result).hasSize(1);
        assertThat(result.get("term1")).containsExactly("synonym1");
    }

    @Test
    void shouldThrowExceptionForNullData() {
        Map<String, List<String>> result = parser.parse(null);
        assertThat(result).isEmpty();

    }

}
