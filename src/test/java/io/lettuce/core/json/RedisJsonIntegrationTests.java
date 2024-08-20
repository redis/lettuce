/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.RedisContainerIntegrationTests;
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.json.arguments.JsonSetArgs;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisJsonIntegrationTests extends RedisContainerIntegrationTests {

    private static final JsonPath MOUNTAIN_BIKES_PATH = JsonPath.of("$..mountain_bikes");

    private static final String BIKES_INVENTORY = "bikes:inventory";

    @Test
    void jsonArrappend() {
        JsonParser<String, String> parser = redis.getJsonParser();

        JsonValue<String, String> element = parser.createJsonValue("\"{id:bike6}\"");
        List<Long> appendedElements = redis.jsonArrappend(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, element);
        assertThat(appendedElements).hasSize(1);
        assertThat(appendedElements.get(0)).isEqualTo(4);
    }

    @Test
    void jsonArrindex() {
        JsonParser<String, String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1].colors");
        JsonValue<String, String> element = parser.createJsonValue("\"white\"");

        List<Long> arrayIndex = redis.jsonArrindex(BIKES_INVENTORY, myPath, element, null);
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex).hasSize(1);
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(1L);
    }

    @Test
    void jsonArrinsert() {
        JsonParser<String, String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1].colors");
        JsonValue<String, String> element = parser.createJsonValue("\"ultramarine\"");

        List<Long> arrayIndex = redis.jsonArrinsert(BIKES_INVENTORY, myPath, 1, element);
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex).hasSize(1);
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(3L);
    }

    @Test
    void jsonArrlen() {
        List<Long> poppedJson = redis.jsonArrlen(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH);
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).longValue()).isEqualTo(3);
    }

    @Test
    void jsonArrpop() {
        List<JsonValue<String, String>> poppedJson = redis.jsonArrpop(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH, -1);
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).toValue()).contains(
                "{\"id\":\"bike:3\",\"model\":\"Weywot\",\"description\":\"This bike gives kids aged six years and old");
    }

    @Test
    void jsonArrtrim() {
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1].colors");
        JsonRangeArgs range = JsonRangeArgs.Builder.start(1).stop(2);

        List<Long> arrayIndex = redis.jsonArrtrim(BIKES_INVENTORY, myPath, range);
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex).hasSize(1);
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(1L);
    }

    @Test
    void jsonClear() {
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1].colors");

        Long result = redis.jsonClear(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(1L);
    }

    @Test
    void jsonGet() {
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
    void jsonMerge() {
        JsonParser<String, String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1]");
        JsonValue<String, String> element = parser.createJsonValue("\"ultramarine\"");

        String result = redis.jsonMerge(BIKES_INVENTORY, myPath, element);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void jsonMGet() {
        JsonPath path = JsonPath.of("$..model");

        List<JsonValue<String, String>> value = redis.jsonMGet(path, BIKES_INVENTORY);
        assertThat(value).hasSize(1);
        assertThat(value.get(0).toValue()).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\"]");
    }

    @Test
    void jsonMset() {
        JsonParser<String, String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1]");

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

        JsonMsetArgs args1 = JsonMsetArgs.Builder.key(BIKES_INVENTORY).path(myPath).element(bikeRecord);

        bikeRecord = parser.createEmptyJsonObject();
        bikeSpecs = parser.createEmptyJsonObject();
        bikeColors = parser.createEmptyJsonArray();
        bikeSpecs.put("material", parser.createJsonValue("\"wood\""));
        bikeSpecs.put("weight", parser.createJsonValue("19"));
        bikeColors.add(parser.createJsonValue("\"walnut\""));
        bikeColors.add(parser.createJsonValue("\"chestnut\""));
        bikeRecord.put("id", parser.createJsonValue("\"bike:13\""));
        bikeRecord.put("model", parser.createJsonValue("\"Woody\""));
        bikeRecord.put("description", parser.createJsonValue("\"The Woody is an environmentally-friendly wooden bike\""));
        bikeRecord.put("price", parser.createJsonValue("\"1112\""));
        bikeRecord.put("specs", bikeSpecs);
        bikeRecord.put("colors", bikeColors);

        JsonMsetArgs args2 = JsonMsetArgs.Builder.key(BIKES_INVENTORY).path(myPath).element(bikeRecord);

        String result = redis.jsonMSet(args1, args2);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void jsonNumincrby() {
        JsonPath path = JsonPath.of("$..mountain_bikes[0:1].price");

        List<Number> value = redis.jsonNumincrby(BIKES_INVENTORY, path, 5L);
        assertThat(value).hasSize(1);
        assertThat(value.get(0).longValue()).isEqualTo(1933L);
    }

    @Test
    void jsonObjkeys() {
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1]");

        List<String> result = redis.jsonObjkeys(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(6);
        assertThat(result).contains("id", "model", "description", "price", "specs", "colors");
    }

    @Test
    void jsonObjlen() {
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1]");

        List<Long> result = redis.jsonObjlen(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(6L);
    }

    @Test
    void jsonSet() {
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
    void jsonStrappend() {
        JsonParser<String, String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1].colors[1]");
        JsonValue<String, String> element = parser.createJsonValue("\"-light\"");

        List<Long> result = redis.jsonStrappend(BIKES_INVENTORY, myPath, element);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(11L);
    }

    @Test
    void jsonStrlen() {
        JsonParser<String, String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1].colors[1]");

        List<Long> result = redis.jsonStrlen(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(5L);
    }

    @Test
    void jsonToggle() {
        JsonParser<String, String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of("$..complete");

        List<Long> result = redis.jsonToggle(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(1L);
    }

    @Test
    void jsonDel() {
        JsonPath path = JsonPath.of("$..mountain_bikes[2:3]");

        Long value = redis.jsonDel(BIKES_INVENTORY, path);
        assertThat(value).isEqualTo(1);
    }

    @Test
    void jsonType() {
        String jsonType = redis.jsonType(BIKES_INVENTORY, MOUNTAIN_BIKES_PATH).get(0);
        assertThat(jsonType).isEqualTo("array");
    }

}
