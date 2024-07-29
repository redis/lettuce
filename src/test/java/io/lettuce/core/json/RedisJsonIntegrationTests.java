/*
 * Copyright 2024, Redis Ltd. and Contributors
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

package io.lettuce.core.json;

import io.lettuce.core.RedisContainerIntegrationTests;
import io.lettuce.core.json.arguments.JsonSetArgs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisJsonIntegrationTests extends RedisContainerIntegrationTests {

    @Test
    void arrayAppendAndPop() throws ExecutionException, InterruptedException {
        JsonParser<String, String> parser = redis.getStatefulConnection().getJsonParser();

        String jsonType = redis.jsonType("bikes:inventory", JsonPath.of("$..commuter_bikes")).get().get(0);
        assertThat(jsonType).isEqualTo("array");

        JsonValue<String> element = parser.createJsonValue("\"{id:bike6}\"");
        List<Long> appendedElements = redis.jsonArrappend("bikes:inventory", JsonPath.of("$..mountain_bikes"), element).get();
        assertThat(appendedElements).hasSize(1);
        assertThat(appendedElements.get(0)).isEqualTo(6);

        List<JsonValue<String>> poppedJson = redis.jsonArrpop("bikes:inventory", JsonPath.of("$..mountain_bikes"), -1).get();
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).toValue()).isEqualTo("\"{id:bike6}\"");

    }

    @Test
    void exampleArrayProcessing() throws ExecutionException, InterruptedException {
        List<JsonValue<String>> poppedJson = redis.jsonArrpop("bikes:inventory", JsonPath.of("$..mountain_bikes"), -1).get();
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).toValue()).isEqualTo("\"{id:bike6}\"");
    }

    @Test
    void exampleObjectProcessing() throws ExecutionException, InterruptedException {
        JsonParser<String, String> parser = redis.getStatefulConnection().getJsonParser();
        JsonObject<String, String> bikeRecord = parser.createJsonObject();
        JsonObject<String, String> bikeSpecs = parser.createJsonObject();
        JsonArray<String> bikeColors = parser.createJsonArray();

        bikeSpecs.add("material", parser.createJsonValue("composite"));
        bikeSpecs.add("weight", parser.createJsonValue("11"));

        bikeColors.add(parser.createJsonValue("yellow"));
        bikeColors.add(parser.createJsonValue("orange"));

        bikeRecord.add("id", parser.createJsonValue("bike:43"));
        bikeRecord.add("model", parser.createJsonValue("DesertFox"));
        bikeRecord.add("description", parser.createJsonValue("The DesertFox is a versatile bike for all terrains"));
        bikeRecord.add("price", parser.createJsonValue("1299"));
        bikeRecord.add("specs", bikeSpecs);
        bikeRecord.add("colors", bikeColors);

        JsonSetArgs args = JsonSetArgs.Builder.none();

        Boolean result = redis.jsonSet("bikes:inventory", JsonPath.of("$..xc_bikes"), bikeRecord, args).get();
        assertThat(result).isTrue();
    }

}
