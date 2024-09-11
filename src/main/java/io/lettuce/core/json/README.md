# RedisJSON support in Lettuce

Lettuce supports [RedisJSON](https://oss.redis.com/redisjson/)  starting from [Lettuce 6.5.0.RELEASE](https://github.com/redis/lettuce/releases/tag/6.5.0.RELEASE).

The driver generally allows three distinct ways of working with the RedisJSON module:
* (Default mode) - default JSON parsing using Jackson behind the scenes 
* (Advanced mode) - custom JSON parsing using a user-provided JSON parser
* (Power-user mode) - unprocessed JSON documents that have not gone through any process of deserialization or serialization

> [!IMPORTANT]\
> In all the above modes, the driver would refrain from processing the JSON document in the main event loop and instead
delegate this to the user thread. This behaviour is consistent when both receiving and sending JSON documents - when
receiving the parsing is done lazily whenever a method is called that requires the JSON to be parsed; when sending the
JSON is serialized immediately after it is passed to any of the commands, but before dispatching the command to the
event loop.


## Default mode
Best for:
* Most typical use-cases where the JSON document is parsed and processed

### Example usage:

```java
RedisURI redisURI = RedisURI.Builder.redis("acme.com").build();
RedisClient redisClient = RedisClient.create(redisURI);
try (StatefulRedisConnection<ByteBuffer, ByteBuffer> connect = redisClient.connect()){
    redis = connect.async();
    JsonPath path = JsonPath.of("$..mountain_bikes[0:2].model");

    JsonParser parser = redis.getJsonParser();
    JsonObject bikeRecord = parser.createJsonObject();
    JsonObject bikeSpecs = parser.createJsonObject();
    JsonArray bikeColors = parser.createJsonArray();

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
    
    String result = redis.jsonSet("bikes:inventory", path, bikeRecord).get();
}
```

## Advanced mode
Best for:
* Applications that want to handle parsing manually - either by using another library or by implementing their own parser

### Example usage:

```java
RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();

try (RedisClient client = RedisClient.create(redisURI)) {
    client.setOptions(ClientOptions.builder().jsonParser(new CustomParser()).build());
    StatefulRedisConnection<String, String> connection = client.connect(StringCodec.UTF8);
    RedisCommands<String, String> redis = connection.sync();
}
```

## Power-user mode
Best for:
* Applications that do little to no processing on the Java layer

### Example usage:

```java
JsonPath myPath = JsonPath.of("$..mountain_bikes");
RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
try (RedisClient client = RedisClient.create(redisURI)) {
    RedisAsyncCommands<String, String> redis = client.connect().async();
    RedisFuture<List<JsonValue>> bikes = redis.jsonGet("bikes:inventory", myPath);

    CompletionStage<RedisFuture<String>> stage = bikes.thenApply(
            fetchedBikes -> redis.jsonSet("service_bikes", JsonPath.ROOT_PATH, fetchedBikes.get(0)));

    String result = stage.toCompletableFuture().get().get();
}    
```
