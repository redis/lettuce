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
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.json.arguments.JsonSetArgs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisJsonIntegrationTests extends RedisContainerIntegrationTests {

    private static final JsonPath MOUNTAIN_BIKES_PATH = JsonPath.of("$..mountain_bikes");

    private static final String BIKES_INVENTORY = "bikes:inventory";

    private static final String COMMUTER_BIKES = "$..commuter_bikes";

    @Test
    void jsonArrappend() throws ExecutionException, InterruptedException {
        JsonParser<String, String> parser = redis.getStatefulConnection().getJsonParser();

        JsonValue<String, String> element = parser.createJsonValue("\"{id:bike6}\"");
        List<Long> appendedElements = redis.jsonArrappend(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, element).get();
        assertThat(appendedElements).hasSize(1);
        assertThat(appendedElements.get(0)).isEqualTo(5);

        // Cleanup

        List<JsonValue<String, String>> poppedJson = redis.jsonArrpop(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, -1).get();
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).toValue()).isEqualTo("\"{id:bike6}\"");
    }

    @Test
    void jsonArrindex() throws ExecutionException, InterruptedException {
        JsonRangeArgs range = JsonRangeArgs.Builder.start(1).stop(4);
        JsonParser<String, String> parser = redis.getStatefulConnection().getJsonParser();
        JsonValue<String, String> element = parser.createJsonValue("\"{id:bike3}\"");

        List<Long> arrayIndex = redis.jsonArrindex(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, element, range).get();
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(3L);
    }

    @Test
    void jsonArrinsert() {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonArrlen() throws ExecutionException, InterruptedException {
        List<Long> poppedJson = redis.jsonArrlen(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH).get();
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).longValue()).isEqualTo(5);
    }

    @Test
    void jsonArrpop() throws ExecutionException, InterruptedException {
        List<JsonValue<String, String>> poppedJson = redis.jsonArrpop(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, -1).get();
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).toValue()).isEqualTo("\"{id:bike6}\"");

        throw new RuntimeException("Missing cleanup");
    }

    @Test
    void jsonArrtrim() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonClear() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonGet() throws ExecutionException, InterruptedException {
        JsonPath path = JsonPath.of("$..mountain_bikes[0:2].model");

        // Verify codec parsing
        List<JsonValue<String, String>> value = redis.jsonGet(BIKES_INVENTORY, JsonGetArgs.Builder.none(), path).get();
        assertThat(value).hasSize(1);
        assertThat(value.get(0).toValue()).isEqualTo("[\"Phoebe\",\"Quaoar\"]");

        // Verify array parsing
        assertThat(value.get(0).isJsonArray()).isTrue();
        assertThat(value.get(0).asJsonArray().size()).isEqualTo(2);
        assertThat(value.get(0).asJsonArray().asList().get(0).toValue()).isEqualTo("\"Phoebe\"");
        assertThat(value.get(0).asJsonArray().asList().get(1).toValue()).isEqualTo("\"Quaoar\"");

        // Verify String parsing
        assertThat(value.get(0).asJsonArray().asList().get(0).isString()).isTrue();
        assertThat(value.get(0).asJsonArray().asList().get(0).asString()).isEqualTo("Phoebe");
        assertThat(value.get(0).asJsonArray().asList().get(1).isString()).isTrue();
        assertThat(value.get(0).asJsonArray().asList().get(1).asString()).isEqualTo("Quaoar");
    }

    @Test
    void jsonMerge() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonMGet() throws ExecutionException, InterruptedException {
        JsonPath path = JsonPath.of("$..model");

        List<JsonValue<String, String>> value = redis.jsonMGet(path, BIKES_INVENTORY).get();
        assertThat(value).hasSize(1);
        assertThat(value.get(0).toValue()).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\",\"Salacia\",\"Mimas\"]");
    }

    @Test
    void jsonMset() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonNumincrby() throws ExecutionException, InterruptedException {
        JsonPath path = JsonPath.of("$..mountain_bikes[0:1].price");

        List<Number> value = redis.jsonNumincrby(BIKES_INVENTORY, path, 5L).get();
        assertThat(value).hasSize(1);
        assertThat(value.get(0).longValue()).isEqualTo(1930L);

        value = redis.jsonNumincrby(BIKES_INVENTORY, path, -5L).get();
        assertThat(value).hasSize(1);
        assertThat(value.get(0).longValue()).isEqualTo(1925L);
    }

    @Test
    void jsonObjkeys() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonObjlen() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonSet() throws ExecutionException, InterruptedException {
        JsonParser<String, String> parser = redis.getStatefulConnection().getJsonParser();
        JsonObject<String, String> bikeRecord = parser.createEmptyJsonObject();
        JsonObject<String, String> bikeSpecs = parser.createEmptyJsonObject();
        JsonArray<String, String> bikeColors = parser.createEmptyJsonArray();

        bikeSpecs.put("material", parser.createJsonValue("\"composite\""));
        bikeSpecs.put("weight", parser.createJsonValue("11"));

        bikeColors.add(parser.createJsonValue("\"yellow\""));
        bikeColors.add(parser.createJsonValue("\"orange\""));

        bikeRecord.put("id", parser.createJsonValue("\"bike:43\""));
        bikeRecord.put("model", parser.createJsonValue("\"DesertFox\""));
        bikeRecord.put("description", parser.createJsonValue("\"The DesertFox is a versatile bike for all terrains\""));
        bikeRecord.put("price", parser.createJsonValue("\"1299\""));
        bikeRecord.put("specs", bikeSpecs);
        bikeRecord.put("colors", bikeColors);

        JsonSetArgs args = JsonSetArgs.Builder.none();

        String result = redis.jsonSet(BIKES_INVENTORY, JsonPath.of(COMMUTER_BIKES), bikeRecord, args).get();
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void jsonStrappend() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonStrlen() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonToggle() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonDel() throws ExecutionException, InterruptedException {
        JsonPath path = JsonPath.of("$..mountain_bikes[3:4]");

        Long value = redis.jsonDel(BIKES_INVENTORY, path).get();
        assertThat(value).isEqualTo(1);
    }

    @Test
    void jsonType() throws ExecutionException, InterruptedException {
        String jsonType = redis.jsonType(BIKES_INVENTORY, JsonPath.of(COMMUTER_BIKES)).get().get(0);
        assertThat(jsonType).isEqualTo("array");
    }

}
