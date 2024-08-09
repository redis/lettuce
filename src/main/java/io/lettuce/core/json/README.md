# RedisJSON support in Lettuce

Lettuce supports [RedisJSON](https://oss.redis.com/redisjson/)  starting from [Lettuce 6.5.0.RELEASE](https://github.com/redis/lettuce/releases/tag/6.5.0.RELEASE).

The driver generally allows three distinct ways of working with the RedisJSON module:
* (Default mode) - default JSON parsing using GSON behind the scenes 
* (Advanced mode) - custom JSON parsing using a user-provided JSON parser library
* (Power-user mode) - unprocessed JSON documents based on the Codec infrastructure

> [!IMPORTANT]\
> In all of the above modes the driver would refrain from processing the JSON document in the main event loop and instead
delegate this to the user thread. This behaviour is consistent when both receiving and sending JSON documents - when
receiving the parsing is done lazily whenever a method is called that requires the JSON to be parsed; when sending the
JSON is serialized immediately after it is passed to any of the commands, but before dispatching the command to the
event loop.


## Default mode
Best for:
* Most typical use-cases where the JSON document is parsed and processed

```java
RedisURI redisURI = RedisURI.Builder.redis("acme.com").build();
RedisClient redisClient = RedisClient.create(redisURI);
try(StatefulRedisConnection<ByteBuffer, ByteBuffer> connect = redisClient.connect()){
    redis = connect.async();
    JsonPath path = JsonPath.of("$..mountain_bikes[0:2].model");

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

    String result = redis.jsonSet("bikes:inventory", path, bikeRecord, args).get();
}
```

## Advanced mode
Best for:
* Applications that want to handle parsing manually - either by using another library or by implementing their own parser
```java

```

## Power-user mode
Best for:
* Applications that do little to no processing on the Java layer
* Applications that require that a specific custom RedisCodec be used

Example usage:

```java
RedisURI redisURI = RedisURI.Builder.redis("acme.com").build();
RedisClient redisClient = RedisClient.create(redisURI);
try(StatefulRedisConnection<ByteBuffer, ByteBuffer> connect = redisClient.connect(new ByteBufferCodec())){
    redis = connect.async();
    JsonPath path = JsonPath.of("$..mountain_bikes[0:2].model");

    List<JsonValue<ByteBuffer>> value = redis.jsonGet(BIKES_INVENTORY, JsonGetArgs.Builder.none(), path).get();
    return value;
}
```
