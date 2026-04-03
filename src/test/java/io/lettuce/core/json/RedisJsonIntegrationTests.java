/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.arguments.JsonGetArgs;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(INTEGRATION_TEST)
public class RedisJsonIntegrationTests {

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
        JsonValue value = redis.getJsonParser().createJsonValue(read);

        redis.jsonSet("bikes:inventory", JsonPath.ROOT_PATH, value);
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
        JsonParser parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);

        JsonValue element = parser.createJsonValue("\"{id:bike6}\"");
        List<Long> appendedElements = redis.jsonArrappend(BIKES_INVENTORY, myPath, element);
        assertThat(appendedElements).hasSize(1);
        assertThat(appendedElements.get(0)).isEqualTo(4);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonArrappendStringOverload(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<Long> appendedElements = redis.jsonArrappend(BIKES_INVENTORY, myPath, "\"{id:bike7}\"");
        assertThat(appendedElements).hasSize(1);
        assertThat(appendedElements.get(0)).isEqualTo(4);

        // Verify appended content
        List<JsonValue> value = redis.jsonGet(BIKES_INVENTORY, myPath);
        assertThat(value).hasSize(1);
        if (path.startsWith("$")) {
            JsonArray matches = value.get(0).asJsonArray();
            assertThat(matches.size()).isEqualTo(1);
            JsonArray arr = matches.get(0).asJsonArray();
            String last = arr.get(arr.size() - 1).toString();
            assertThat(last).isEqualTo("\"{id:bike7}\"");
        } else {
            JsonArray arr = value.get(0).asJsonArray();
            String last = arr.get(arr.size() - 1).toString();
            assertThat(last).isEqualTo("\"{id:bike7}\"");
        }
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { BIKE_COLORS_V1, BIKE_COLORS_V2 })
    void jsonArrindex(String path) {
        JsonParser parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);
        JsonValue element = parser.createJsonValue("\"white\"");

        List<Long> arrayIndex = redis.jsonArrindex(BIKES_INVENTORY, myPath, element);
        assertThat(arrayIndex).isNotNull();
        assertThat(arrayIndex).hasSize(1);
        assertThat(arrayIndex.get(0).longValue()).isEqualTo(1L);
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { BIKE_COLORS_V1, BIKE_COLORS_V2 })
    void jsonArrinsert(String path) {
        JsonParser parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);
        JsonValue element = parser.createJsonValue("\"ultramarine\"");

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

    @Test
    void jsonArrLenAsyncAndReactive() throws ExecutionException, InterruptedException {
        RedisAsyncCommands<String, String> asyncCommands = client.connect().async();
        RedisReactiveCommands<String, String> reactiveCommands = client.connect().reactive();

        JsonPath myPath = JsonPath.of(MOUNTAIN_BIKES_V1);

        List<Long> poppedJson = asyncCommands.jsonArrlen(BIKES_INVENTORY, myPath).get();
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).longValue()).isEqualTo(3);

        StepVerifier.create(reactiveCommands.jsonArrlen(BIKES_INVENTORY, myPath)).consumeNextWith(actual -> {
            assertThat(actual).isEqualTo(3);
        }).verifyComplete();
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonArrpop(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<JsonValue> poppedJson = redis.jsonArrpop(BIKES_INVENTORY, myPath);
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0).toString()).contains(
                "{\"id\":\"bike:3\",\"model\":\"Weywot\",\"description\":\"This bike gives kids aged six years and old");
    }

    @Test
    public void jsonArrpopEmptyArray() {
        JsonValue value = redis.getJsonParser().createJsonValue("[\"one\"]");
        redis.jsonSet("myKey", JsonPath.ROOT_PATH, value);
        List<JsonValue> result = redis.jsonArrpop("myKey");
        assertThat(result.toString()).isEqualTo("[\"one\"]");
        assertThat(redis.jsonGet("myKey").get(0).toString()).isEqualTo("[]");
    }

    @Test
    public void jsonArrpopWithRootPathAndIndex() {
        JsonValue value = redis.getJsonParser().createJsonValue("[\"one\",\"two\",\"three\"]");
        redis.jsonSet("myKey", JsonPath.ROOT_PATH, value);
        List<JsonValue> result = redis.jsonArrpop("myKey", JsonPath.ROOT_PATH, 1);
        assertThat(result.toString()).isEqualTo("[\"two\"]");
        assertThat(redis.jsonGet("myKey").get(0).toString()).isEqualTo("[\"one\",\"three\"]");
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
        List<JsonValue> value = redis.jsonGet(BIKES_INVENTORY, myPath);
        assertThat(value).hasSize(1);

        if (path.startsWith("$")) {
            assertThat(value.get(0).toString()).isEqualTo("[\"Phoebe\",\"Quaoar\"]");

            // Verify array parsing
            assertThat(value.get(0).isJsonArray()).isTrue();
            assertThat(value.get(0).asJsonArray().size()).isEqualTo(2);
            assertThat(value.get(0).asJsonArray().asList().get(0).toString()).isEqualTo("\"Phoebe\"");
            assertThat(value.get(0).asJsonArray().asList().get(1).toString()).isEqualTo("\"Quaoar\"");

            // Verify String parsing
            assertThat(value.get(0).asJsonArray().asList().get(0).isString()).isTrue();
            assertThat(value.get(0).asJsonArray().asList().get(0).asString()).isEqualTo("Phoebe");
            assertThat(value.get(0).asJsonArray().asList().get(1).isString()).isTrue();
            assertThat(value.get(0).asJsonArray().asList().get(1).isNull()).isFalse();
            assertThat(value.get(0).asJsonArray().asList().get(1).asString()).isEqualTo("Quaoar");
        } else {
            assertThat(value.get(0).toString()).isEqualTo("\"Phoebe\"");

            // Verify array parsing
            assertThat(value.get(0).isString()).isTrue();
            assertThat(value.get(0).asString()).isEqualTo("Phoebe");
        }
    }

    @Test
    void jsonGetNull() {
        JsonPath myPath = JsonPath.of("$..inventory.owner");

        // Verify codec parsing
        List<JsonValue> value = redis.jsonGet(BIKES_INVENTORY, myPath);
        assertThat(value).hasSize(1);

        assertThat(value.get(0).toString()).isEqualTo("[null]");

        // Verify array parsing
        assertThat(value.get(0).isJsonArray()).isTrue();
        assertThat(value.get(0).asJsonArray().size()).isEqualTo(1);
        assertThat(value.get(0).asJsonArray().asList().get(0).toString()).isEqualTo("null");
        assertThat(value.get(0).asJsonArray().asList().get(0).isNull()).isTrue();
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1 + "[1]", MOUNTAIN_BIKES_V2 + "[1]" })
    void jsonMerge(String path) {
        JsonParser parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);
        JsonValue element = parser.createJsonValue("\"ultramarine\"");

        String result = redis.jsonMerge(BIKES_INVENTORY, myPath, element);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("OK");
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "..model", "$..model" })
    void jsonMGet(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<JsonValue> value = redis.jsonMGet(myPath, BIKES_INVENTORY);
        assertThat(value).hasSize(1);
        if (path.startsWith("$")) {
            assertThat(value.get(0).toString()).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\"]");
        } else {
            assertThat(value.get(0).toString()).isEqualTo("\"Phoebe\"");
        }
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1 + "[1]", MOUNTAIN_BIKES_V2 + "[1]" })
    void jsonMset(String path) {
        JsonParser parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);

        JsonObject bikeRecord = parser.createJsonObject();
        JsonObject bikeSpecs = parser.createJsonObject();
        JsonArray bikeColors = parser.createJsonArray();
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

        bikeRecord = parser.createJsonObject();
        bikeSpecs = parser.createJsonObject();
        bikeColors = parser.createJsonArray();
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

        JsonValue value = redis.jsonGet(BIKES_INVENTORY, JsonPath.ROOT_PATH).get(0);
        assertThat(value).isNotNull();
        assertThat(value.isJsonArray()).isTrue();
        assertThat(value.asJsonArray().size()).isEqualTo(1);
        assertThat(value.asJsonArray().asList().get(0).toString()).contains(
                "{\"id\":\"bike:13\",\"model\":\"Woody\",\"description\":\"The Woody is an environmentally-friendly wooden bike\"");
    }

    @Test
    void jsonMsetCrossslot() {
        JsonParser parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(BIKES_INVENTORY);

        JsonObject bikeRecord = parser.createJsonObject();
        JsonObject bikeSpecs = parser.createJsonObject();
        JsonArray bikeColors = parser.createJsonArray();
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

        bikeRecord = parser.createJsonObject();
        bikeSpecs = parser.createJsonObject();
        bikeColors = parser.createJsonArray();
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

        JsonValue value = redis.jsonGet("bikes:service", JsonPath.ROOT_PATH).get(0);
        assertThat(value).isNotNull();
        assertThat(value.isJsonArray()).isTrue();
        assertThat(value.asJsonArray().size()).isEqualTo(1);
        assertThat(value.asJsonArray().asList().get(0).toString()).contains(
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

        JsonParser parser = redis.getJsonParser();
        JsonObject bikeRecord = parser.createJsonObject();
        JsonObject bikeSpecs = parser.createJsonObject();
        JsonArray bikeColors = parser.createJsonArray();

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

        String result = redis.jsonSet(BIKES_INVENTORY, myPath, bikeRecord);
        assertThat(result).isEqualTo("OK");
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonSetStringAndGet(String path) {
        JsonPath myPath = JsonPath.of(path);

        // Set using String overload, then get and verify
        String payload = "{\"id\":\"bike:99\",\"model\":\"Stringy\"}";
        String res = redis.jsonSet(BIKES_INVENTORY, myPath, payload);
        assertThat(res).isEqualTo("OK");

        List<JsonValue> got = redis.jsonGet(BIKES_INVENTORY, myPath);
        assertThat(got).hasSize(1);
        // For $-prefixed paths, RedisJSON returns arrays of matches
        if (path.startsWith("$")) {
            assertThat(got.get(0).isJsonArray()).isTrue();
            assertThat(got.get(0).asJsonArray().size()).isEqualTo(1);
            assertThat(got.get(0).asJsonArray().get(0).isJsonObject()).isTrue();
            assertThat(got.get(0).asJsonArray().get(0).asJsonObject().get("id").asString()).isEqualTo("bike:99");
            assertThat(got.get(0).asJsonArray().get(0).asJsonObject().get("model").asString()).isEqualTo("Stringy");
        } else {
            assertThat(got.get(0).isJsonObject()).isTrue();
            assertThat(got.get(0).asJsonObject().get("id").asString()).isEqualTo("bike:99");
            assertThat(got.get(0).asJsonObject().get("model").asString()).isEqualTo("Stringy");
        }
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "..mountain_bikes[1].colors[1]", "$..mountain_bikes[1].colors[1]" })
    void jsonStrappend(String path) {
        JsonParser parser = redis.getJsonParser();
        JsonPath myPath = JsonPath.of(path);
        JsonValue element = parser.createJsonValue("\"-light\"");

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

    @Test
    void jsonGetToObject() {
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1]");
        JsonValue value = redis.jsonGet(BIKES_INVENTORY, myPath).get(0);
        assertThat(value).isNotNull();
        assertThat(value.isNull()).isFalse();
        assertThat(value.asJsonArray().get(0).isJsonObject()).isTrue();

        MountainBike bike = value.asJsonArray().get(0).asJsonObject().toObject(MountainBike.class);

        assertThat(bike).isNotNull();
        assertThat(bike).isInstanceOf(MountainBike.class);

        assertThat(bike.id).isEqualTo("bike:2");
        assertThat(bike.model).isEqualTo("Quaoar");
        assertThat(bike.description).contains("Redesigned for the 2020 model year, this bike impressed");
    }

    static class MountainBike {

        public String id;

        public String model;

        public String description;

        public String price;

        public Specs specs;

        public List colors;

    }

    static class Specs {

        public String material;

        public String weight;

    }

    @Test
    void jsonSetFromObject() {
        JsonPath myPath = JsonPath.of("$..mountain_bikes[1]");
        JsonValue value = redis.jsonGet(BIKES_INVENTORY, myPath).get(0);
        JsonParser parser = redis.getJsonParser();

        MountainBike desertFox = new MountainBike();
        desertFox.specs = new Specs();
        desertFox.id = "bike:43";
        desertFox.model = "DesertFox";
        desertFox.description = "The DesertFox is a versatile bike for all terrains";
        desertFox.price = "1299";
        desertFox.specs.material = "composite";
        desertFox.specs.weight = "11";
        desertFox.colors = Arrays.asList("yellow", "orange");

        JsonValue newValue = parser.fromObject(desertFox);

        assertThat(newValue).isNotNull();
        assertThat(newValue.isNull()).isFalse();
        assertThat(newValue.isJsonObject()).isTrue();
        assertThat(newValue.asJsonObject().size()).isEqualTo(6);
        assertThat(newValue.asJsonObject().get("id").toString()).isEqualTo("\"bike:43\"");
        assertThat(newValue.asJsonObject().get("model").toString()).isEqualTo("\"DesertFox\"");
        assertThat(newValue.asJsonObject().get("description").toString())
                .isEqualTo("\"The DesertFox is a versatile bike for all terrains\"");
        assertThat(newValue.asJsonObject().get("price").toString()).isEqualTo("\"1299\"");
        assertThat(newValue.asJsonObject().get("specs").toString()).isEqualTo("{\"material\":\"composite\",\"weight\":\"11\"}");
        assertThat(newValue.asJsonObject().get("colors").toString()).isEqualTo("[\"yellow\",\"orange\"]");

        String result = redis.jsonSet(BIKES_INVENTORY, myPath, newValue);

        assertThat(result).isEqualTo("OK");
    }

    @Test
    void byteArrayCodec() throws ExecutionException, InterruptedException {
        JsonPath myPath = JsonPath.of("$..mountain_bikes");
        byte[] myMountainBikesKey = BIKES_INVENTORY.getBytes();
        byte[] myServiceBikesKey = "service_bikes".getBytes();

        RedisAsyncCommands<byte[], byte[]> redis = client.connect(ByteArrayCodec.INSTANCE).async();
        RedisFuture<List<JsonValue>> bikes = redis.jsonGet(myMountainBikesKey, myPath);

        CompletionStage<RedisFuture<String>> stage = bikes
                .thenApply(fetchedBikes -> redis.jsonSet(myServiceBikesKey, JsonPath.ROOT_PATH, fetchedBikes.get(0)));

        String result = stage.toCompletableFuture().get().get();

        assertThat(result).isEqualTo("OK");
    }

    @Test
    void withCustomParser() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

        try (RedisClient client = RedisClient.create(redisURI)) {
            client.setOptions(ClientOptions.builder().jsonParser(CustomParser::new).build());
            StatefulRedisConnection<String, String> connection = client.connect(StringCodec.UTF8);
            RedisCommands<String, String> redis = connection.sync();
            assertThat(redis.getJsonParser()).isInstanceOf(CustomParser.class);
        }
    }

    static class CustomParser implements JsonParser {

        @Override
        public JsonValue loadJsonValue(ByteBuffer bytes) {
            return null;
        }

        @Override
        public JsonValue createJsonValue(ByteBuffer bytes) {
            return null;
        }

        @Override
        public JsonValue createJsonValue(String value) {
            return null;
        }

        @Override
        public JsonObject createJsonObject() {
            return null;
        }

        @Override
        public JsonArray createJsonArray() {
            return null;
        }

        @Override
        public JsonValue fromObject(Object object) {
            return null;
        }

    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "..mountain_bikes[0:2].model", "$..mountain_bikes[0:2].model" })
    void jsonGetRaw(String path) {
        JsonPath myPath = JsonPath.of(path);
        List<String> value = redis.jsonGetRaw(BIKES_INVENTORY, myPath);
        assertThat(value).hasSize(1);
        if (path.startsWith("$")) {
            assertThat(value.get(0)).isEqualTo("[\"Phoebe\",\"Quaoar\"]");
        } else {
            assertThat(value.get(0)).isEqualTo("\"Phoebe\"");
        }
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "..model", "$..model" })
    void jsonMGetRaw(String path) {
        JsonPath myPath = JsonPath.of(path);
        List<String> value = redis.jsonMGetRaw(myPath, BIKES_INVENTORY);
        assertThat(value).hasSize(1);
        if (path.startsWith("$")) {
            assertThat(value.get(0)).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\"]");
        } else {
            assertThat(value.get(0)).isEqualTo("\"Phoebe\"");
        }
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonArrpopRaw(String path) {
        JsonPath myPath = JsonPath.of(path);
        List<String> poppedJson = redis.jsonArrpopRaw(BIKES_INVENTORY, myPath);
        assertThat(poppedJson).hasSize(1);
        assertThat(poppedJson.get(0)).contains(
                "{\"id\":\"bike:3\",\"model\":\"Weywot\",\"description\":\"This bike gives kids aged six years and old");
    }

    @Test
    public void jsonArrpopRawEmptyArray() {
        JsonValue value = redis.getJsonParser().createJsonValue("[\"one\"]");
        redis.jsonSet("myKey", JsonPath.ROOT_PATH, value);
        List<String> result = redis.jsonArrpopRaw("myKey");
        assertThat(result.toString()).isEqualTo("[\"one\"]");
        assertThat(redis.jsonGetRaw("myKey").get(0)).isEqualTo("[]");
    }

    @Test
    void jsonMGetReturnsMultipleElementsForMultipleKeys() {
        // Setup: Create two separate JSON documents
        JsonParser parser = redis.getJsonParser();
        JsonValue bike1 = parser.createJsonValue("{\"id\":\"bike:100\",\"model\":\"Speedy\"}");
        JsonValue bike2 = parser.createJsonValue("{\"id\":\"bike:200\",\"model\":\"Cruiser\"}");
        JsonValue bike3 = parser.createJsonValue("{\"id\":\"bike:300\",\"model\":\"Racer\"}");

        redis.jsonSet("test:bike1", JsonPath.ROOT_PATH, bike1);
        redis.jsonSet("test:bike2", JsonPath.ROOT_PATH, bike2);
        redis.jsonSet("test:bike3", JsonPath.ROOT_PATH, bike3);

        // Query multiple keys
        List<JsonValue> values = redis.jsonMGet(JsonPath.ROOT_PATH, "test:bike1", "test:bike2", "test:bike3");

        // Verify that the list contains multiple elements (one per key)
        assertThat(values).hasSize(3);
        assertThat(values.get(0).toString()).contains("bike:100");
        assertThat(values.get(1).toString()).contains("bike:200");
        assertThat(values.get(2).toString()).contains("bike:300");
    }

    @Test
    void jsonArrpopReturnsMultipleElementsForMultipleArrayMatches() {
        // Path $..colors matches the colors array in both bike:1 and bike:2
        JsonPath colorsPath = JsonPath.of("$..colors");

        // Pop from all matching colors arrays
        List<JsonValue> poppedValues = redis.jsonArrpop(BIKES_INVENTORY, colorsPath);

        // Should return 2 elements (one from each colors array that exists)
        // bike:1 colors: ["black", "silver"] -> pops "silver"
        // bike:2 colors: ["black", "white"] -> pops "white"
        assertThat(poppedValues).hasSize(2);
        // The popped elements should be the last elements from each array
        assertThat(poppedValues.get(0).toString()).isEqualTo("\"silver\"");
        assertThat(poppedValues.get(1).toString()).isEqualTo("\"white\"");
    }

    @Test
    void jsonArrpopWithIndexReturnsMultipleElementsForMultipleArrayMatches() {
        // Path $..colors matches colors arrays
        JsonPath colorsPath = JsonPath.of("$..colors");

        // Pop from index 0 of all matching colors arrays
        List<JsonValue> poppedValues = redis.jsonArrpop(BIKES_INVENTORY, colorsPath, 0);

        // Should return 2 elements (one from each colors array)
        // bike:1 colors: ["black", "silver"] -> pops "black"
        // bike:2 colors: ["black", "white"] -> pops "black"
        assertThat(poppedValues).hasSize(2);
        assertThat(poppedValues.get(0).toString()).isEqualTo("\"black\"");
        assertThat(poppedValues.get(1).toString()).isEqualTo("\"black\"");
    }

    @Test
    void jsonGetWithMultiplePathsReturnsMultipleElements() {
        // Query two different paths
        JsonPath modelPath = JsonPath.of("$..model");
        JsonPath idPath = JsonPath.of("$..id");

        List<JsonValue> values = redis.jsonGet(BIKES_INVENTORY, modelPath, idPath);

        // With multiple paths, jsonGet returns a single JSON object containing both paths' results
        // The List<JsonValue> will have 1 element which is the combined result
        assertThat(values).hasSize(1);
        String result = values.get(0).toString();
        // The result should contain both paths as keys
        assertThat(result).contains("$..model");
        assertThat(result).contains("$..id");
    }

    @Test
    void jsonMGetWithMultipleKeysAndJsonPath() {
        // Setup: Create documents with nested structure
        JsonParser parser = redis.getJsonParser();
        JsonValue doc1 = parser.createJsonValue("{\"items\":[{\"name\":\"a\"},{\"name\":\"b\"}]}");
        JsonValue doc2 = parser.createJsonValue("{\"items\":[{\"name\":\"c\"},{\"name\":\"d\"}]}");

        redis.jsonSet("test:doc1", JsonPath.ROOT_PATH, doc1);
        redis.jsonSet("test:doc2", JsonPath.ROOT_PATH, doc2);

        // Query multiple keys with a JSONPath
        JsonPath namePath = JsonPath.of("$..name");
        List<JsonValue> values = redis.jsonMGet(namePath, "test:doc1", "test:doc2");

        // Should return 2 elements (one per key)
        assertThat(values).hasSize(2);
        // Each element should contain the array of names
        assertThat(values.get(0).toString()).isEqualTo("[\"a\",\"b\"]");
        assertThat(values.get(1).toString()).isEqualTo("[\"c\",\"d\"]");
    }

    @Test
    void jsonMGetReturnsNullForMissingKeys() {
        JsonParser parser = redis.getJsonParser();
        JsonValue doc = parser.createJsonValue("{\"value\":42}");
        redis.jsonSet("test:exists", JsonPath.ROOT_PATH, doc);

        List<JsonValue> values = redis.jsonMGet(JsonPath.ROOT_PATH, "test:exists", "test:missing", "test:exists");

        // Should return 3 elements
        assertThat(values).hasSize(3);
        assertThat(values.get(0).isNull()).isFalse();
        assertThat(values.get(1).isNull()).isTrue(); // Missing key returns JSON null
        assertThat(values.get(2).isNull()).isFalse();
    }

    @Test
    void jsonArrpopReturnsNullForNonArrayPaths() {
        // Create a document with multiple paths, some arrays, some not
        JsonParser parser = redis.getJsonParser();
        JsonValue doc = parser
                .createJsonValue("{\"arr1\":[1,2,3],\"notArray\":\"string\",\"arr2\":[4,5,6],\"nested\":{\"arr3\":[7,8,9]}}");
        redis.jsonSet("test:mixed", JsonPath.ROOT_PATH, doc);

        // Use wildcard to match all top-level fields
        JsonPath wildcardPath = JsonPath.of("$.*");
        List<JsonValue> poppedValues = redis.jsonArrpop("test:mixed", wildcardPath);

        // Should return results for all 4 matched paths
        // arr1 -> 3, notArray -> null (not array), arr2 -> 6, nested -> null (not array)
        assertThat(poppedValues).hasSize(4);
        assertThat(poppedValues.get(0).toString()).isEqualTo("3"); // arr1
        assertThat(poppedValues.get(1).isNull()).isTrue(); // notArray (string, not array)
        assertThat(poppedValues.get(2).toString()).isEqualTo("6"); // arr2
        assertThat(poppedValues.get(3).isNull()).isTrue(); // nested (object, not array)
    }

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { "..mountain_bikes[0:2].model", "$..mountain_bikes[0:2].model" })
    void jsonGetValueReturnsSingleJsonValue(String path) {
        JsonPath myPath = JsonPath.of(path);

        JsonValue value = redis.jsonGetValue(BIKES_INVENTORY, myPath);
        assertThat(value).isNotNull();

        if (path.startsWith("$")) {
            assertThat(value.toString()).isEqualTo("[\"Phoebe\",\"Quaoar\"]");
            assertThat(value.isJsonArray()).isTrue();
            assertThat(value.asJsonArray().size()).isEqualTo(2);
        } else {
            assertThat(value.toString()).isEqualTo("\"Phoebe\"");
        }
    }

    @Test
    void jsonGetValueWithArgs() {
        JsonPath myPath = JsonPath.of("$..model");
        JsonGetArgs args = JsonGetArgs.Builder.defaults();

        JsonValue value = redis.jsonGetValue(BIKES_INVENTORY, args, myPath);
        assertThat(value).isNotNull();
        assertThat(value.toString()).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\"]");
    }

    @Test
    void jsonGetValueWithMultiplePaths() {
        JsonPath modelPath = JsonPath.of("$..model");
        JsonPath idPath = JsonPath.of("$..id");

        JsonValue value = redis.jsonGetValue(BIKES_INVENTORY, modelPath, idPath);
        assertThat(value).isNotNull();
        // The result should be a JSON object containing both paths as keys
        assertThat(value.asJsonObject().get("$..model").isJsonArray()).isTrue();
        assertThat(value.asJsonObject().get("$..id").isJsonArray()).isTrue();

        assertThat(value.asJsonObject().get("$..model").asJsonArray().size()).isEqualTo(3);
        assertThat(value.asJsonObject().get("$..id").asJsonArray().size()).isEqualTo(3);

        assertThat(value.asJsonObject().get("$..model").asJsonArray().asList().get(0).toString()).isEqualTo("\"Phoebe\"");
        assertThat(value.asJsonObject().get("$..id").asJsonArray().asList().get(0).toString()).isEqualTo("\"bike:1\"");
    }

    @Test
    void jsonGetValueRawReturnsSingleString() {
        JsonPath myPath = JsonPath.of("$..model");

        String value = redis.jsonGetValueRaw(BIKES_INVENTORY, myPath);
        assertThat(value).isNotNull();
        assertThat(value).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\"]");
    }

    @Test
    void jsonGetValueRawWithArgs() {
        JsonPath myPath = JsonPath.of("$..model");
        JsonGetArgs args = JsonGetArgs.Builder.indent("  ").newline("\n");

        String value = redis.jsonGetValueRaw(BIKES_INVENTORY, args, myPath);
        assertThat(value).isNotNull();
        // Should contain pretty-printed JSON
        assertThat(value).isEqualTo("[\n  \"Phoebe\",\n  \"Quaoar\",\n  \"Weywot\"\n]");
    }

}
