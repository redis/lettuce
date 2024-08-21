# Integration and Extension

## Codecs

Codecs are a pluggable mechanism for transcoding keys and values between
your application and Redis. The default codec supports UTF-8 encoded
String keys and values.

Each connection may have its codec passed to the extended
`RedisClient.connect` methods:

``` java
StatefulRedisConnection<K, V> connect(RedisCodec<K, V> codec)
StatefulRedisPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec)
```

Lettuce ships with predefined codecs:

- `io.lettuce.core.codec.ByteArrayCodec` - use `byte[]` for keys and
  values

- `io.lettuce.core.codec.StringCodec` - use Strings for keys and values.
  Using the default charset or a specified `Charset` with improved
  support for `US_ASCII` and `UTF-8`.

- `io.lettuce.core.codec.CipherCodec` - used for transparent encryption
  of values.

- `io.lettuce.core.codec.CompressionCodec` - apply `GZIP` or `DEFLATE`
  compression to values.

Publish/Subscribe connections use channel names and patterns for keys;
messages are treated as values.

Keys and values can be encoded independently from each other which means
the key can be a `java.lang.String` while the value is a `byte[]`. Many
other constellations are possible like:

- Representing your data as JSON if your data is mapped to a particular
  Java type. Different types are complex to map since the codec applies
  to all operations.

- Serialize your data using the Java Serializer
  (`ObjectInputStream`/`ObjectOutputStream`). Allows type-safe
  conversions but is less interoperable with other languages

