/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisContainerIntegrationTests;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.arguments.JsonMsetArgs;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

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

    @ParameterizedTest(name = "With {0} as path")
    @ValueSource(strings = { MOUNTAIN_BIKES_V1, MOUNTAIN_BIKES_V2 })
    void jsonArrpop(String path) {
        JsonPath myPath = JsonPath.of(path);

        List<JsonValue> poppedJson = redis.jsonArrpop(BIKES_INVENTORY, myPath);
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
        List<JsonValue> value = redis.jsonGet(BIKES_INVENTORY, myPath);
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
        List<JsonValue> value = redis.jsonGet(BIKES_INVENTORY, myPath);
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
            assertThat(value.get(0).toValue()).isEqualTo("[\"Phoebe\",\"Quaoar\",\"Weywot\"]");
        } else {
            assertThat(value.get(0).toValue()).isEqualTo("\"Phoebe\"");
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
        assertThat(value.asJsonArray().asList().get(0).toValue()).contains(
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
        assertThat(newValue.asJsonObject().get("id").toValue()).isEqualTo("\"bike:43\"");
        assertThat(newValue.asJsonObject().get("model").toValue()).isEqualTo("\"DesertFox\"");
        assertThat(newValue.asJsonObject().get("description").toValue())
                .isEqualTo("\"The DesertFox is a versatile bike for all terrains\"");
        assertThat(newValue.asJsonObject().get("price").toValue()).isEqualTo("\"1299\"");
        assertThat(newValue.asJsonObject().get("specs").toValue()).isEqualTo("{\"material\":\"composite\",\"weight\":\"11\"}");
        assertThat(newValue.asJsonObject().get("colors").toValue()).isEqualTo("[\"yellow\",\"orange\"]");

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
            client.setOptions(ClientOptions.builder().jsonParser(new CustomParser()).build());
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

}
