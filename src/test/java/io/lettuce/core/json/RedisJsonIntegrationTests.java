/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.RedisContainerIntegrationTests;
import io.lettuce.core.json.arguments.JsonGetArgs;
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
        JsonParser<String, String> parser = redis.getJsonParser();

        JsonValue<String, String> element = parser.createJsonValue("\"{id:bike6}\"");
        List<Long> appendedElements = redis.jsonArrappend(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, element);
        assertThat(appendedElements).hasSize(1);
        assertThat(appendedElements.get(0)).isEqualTo(4);
    }

    @Test
    void jsonArrindex() throws ExecutionException, InterruptedException {
        JsonParser<String, String> parser = redis.getJsonParser();
        JsonValue<String, String> element = parser.createJsonValue("\"id\": \"bike:2\"");

        List<Long> arrayIndex = redis.jsonArrindex(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, element, null);
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(3L);
    }

    @Test
    void jsonArrinsert() {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonArrlen() throws ExecutionException, InterruptedException {
        List<Long> poppedJson = redis.jsonArrlen(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH);
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).longValue()).isEqualTo(3);
    }

    @Test
    void jsonArrpop() throws ExecutionException, InterruptedException {
        List<JsonValue<String, String>> poppedJson = redis.jsonArrpop(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, -1);
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).toValue()).contains(
                "{\"id\":\"bike:3\",\"model\":\"Weywot\",\"description\":\"This bike gives kids aged six years and old");
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
        List<JsonValue<String, String>> value = redis.jsonGet(BIKES_INVENTORY, JsonGetArgs.Builder.none(), path);
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

        List<JsonValue<String, String>> value = redis.jsonMGet(path, BIKES_INVENTORY);
        assertThat(value).hasSize(1);
        assertThat(value.get(0).toValue()).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\"]");
    }

    @Test
    void jsonMset() throws ExecutionException, InterruptedException {
        throw new RuntimeException("Not implemented");
    }

    @Test
    void jsonNumincrby() throws ExecutionException, InterruptedException {
        JsonPath path = JsonPath.of("$..mountain_bikes[0:1].price");

        List<Number> value = redis.jsonNumincrby(BIKES_INVENTORY, path, 5L);
        assertThat(value).hasSize(1);
        assertThat(value.get(0).longValue()).isEqualTo(1933L);
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
        JsonParser<String, String> parser = redis.getJsonParser();
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

        String result = redis.jsonSet(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, bikeRecord, args);
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
        JsonPath path = JsonPath.of("$..mountain_bikes[2:3]");

        Long value = redis.jsonDel(BIKES_INVENTORY, path);
        assertThat(value).isEqualTo(1);
    }

    @Test
    void jsonType() throws ExecutionException, InterruptedException {
        String jsonType = redis.jsonType(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH).get(0);
        assertThat(jsonType).isEqualTo("array");
    }

}
