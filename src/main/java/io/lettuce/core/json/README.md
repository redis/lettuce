# RedisJSON support in Lettuce

Lettuce supports [RedisJSON](https://oss.redis.com/redisjson/)  starting from [Lettuce 6.5.0.RELEASE](https://github.com/redis/lettuce/releases/tag/6.5.0.RELEASE).

The driver generally allows three distinct ways of working with the RedisJSON module:
* Default JSON parsing using GSON behind the scenes (Default mode)
* Custom JSON parsing using a user-provided JSON parser library (Advanced mode)
* Unprocessed JSON documents (Power-user mode)

> [!IMPORTANT]
> In all of the above modes the driver would refrain from processing the JSON document in the main event loop and instead
delegate this to the user thread. This behaviour is consistent for both reading and writing JSON documents - when
reading the parsing is happening as a lazy-loading mechanism whenever a method is called that requires the JSON to be
parsed; when writing the JSON is serialized after it is passed to any of the commands.


## Default mode
Best for:
* Applications that do little to no processing on the Java layer
```java

```

## Advanced mode
Best for:
* Applications that do little to no processing on the Java layer
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
try(StatefulRedisConnection<ByteBuffer, ByteBuffer> connect = redisClient.connect()){
    redis = connect.async();

}
```
