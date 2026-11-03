# Streaming API

Redis can contain a huge set of data. Collections can burst your memory,
when the amount of data is too massive for your heap. Lettuce can return
your collection data either as List/Set/Map or can push the data on
`StreamingChannel` interfaces.

`StreamingChannel`s are similar to callback methods. Every method, which
can return bulk data (except transactions/multi and some config methods)
specifies beside a regular method with a collection return class also
method which accepts a `StreamingChannel`. Lettuce interacts with a
`StreamingChannel` as the data arrives so data can be processed while
the command is running and is not yet completed.

There are 4 StreamingChannels accepting different data types:

- [KeyStreamingChannel](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/output/KeyStreamingChannel.html)

- [ValueStreamingChannel](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/output/ValueStreamingChannel.html)

- [KeyValueStreamingChannel](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/output/KeyValueStreamingChannel.html)

- [ScoredValueStreamingChannel](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/output/ScoredValueStreamingChannel.html)

The result of the streaming methods is the count of keys/values/key-value
pairs as `long` value.

!!! NOTE
    Don’t issue blocking calls (includes synchronous API calls to Lettuce)
    from inside of callbacks such as the streaming API as this would block
    the EventLoop. If you need to fetch data from Redis from inside a
    `StreamingChannel` callback, please use the asynchronous API or use
    the reactive API directly.

``` java
Long count = redis.hgetall(new KeyValueStreamingChannel<String, String>()
    {
        @Override
        public void onKeyValue(String key, String value)
        {
            ...
        }
    }, key);
```

Streaming happens real-time to the redis responses. The method call
(future) completes after the last call to the StreamingChannel.

## Examples

``` java
redis.lpush("key", "one")
redis.lpush("key", "two")
redis.lpush("key", "three")

Long count = redis.lrange(new ValueStreamingChannel<String, String>()
    {
        @Override
        public void onValue(String value)
        {
            System.out.println("Value: " + value);
        }
    }, "key",  0, -1);

System.out.println("Count: " + count);
```

will produce the following output:

    Value: one
    Value: two
    Value: three
    Count: 3
