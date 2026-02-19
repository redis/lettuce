# Pipelining and command flushing

Redis is a TCP server using the client-server model and what is called a
Request/Response protocol. This means that usually a request is
accomplished with the following steps:

- The client sends a query to the server and reads from the socket,
  usually in a blocking way, for the server response.

- The server processes the command and sends the response back to the
  client.

A request/response server can be implemented so that it is able to
process new requests even if the client did not already read the old
responses. This way it is possible to send multiple commands to the
server without waiting for the replies at all, and finally read the
replies in a single step.

Using the synchronous API, in general, the program flow is blocked until
the response is accomplished. The underlying connection is busy with
sending the request and receiving its response. Blocking, in this case,
applies only from a current Thread perspective, not from a global
perspective.

To understand why using a synchronous API does not block on a global
level we need to understand what this means. Lettuce is a non-blocking
and asynchronous client. It provides a synchronous API to achieve a
blocking behavior on a per-Thread basis to create await (synchronize) a
command response. Blocking does not affect other Threads per se. Lettuce
is designed to operate in a pipelining way. Multiple threads can share
one connection. While one Thread may process one command, the other
Thread can send a new command. As soon as the first request returns, the
first Thread’s program flow continues, while the second request is
processed by Redis and comes back at a certain point in time.

Lettuce is built on top of netty decouple reading from writing and to
provide thread-safe connections. The result is, that reading and writing
can be handled by different threads and commands are written and read
independent of each other but in sequence. You can find more details
about [message ordering](#message-ordering) to learn
about command ordering rules in single- and multi-threaded arrangements.
The transport and command execution layer does not block the processing
until a command is written, processed and while its response is read.
Lettuce sends commands at the moment they are invoked.

A good example is the [async API](user-guide/async-api.md). Every
invocation on the [async API](user-guide/async-api.md) returns a
`Future` (response handle) after the command is written to the netty
pipeline. A write to the pipeline does not mean, the command is written
to the underlying transport. Multiple commands can be written without
awaiting the response. Invocations to the API (sync, async and starting
with `4.0` also reactive API) can be performed by multiple threads.

Sharing a connection between threads is possible but keep in mind:

**The longer commands need for processing, the longer other invoker wait
for their results**

You should not use transactional commands (`MULTI`) on shared
connection. If you use Redis-blocking commands (e. g. `BLPOP`) all
invocations of the shared connection will be blocked until the blocking
command returns which impacts the performance of other threads. Blocking
commands can be a reason to use multiple connections.

## Command flushing

!!! NOTE
    Command flushing is an advanced topic and in most cases (i.e. unless
    your use-case is a single-threaded mass import application) you won’t
    need it as Lettuce uses pipelining by default.

The normal operation mode of Lettuce is to flush every command which
means, that every command is written to the transport after it was
issued. Any regular user desires this behavior. You can control command
flushing since Version `3.3`.

Why would you want to do this? A flush is an [expensive system
call](https://github.com/netty/netty/issues/1759) and impacts
performance. Batching, disabling auto-flushing, can be used under
certain conditions and is recommended if:

- You perform multiple calls to Redis and you’re not depending
  immediately on the result of the call

- You’re bulk-importing

Controlling the flush behavior is only available on the async API. The
sync API emulates blocking calls, and as soon as you invoke a command,
you can no longer interact with the connection until the blocking call
ends.

The `AutoFlushCommands` state is set per connection and, therefore
visible to all threads using a shared connection. If you want to omit
this effect, use dedicated connections. The `AutoFlushCommands` state
cannot be set on pooled connections by the Lettuce connection pooling.

!!! WARNING
    Do not use `setAutoFlushCommands(…)` when sharing a connection across
    threads, at least not without proper synchronization. According to the
    many questions and (invalid) bug reports using
    `setAutoFlushCommands(…)` in a multi-threaded scenario causes a lot of
    complexity overhead and is very likely to cause issues on your side.
    `setAutoFlushCommands(…)` can only be reliably used on single-threaded
    connection usage in scenarios like bulk-loading.

``` java
StatefulRedisConnection<String, String> connection = client.connect();
RedisAsyncCommands<String, String> commands = connection.async();

// disable auto-flushing
commands.setAutoFlushCommands(false);

// perform a series of independent calls
List<RedisFuture<?>> futures = Lists.newArrayList();
for (int i = 0; i < iterations; i++) {
    futures.add(commands.set("key-" + i, "value-" + i));
    futures.add(commands.expire("key-" + i, 3600));
}

// write all commands to the transport layer
commands.flushCommands();

// synchronization example: Wait until all futures complete
boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS,
                   futures.toArray(new RedisFuture[futures.size()]));

// later
connection.close();
```

### Performance impact

Commands invoked in the default flush-after-write mode perform in an
order of about 100Kops/sec (async/multithreaded execution). Grouping
multiple commands in a batch (size depends on your environment, but
batches between 50 and 1000 work nice during performance tests) can
increase the throughput up to a factor of 5x.

Pipelining within the Redis docs: <https://redis.io/docs/latest/develop/use/pipelining/>