- Serializing your data using
  [Kryo](https://github.com/EsotericSoftware/kryo) for improved
  type-safe serialization.

- Any specialized codecs like the `BitStringCodec` (see below)

### Exception handling during Encoding

Codecs should be designed in a way that doesn’t allow encoding
exceptions except for Out-of-Memory scenarios. Encoding of keys and
values happens on the Event Loop after registering a command in the
protocol stack and sending a command to the write queue. Exceptions at
that stage will leave the command in the protocol stack while a command
might have not been sent to Redis because encoding has failed. Such a
state desynchronizes the protocol state and your commands will fail
with:
`Cannot encode command. Please close the connection as the connection state may be out of sync.`.

JSON and JDK serialization can fail because the underlying object graph
cannot be serialized (i.e. an object does not implement `Serializable`
or Jackson cannot serialize a value because of misconfiguration). If you
want to remain safe (and remove encoding load from the Event Loop),
rather serialize such objects beforehand and use the resulting `byte[]`
as value input to Redis commands.

### Why `ByteBuffer` instead of `byte[]`

The `RedisCodec` interface accepts and returns `ByteBuffer`s for data
interchange. A `ByteBuffer` is not opinionated about the source of the
underlying bytes. The `byte[]` interface of Lettuce 3.x required the
user to provide an array with the exact data for interchange. So if you
have an array where you want to use only a subset, you’re required to
create a new instance of a byte array and copy the data. The same
applies if you have a different byte source (e.g. netty's `ByteBuf` or
an NIO `ByteBuffer`). The `ByteBuffer`s for decoding are pointers to the
underlying data. `ByteBuffer`s for encoding data can be either pure
pointers or allocated memory. Lettuce does not free any memory (such as
pooled buffers).

### Diversity in Codecs

As in every other segment of technology, there is no one-fits-it-all
solution when it comes to Codecs. Redis data structures provide a
variety of The key and value limitation of codecs is intentionally and a
balance amongst convenience and simplicity. The Redis API allows much
more variance in encoding and decoding particular data elements. A good
example is Redis hashes. A hash is identified by its key but stores
another key/value pairs. The keys of the key-value pairs could be
encoded using a different approach than the key of the hash. Another
different approach might be to use different encodings between lists and
sets. Using a base codec (such as UTF-8 or byte array) and performing an
own conversion on top of the base codec is often the better idea.

### Multi-Threading

A key point in Codecs is that Codecs are shared resources and can be
used by multiple threads. Your Codec needs to be thread-safe (by
shared-nothing, pooling or synchronization). Every logical Lettuce
connection uses its codec instance. Codec instances are shared as soon
as multiple threads are issuing commands or if you use Redis Cluster.

### Compression

Compression can be a good idea when storing larger chunks of data within
Redis. Any textual data structures (such as JSON or XML) are suited for
compression. Compression is handled at Codec-level which means you do
not have to change your application to apply compression. The
`CompressionCodec` provides basic and transparent compression for values
using either GZIP or Deflate compression:

``` java
StatefulRedisConnection<String, Object> connection = client.connect(
                CompressionCodec.valueCompressor(new SerializedObjectCodec(), CompressionCodec.CompressionType.GZIP)).sync();

StatefulRedisConnection<String, String> connection = client.connect(
                CompressionCodec.valueCompressor(StringCodec.UTF8, CompressionCodec.CompressionType.DEFLATE)).sync();
```

Compression can be used with any codec, the compressor just wraps the
inner `RedisCodec` and compresses/decompresses the data that is
interchanged. You can build your own compressor the same way as you can
provide own codecs.

### Examples

``` java
public class BitStringCodec extends StringCodec {
    @Override
    public String decodeValue(ByteBuffer bytes) {
        StringBuilder bits = new StringBuilder(bytes.remaining() * 8);
        while (bytes.remaining() > 0) {
            byte b = bytes.get();
            for (int i = 0; i < 8; i++) {
                bits.append(Integer.valueOf(b >>> i & 1));
            }
        }
        return bits.toString();
    }
}

StatefulRedisConnection<String, String> connection = client.connect(new BitStringCodec());
RedisCommands<String, String> redis = connection.sync();

redis.setbit(key, 0, 1);
redis.setbit(key, 1, 1);
redis.setbit(key, 2, 0);
redis.setbit(key, 3, 0);
redis.setbit(key, 4, 0);
redis.setbit(key, 5, 1);

redis.get(key) == "00100011"
```

``` java
public class SerializedObjectCodec implements RedisCodec<String, Object> {
    private Charset charset = Charset.forName("UTF-8");

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return charset.decode(bytes).toString();
    }

    @Override
    public Object decodeValue(ByteBuffer bytes) {
        try {
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(array));
            return is.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return charset.encode(key);
    }

    @Override
    public ByteBuffer encodeValue(Object value) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bytes);
            os.writeObject(value);
            return ByteBuffer.wrap(bytes.toByteArray());
        } catch (IOException e) {
            return ByteBuffer.wrap(new byte[0]);
        }
    }
}
```

## CDI Support

CDI support for Lettuce is available for `RedisClient` and
`RedisClusterClient`. You need to provide a `RedisURI` in order to get
Lettuce injected.

### RedisURI producer

Implement a simple producer (either field producer or producer method)
of `RedisURI`:

``` java
@Produces
public RedisURI redisURI() {
    return RedisURI.Builder.redis("localhost").build();
}
```

Lettuce also supports qualified `RedisURI`'s:

``` java
@Produces
@PersonDB
public RedisURI redisURI() {
    return RedisURI.Builder.redis("localhost").build();
}
```

### Injection

After declaring your `RedisURI`'s you can start using Lettuce in your
classes:

``` java
public class InjectedClient {

    @Inject
    private RedisClient redisClient;

    @Inject
    private RedisClusterClient redisClusterClient;

    @Inject
    @PersonDB
    private RedisClient redisClient;

    private RedisConnection<String, String> connection;

    @PostConstruct
    public void postConstruct() {
        connection = redisClient.connect();
    }

    public void pingRedis() {
        connection.ping();
    }

    @PreDestroy
    public void preDestroy() {
        if (connection != null) {
            connection.close();
        }
    }
}
```

### Activating Lettuce’s CDI extension

By default, you just drop Lettuce on your classpath and declare at least
one `RedisURI` bean. That’s all.

The CDI extension registers one bean pair (`RedisClient` and
`RedisClusterClient`) per discovered `RedisURI`. This means, if you do
not declare any `RedisURI` producers, the CDI extension won’t be
activated at all. This way you can use Lettuce in CDI-capable containers
without even activating the CDI extension.

All produced beans (`RedisClient` and `RedisClusterClient`) remain
active as long as your application is running since the beans are
`@ApplicationScoped`.

