/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisContainerIntegrationTests;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.lettuce.core.json.arguments.JsonSetArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisJsonIntegrationTests extends RedisContainerIntegrationTests {

    private static final String BIKES_INVENTORY = "bikes:inventory";

    private static final String BIKE_COLORS_V1 = "..mountain_bikes[1].colors";

    private static final String BIKE_COLORS_V2 = "$..mountain_bikes[1].colors";

    private static final String MOUNTAIN_BIKES_V1 = "..mountain_bikes";

    private static final String MOUNTAIN_BIKES_V2 = "$..mountain_bikes";

    protected static RedisClient client;

    protected static RedisCommands<String, String> redis;

    public RedisJsonIntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        client = RedisClient.create(redisURI);
        redis = client.connect().sync();
    }

    @BeforeEach
    public void prepare() throws IOException {
        redis.flushall();

        Path path = Paths.get("src/test/resources/bike-inventory.json");
        String read = String.join("", Files.readAllLines(path));
        JsonValue<String> value = redis.getJsonParser().createJsonValue(read);

        redis.jsonSet("bikes:inventory", JsonPath.ROOT_PATH, value, JsonSetArgs.Builder.none());
    }

    @AfterAll
    static void teardown() {
        if (client != null) {
            client.shutdown();
        }
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonArrappend(String path) {
        JsonParser<String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);

        JsonValue<String> element = parser.createJsonValue("\"{id:bike6}\"");
        List<Long> appendedElements = redis.jsonArrappend(BIKES_INVENTORY, myPath, element);
        assertThat(appendedElements).hasSize(1);
        assertThat(appendedElements.get(0)).isEqualTo(4);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { BIKE_COLORS_V1, BIKE_COLORS_V2 })
    void jsonArrindex(String path) {
        JsonParser<String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);
        JsonValue<String> element = parser.createJsonValue("\"white\"");

        List<Long> arrayIndex = redis.jsonArrindex(BIKES_INVENTORY, myPath, element, null);
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex).hasSize(1);
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(1L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { BIKE_COLORS_V1, BIKE_COLORS_V2 })
    void jsonArrinsert(String path) {
        JsonParser<String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);
        JsonValue<String> element = parser.createJsonValue("\"ultramarine\"");

        List<Long> arrayIndex = redis.jsonArrinsert(BIKES_INVENTORY, myPath, 1, element);
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex).hasSize(1);
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(3L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonArrlen(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<Long> poppedJson = redis.jsonArrlen(BIKES_INVENTORY, myPath);
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).longValue()).isEqualTo(3);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonArrpop(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<JsonValue<String>> poppedJson = redis.jsonArrpop(BIKES_INVENTORY, myPath, -1);
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).toValue()).contains(
                "{\"id\":\"bike:3\",\"model\":\"Weywot\",\"description\":\"This bike gives kids aged six years and old");
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { BIKE_COLORS_V1, BIKE_COLORS_V2 })
    void jsonArrtrim(String path) {
        JsonPath myPath = JsonPath.of(path);
        JsonRangeArgs range = JsonRangeArgs.Builder.start(1).stop(2);

        List<Long> arrayIndex = redis.jsonArrtrim(BIKES_INVENTORY, myPath, range);
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex).hasSize(1);
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(1L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { BIKE_COLORS_V1, BIKE_COLORS_V2 })
    void jsonClear(String path) {
        JsonPath myPath = JsonPath.of(path);

        Long result = redis.jsonClear(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(1L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "..mountain_bikes[0:2].model", "$..mountain_bikes[0:2].model" })
    void jsonGet(String path) {
        JsonPath myPath = JsonPath.of(path);

        // Verify codec parsing
        List<JsonValue<String>> value = redis.jsonGet(BIKES_INVENTORY, JsonGetArgs.Builder.none(), myPath);
        assertThat(value).hasSize(1);

        if (path.startsWith("$")) {
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
            assertThat(value.get(0).asJsonArray().asList().get(1).isNull()).isFalse();
            assertThat(value.get(0).asJsonArray().asList().get(1).asString()).isEqualTo("Quaoar");
        } else {
            assertThat(value.get(0).toValue()).isEqualTo("\"Phoebe\"");

            // Verify array parsing
            assertThat(value.get(0).isString()).isTrue();
            assertThat(value.get(0).asString()).isEqualTo("Phoebe");
        }
    }

    @Test
    void jsonGetNull() {
        JsonPath myPath = JsonPath.of("$..inventory.owner");

        // Verify codec parsing
        List<JsonValue<String>> value = redis.jsonGet(BIKES_INVENTORY, JsonGetArgs.Builder.none(), myPath);
        assertThat(value).hasSize(1);

        assertThat(value.get(0).toValue()).isEqualTo("[null]");

        // Verify array parsing
        assertThat(value.get(0).isJsonArray()).isTrue();
        assertThat(value.get(0).asJsonArray().size()).isEqualTo(1);
        assertThat(value.get(0).asJsonArray().asList().get(0).toValue()).isEqualTo("null");
        assertThat(value.get(0).asJsonArray().asList().get(0).isNull()).isTrue();
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1 + "[1]", MOUNTAIN_BIKES_V2 + "[1]" })
    void jsonMerge(String path) {
        JsonParser<String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);
        JsonValue<String> element = parser.createJsonValue("\"ultramarine\"");

        String result = redis.jsonMerge(BIKES_INVENTORY, myPath, element);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("OK");
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "..model", "$..model" })
    void jsonMGet(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<JsonValue<String>> value = redis.jsonMGet(myPath, BIKES_INVENTORY);
        assertThat(value).hasSize(1);
        if (path.startsWith("$")) {
            assertThat(value.get(0).toValue()).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\"]");
        } else {
            assertThat(value.get(0).toValue()).isEqualTo("\"Phoebe\"");
        }
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1 + "[1]", MOUNTAIN_BIKES_V2 + "[1]" })
    void jsonMset(String path) {
        JsonParser<String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);

        JsonObject<String> bikeRecord = parser.createEmptyJsonObject();
        JsonObject<String> bikeSpecs = parser.createEmptyJsonObject();
        JsonArray<String> bikeColors = parser.createEmptyJsonArray();
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

        JsonMsetArgs<String, String> args1 = new JsonMsetArgs<>(BIKES_INVENTORY, myPath, bikeRecord);

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

        JsonMsetArgs<String, String> args2 = new JsonMsetArgs<>(BIKES_INVENTORY, myPath, bikeRecord);

        List<JsonMsetArgs<String, String>> args = Arrays.asList(args1, args2);
        String result = redis.jsonMSet(args);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("OK");

        JsonValue<String> value = redis.jsonGet(BIKES_INVENTORY, JsonGetArgs.Builder.none(), JsonPath.ROOT_PATH).get(0);
        assertThat(value).isNotNull();
        assertThat(value.isJsonArray()).isTrue();
        assertThat(value.asJsonArray().size()).isEqualTo(1);
        assertThat(value.asJsonArray().asList().get(0).toValue()).contains(
                "{\"id\":\"bike:13\",\"model\":\"Woody\",\"description\":\"The Woody is an environmentally-friendly wooden bike\"");
    }

    @Test
    void jsonMsetCrossslot() {
        JsonParser<String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(BIKES_INVENTORY);

        JsonObject<String> bikeRecord = parser.createEmptyJsonObject();
        JsonObject<String> bikeSpecs = parser.createEmptyJsonObject();
        JsonArray<String> bikeColors = parser.createEmptyJsonArray();
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

        JsonMsetArgs<String, String> args1 = new JsonMsetArgs<>(BIKES_INVENTORY, myPath, bikeRecord);

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

        JsonMsetArgs<String, String> args2 = new JsonMsetArgs<>("bikes:service", JsonPath.ROOT_PATH, bikeRecord);

        List<JsonMsetArgs<String, String>> args = Arrays.asList(args1, args2);
        String result = redis.jsonMSet(args);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("OK");

        JsonValue<String> value = redis.jsonGet("bikes:service", JsonGetArgs.Builder.none(), JsonPath.ROOT_PATH).get(0);
        assertThat(value).isNotNull();
        assertThat(value.isJsonArray()).isTrue();
        assertThat(value.asJsonArray().size()).isEqualTo(1);
        assertThat(value.asJsonArray().asList().get(0).toValue()).contains(
                "{\"id\":\"bike:13\",\"model\":\"Woody\",\"description\":\"The Woody is an environmentally-friendly wooden bike\"");
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "$..mountain_bikes[0:1].price", "..mountain_bikes[0:1].price" })
    void jsonNumincrby(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<Number> value = redis.jsonNumincrby(BIKES_INVENTORY, myPath, 5L);
        assertThat(value).hasSize(1);
        assertThat(value.get(0).longValue()).isEqualTo(1933L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1 + "[1]", MOUNTAIN_BIKES_V2 + "[1]" })
    void jsonObjkeys(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<String> result = redis.jsonObjkeys(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(6);
        assertThat(result).contains("id", "model", "description", "price", "specs", "colors");
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1 + "[1]", MOUNTAIN_BIKES_V2 + "[1]" })
    void jsonObjlen(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<Long> result = redis.jsonObjlen(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(6L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonSet(String path) {
        JsonPath myPath = JsonPath.of(path);

        JsonParser<String> parser = redis.getJsonParser();
        JsonObject<String> bikeRecord = parser.createEmptyJsonObject();
        JsonObject<String> bikeSpecs = parser.createEmptyJsonObject();
        JsonArray<String> bikeColors = parser.createEmptyJsonArray();

        bikeSpecs.put("material", parser.createJsonValue("null"));
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

        String result = redis.jsonSet(BIKES_INVENTORY, myPath, bikeRecord, args);
        assertThat(result).isEqualTo("OK");
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "..mountain_bikes[1].colors[1]", "$..mountain_bikes[1].colors[1]" })
    void jsonStrappend(String path) {
        JsonParser<String> parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);
        JsonValue<String> element = parser.createJsonValue("\"-light\"");

        List<Long> result = redis.jsonStrappend(BIKES_INVENTORY, myPath, element);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(11L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { BIKE_COLORS_V1 + "[1]", BIKE_COLORS_V2 + "[1]" })
    void jsonStrlen(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<Long> result = redis.jsonStrlen(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(5L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "$..complete", "..complete" })
    void jsonToggle(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<Long> result = redis.jsonToggle(BIKES_INVENTORY, myPath);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        if (path.startsWith("$")) {
            assertThat(result.get(0)).isEqualTo(1L);
        } else {
            // seems that for JSON.TOGGLE when we use a V1 path the resulting value is a list of string values and not a
            // list of integer values as per the documentation
            assertThat(result).isNotEmpty();
        }
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1 + "[2:3]", MOUNTAIN_BIKES_V2 + "[2:3]" })
    void jsonDel(String path) {
        JsonPath myPath = JsonPath.of(path);

        Long value = redis.jsonDel(BIKES_INVENTORY, myPath);
        assertThat(value).isEqualTo(1);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonType(String path) {
        JsonPath myPath = JsonPath.of(path);

        JsonType jsonType = redis.jsonType(BIKES_INVENTORY, myPath).get(0);
        assertThat(jsonType).isEqualTo(JsonType.ARRAY);
    }

    @Test
    void jsonAllTypes() {
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1]");

        JsonType jsonType = redis.jsonType(BIKES_INVENTORY, myPath).get(0);
        assertThat(jsonType).isEqualTo(JsonType.OBJECT);

        myPath = JsonPath.of("$..mountain_bikes[0:1].price");
        jsonType = redis.jsonType(BIKES_INVENTORY, myPath).get(0);
        assertThat(jsonType).isEqualTo(JsonType.INTEGER);

        myPath = JsonPath.of("$..weight");
        jsonType = redis.jsonType(BIKES_INVENTORY, myPath).get(0);
        assertThat(jsonType).isEqualTo(JsonType.NUMBER);

        myPath = JsonPath.of("$..complete");
        jsonType = redis.jsonType(BIKES_INVENTORY, myPath).get(0);
        assertThat(jsonType).isEqualTo(JsonType.BOOLEAN);

        myPath = JsonPath.of("$..inventory.owner");
        jsonType = redis.jsonType(BIKES_INVENTORY, myPath).get(0);
        assertThat(jsonType).isEqualTo(JsonType.UNKNOWN);
    }

}
