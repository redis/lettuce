# Connecting Redis

Connections to a Redis Standalone, Sentinel, or Cluster require a
specification of the connection details. The unified form is `RedisURI`.
You can provide the database, password and timeouts within the
`RedisURI`. You have following possibilities to create a `RedisURI`:

1.  Use an URI:

    ``` java
        RedisURI.create("redis://localhost/");
    ```

2.  Use the Builder

    ``` java
        RedisURI.Builder.redis("localhost", 6379).auth("password").database(1).build();
    ```

3.  Set directly the values in `RedisURI`

    ``` java
        new RedisURI("localhost", 6379, 60, TimeUnit.SECONDS);
    ```

## URI syntax

**Redis Standalone**

    redis :// [[username :] password@] host [:port][/database]
              [?[timeout=timeout[d|h|m|s|ms|us|ns]] [&clientName=clientName]
              [&libraryName=libraryName] [&libraryVersion=libraryVersion] ]

**Redis Standalone (SSL)**

    rediss :// [[username :] password@] host [: port][/database]
               [?[timeout=timeout[d|h|m|s|ms|us|ns]] [&clientName=clientName]
               [&libraryName=libraryName] [&libraryVersion=libraryVersion] ]

**Redis Standalone (Unix Domain Sockets)**

    redis-socket :// [[username :] password@]path
                     [?[timeout=timeout[d|h|m|s|ms|us|ns]] [&database=database]
                     [&clientName=clientName] [&libraryName=libraryName]
                     [&libraryVersion=libraryVersion] ]

**Redis Sentinel**

    redis-sentinel :// [[username :] password@] host1[:port1] [, host2[:port2]] [, hostN[:portN]] [/database]
                       [?[timeout=timeout[d|h|m|s|ms|us|ns]] [&sentinelMasterId=sentinelMasterId]
                       [&clientName=clientName] [&libraryName=libraryName]
                       [&libraryVersion=libraryVersion] ]

**Schemes**

- `redis` Redis Standalone

- `rediss` Redis Standalone SSL

- `redis-socket` Redis Standalone Unix Domain Socket

- `redis-sentinel` Redis Sentinel

**Timeout units**

- `d` Days

- `h` Hours

- `m` Minutes

- `s` Seconds

- `ms` Milliseconds

- `us` Microseconds

- `ns` Nanoseconds

Hint: The database parameter within the query part has higher precedence
than the database in the path.

RedisURI supports Redis Standalone, Redis Sentinel and Redis Cluster
with plain, SSL, TLS and unix domain socket connections.

Hint: The database parameter within the query part has higher precedence
than the database in the path. RedisURI supports Redis Standalone, Redis
Sentinel and Redis Cluster with plain, SSL, TLS and unix domain socket
connections.

## Authentication

Redis URIs may contain authentication details that effectively lead to
usernames with passwords, password-only, or no authentication.
Connections are authenticated by using the information provided through
`RedisCredentials`. Credentials are obtained at connection time from
`RedisCredentialsProvider`. When configuring username/password on the
URI statically, then a `StaticCredentialsProvider` holds the configured
information.

**Notes**

- When using Redis Sentinel, the password from the URI applies to the
  data nodes only. Sentinel authentication must be configured for each
  sentinel node.

- Usernames are supported as of Redis 6.

- Library name and library version are automatically set on Redis 7.2 or
  greater.

## Basic Usage

``` java
RedisClient client = RedisClient.create("redis://localhost");          

StatefulRedisConnection<String, String> connection = client.connect(); 

RedisCommands<String, String> commands = connection.sync();            

String value = commands.get("foo");                                    

...

connection.close();                                                    

client.shutdown();                                                     
```

- Create the `RedisClient` instance and provide a Redis URI pointing to
  localhost, Port 6379 (default port).

- Open a Redis Standalone connection. The endpoint is used from the
  initialized `RedisClient`

- Obtain the command API for synchronous execution. Lettuce supports
  asynchronous and reactive execution models, too.

- Issue a `GET` command to get the key `foo`.

- Close the connection when you’re done. This happens usually at the
  very end of your application. Connections are designed to be
  long-lived.

- Shut down the client instance to free threads and resources. This
  happens usually at the very end of your application.

Each Redis command is implemented by one or more methods with names
identical to the lowercase Redis command name. Complex commands with
multiple modifiers that change the result type include the CamelCased
modifier as part of the command name, e.g. `zrangebyscore` and
`zrangebyscoreWithScores`.

Redis connections are designed to be long-lived and thread-safe, and if
the connection is lost will reconnect until `close()` is called. Pending
commands that have not timed out will be (re)sent after successful
reconnection.

All connections inherit a default timeout from their RedisClient and  
and will throw a `RedisException` when non-blocking commands fail to
return a result before the timeout expires. The timeout defaults to 60
seconds and may be changed in the RedisClient or for each connection.
Synchronous methods will throw a `RedisCommandExecutionException` in
case Redis responds with an error. Asynchronous connections do not throw
exceptions when Redis responds with an error.

### RedisURI

The RedisURI contains the host/port and can carry
authentication/database details. On a successful connect you get
authenticated, and the database is selected afterward. This applies  
also after re-establishing a connection after a connection loss.

A Redis URI can also be created from an URI string. Supported formats
are:

- `redis://[password@]host[:port][/databaseNumber]` Plaintext Redis
  connection

- `rediss://[password@]host[:port][/databaseNumber]` [SSL
  Connections](Advanced-usage.md#ssl-connections) Redis connection

- `redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId`
  for using Redis Sentinel

- `redis-socket:///path/to/socket` [Unix Domain
  Sockets](Advanced-usage.md#unix-domain-sockets) connection to Redis

### Exceptions

In the case of an exception/error response from Redis, you’ll receive a
`RedisException` containing  
the error message. `RedisException` is a `RuntimeException`.

### Examples

``` java
RedisClient client = RedisClient.create(RedisURI.create("localhost", 6379));
client.setDefaultTimeout(20, TimeUnit.SECONDS);

// …

client.shutdown();
```

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost")
                                .withPassword("authentication")
                                .withDatabase(2)
                                .build();
RedisClient client = RedisClient.create(redisUri);

// …

client.shutdown();
```

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost")
                                .withSsl(true)
                                .withPassword("authentication")
                                .withDatabase(2)
                                .build();
RedisClient client = RedisClient.create(redisUri);

// …

client.shutdown();
```

``` java
RedisURI redisUri = RedisURI.create("redis://authentication@localhost/2");
RedisClient client = RedisClient.create(redisUri);

// …

client.shutdown();
```

## Asynchronous API

This guide will give you an impression how and when to use the
asynchronous API provided by Lettuce 4.x.

### Motivation

Asynchronous methodologies allow you to utilize better system resources,
instead of wasting threads waiting for network or disk I/O. Threads can
be fully utilized to perform other work instead. Lettuce facilitates
asynchronicity from building the client on top of
[netty](http://netty.io) that is a multithreaded, event-driven I/O
framework. All communication is handled asynchronously. Once the
foundation is able to processes commands concurrently, it is convenient
to take advantage from the asynchronicity. It is way harder to turn a
blocking and synchronous working software into a concurrently processing
system.

#### Understanding Asynchronicity

Asynchronicity permits other processing to continue before the
transmission has finished and the response of the transmission is
processed. This means, in the context of Lettuce and especially Redis,
that multiple commands can be issued serially without the need of
waiting to finish the preceding command. This mode of operation is also
known as [Pipelining](http://redis.io/topics/pipelining). The following
example should give you an impression of the mode of operation:

- Given client *A* and client *B*

- Client *A* triggers command `SET A=B`

- Client *B* triggers at the same time of Client *A* command `SET C=D`

- Redis receives command from Client *A*

- Redis receives command from Client *B*

- Redis processes `SET A=B` and responds `OK` to Client *A*

- Client *A* receives the response and stores the response in the
  response handle

- Redis processes `SET C=D` and responds `OK` to Client *B*

- Client *B* receives the response and stores the response in the
  response handle

Both clients from the example above can be either two threads or
connections within an application or two physically separated clients.

Clients can operate concurrently to each other by either being separate
processes, threads, event-loops, actors, fibers, etc. Redis processes
incoming commands serially and operates mostly single-threaded. This
means, commands are processed in the order they are received with some
characteristic that we’ll cover later.

Let’s take the simplified example and enhance it by some program flow
details:

- Given client *A*

- Client *A* triggers command `SET A=B`

- Client *A* uses the asynchronous API and can perform other processing

- Redis receives command from Client *A*

- Redis processes `SET A=B` and responds `OK` to Client *A*

- Client *A* receives the response and stores the response in the
  response handle

- Client *A* can access now the response to its command without waiting
  (non-blocking)

The Client *A* takes advantage from not waiting on the result of the
command so it can process computational work or issue another Redis
command. The client can work with the command result as soon as the
response is available.

#### Impact of asynchronicity to the synchronous API

While this guide helps you to understand the asynchronous API it is
worthwhile to learn the impact on the synchronous API. The general
approach of the synchronous API is no different than the asynchronous
API. In both cases, the same facilities are used to invoke and transport
commands to the Redis server. The only difference is a blocking behavior
of the caller that is using the synchronous API. Blocking happens on
command level and affects only the command completion part, meaning
multiple clients using the synchronous API can invoke commands on the
same connection and at the same time without blocking each other. A call
on the synchronous API is unblocked at the moment a command response was
processed.

- Given client *A* and client *B*

- Client *A* triggers command `SET A=B` on the synchronous API and waits
  for the result

- Client *B* triggers at the same time of Client *A* command `SET C=D`
  on the synchronous API and waits for the result

- Redis receives command from Client *A*

- Redis receives command from Client *B*

- Redis processes `SET A=B` and responds `OK` to Client *A*

- Client *A* receives the response and unblocks the program flow of
  Client *A*

- Redis processes `SET C=D` and responds `OK` to Client *B*

- Client *B* receives the response and unblocks the program flow of
  Client *B*

However, there are some cases you should not share a connection among
threads to avoid side-effects. The cases are:

- Disabling flush-after-command to improve performance

- The use of blocking operations like `BLPOP`. Blocking operations are
  queued on Redis until they can be executed. While one connection is
  blocked, other connections can issue commands to Redis. Once a command
  unblocks the blocking command (that said an `LPUSH` or `RPUSH` hits
  the list), the blocked connection is unblocked and can proceed after
  that.

- Transactions

- Using multiple databases

#### Result handles

Every command invocation on the asynchronous API creates a
`RedisFuture<T>` that can be canceled, awaited and subscribed
(listener). A `CompleteableFuture<T>` or `RedisFuture<T>` is a pointer
to the result that is initially unknown since the computation of its
value is yet incomplete. A `RedisFuture<T>` provides operations for
synchronization and chaining.

``` java
CompletableFuture<String> future = new CompletableFuture<>();

System.out.println("Current state: " + future.isDone());

future.complete("my value");

System.out.println("Current state: " + future.isDone());
System.out.println("Got value: " + future.get());
```

The example prints the following lines:

    Current state: false
    Current state: true
    Got value: my value

Attaching a listener to a future allows chaining. Promises can be used
synonymous to futures, but not every future is a promise. A promise
guarantees a callback/notification and thus it has come to its name.

A simple listener that gets called once the future completes:

``` java
final CompletableFuture<String> future = new CompletableFuture<>();

future.thenRun(new Runnable() {
    @Override
    public void run() {
        try {
            System.out.println("Got value: " + future.get());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
});

System.out.println("Current state: " + future.isDone());
future.complete("my value");
System.out.println("Current state: " + future.isDone());
```

The value processing moves from the caller into a listener that is then
called by whoever completes the future. The example prints the following
lines:

    Current state: false
    Got value: my value
    Current state: true

The code from above requires exception handling since calls to the
`get()` method can lead to exceptions. Exceptions raised during the
computation of the `Future<T>` are transported within an
`ExecutionException`. Another exception that may be thrown is the
`InterruptedException`. This is because calls to `get()` are blocking
calls and the blocked thread can be interrupted at any time. Just think
about a system shutdown.

The `CompletionStage<T>` type allows since Java 8 a much more
sophisticated handling of futures. A `CompletionStage<T>` can consume,
transform and build a chain of value processing. The code from above can
be rewritten in Java 8 in the following style:

``` java
CompletableFuture<String> future = new CompletableFuture<>();

future.thenAccept(new Consumer<String>() {
    @Override
    public void accept(String value) {
        System.out.println("Got value: " + value);
    }
});

System.out.println("Current state: " + future.isDone());
future.complete("my value");
System.out.println("Current state: " + future.isDone());
```

The example prints the following lines:

    Current state: false
    Got value: my value
    Current state: true

You can find the full reference for the `CompletionStage<T>` type in the
[Java 8 API
documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html).

### Creating futures using Lettuce

Lettuce futures can be used for initial and chaining operations. When
using Lettuce futures, you will notice the non-blocking behavior. This
is because all I/O and command processing are handled asynchronously
using the netty EventLoop. The Lettuce `RedisFuture<T>` extends a
`CompletionStage<T>` so all methods of the base type are available.

Lettuce exposes its futures on the Standalone, Sentinel,
Publish/Subscribe and Cluster APIs.

Connecting to Redis is insanely simple:

``` java
RedisClient client = RedisClient.create("redis://localhost");
RedisAsyncCommands<String, String> commands = client.connect().async();
```

In the next step, obtaining a value from a key requires the `GET`
operation:

``` java
RedisFuture<String> future = commands.get("key");
```

### Consuming futures

The first thing you want to do when working with futures is to consume
them. Consuming a futures means obtaining the value. Here is an example
that blocks the calling thread and prints the value:

``` java
RedisFuture<String> future = commands.get("key");
String value = future.get();
System.out.println(value);
```

Invocations to the `get()` method (pull-style) block the calling thread
at least until the value is computed but in the worst case indefinitely.
Using timeouts is always a good idea to not exhaust your threads.

``` java
try {
    RedisFuture<String> future = commands.get("key");
    String value = future.get(1, TimeUnit.MINUTES);
    System.out.println(value);
} catch (Exception e) {
    e.printStackTrace();
}
```

The example will wait at most 1 minute for the future to complete. If
the timeout exceeds, a `TimeoutException` is thrown to signal the
timeout.

Futures can also be consumed in a push style, meaning when the
`RedisFuture<T>` is completed, a follow-up action is triggered:

``` java
RedisFuture<String> future = commands.get("key");

future.thenAccept(new Consumer<String>() {
    @Override
    public void accept(String value) {
        System.out.println(value);
    }
});
```

Alternatively, written in Java 8 lambdas:

``` java
RedisFuture<String> future = commands.get("key");

future.thenAccept(System.out::println);
```

Lettuce futures are completed on the netty EventLoop. Consuming and
chaining futures on the default thread is always a good idea except for
one case: Blocking/long-running operations. As a rule of thumb, never
block the event loop. If you need to chain futures using blocking calls,
use the `thenAcceptAsync()`/`thenRunAsync()` methods to fork the
processing to another thread. The `…​async()` methods need a threading
infrastructure for execution, by default the `ForkJoinPool.commonPool()`
is used. The `ForkJoinPool` is statically constructed and does not grow
with increasing load. Using default `Executor`s is almost always the
better idea.

``` java
Executor sharedExecutor = ...
RedisFuture<String> future = commands.get("key");

future.thenAcceptAsync(new Consumer<String>() {
    @Override
    public void accept(String value) {
        System.out.println(value);
    }
}, sharedExecutor);
```

### Synchronizing futures

A key point when using futures is the synchronization. Futures are
usually used to:

1.  Trigger multiple invocations without the urge to wait for the
    predecessors (Batching)

2.  Invoking a command without awaiting the result at all (Fire&Forget)

3.  Invoking a command and perform other computing in the meantime
    (Decoupling)

4.  Adding concurrency to certain computational efforts (Concurrency)

There are several ways how to wait or get notified in case a future
completes. Certain synchronization techniques apply to some motivations
why you want to use futures.

#### Blocking synchronization

Blocking synchronization comes handy if you perform batching/add
concurrency to certain parts of your system. An example to batching can
be setting/retrieving multiple values and awaiting the results before a
certain point within processing.

``` java
List<RedisFuture<String>> futures = new ArrayList<RedisFuture<String>>();

for (int i = 0; i < 10; i++) {
    futures.add(commands.set("key-" + i, "value-" + i));
}

LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));
```

The code from above does not wait until a certain command completes
before it issues another one. The synchronization is done after all
commands are issued. The example code can easily be turned into a
Fire&Forget pattern by omitting the call to `LettuceFutures.awaitAll()`.

A single future execution can be also awaited, meaning an opt-in to wait
for a certain time but without raising an exception:

``` java
RedisFuture<String> future = commands.get("key");

if(!future.await(1, TimeUnit.MINUTES)) {
    System.out.println("Could not complete within the timeout");
}
```

Calling `await()` is friendlier to call since it throws only an
`InterruptedException` in case the blocked thread is interrupted. You
are already familiar with the `get()` method for synchronization, so we
will not bother you with this one.

At last, there is another way to synchronize futures in a blocking way.
The major caveat is that you will become responsible to handle thread
interruptions. If you do not handle that aspect, you will not be able to
shut down your system properly if it is in a running state.

``` java
RedisFuture<String> future = commands.get("key");
while (!future.isDone()) {
    // do something ...
}
```

While the `isDone()` method does not aim primarily for synchronization
use, it might come handy to perform other computational efforts while
the command is executed.

#### Chaining synchronization

Futures can be synchronized/chained in a non-blocking style to improve
thread utilization. Chaining works very well in systems relying on
event-driven characteristics. Future chaining builds up a chain of one
or more futures that are executed serially, and every chain member
handles a part in the computation. The `CompletionStage<T>` API offers
various methods to chain and transform futures. A simple transformation
of the value can be done using the `thenApply()` method:

``` java
future.thenApply(new Function<String, Integer>() {
    @Override
    public Integer apply(String value) {
        return value.length();
    }
}).thenAccept(new Consumer<Integer>() {
    @Override
    public void accept(Integer integer) {
        System.out.println("Got value: " + integer);
    }
});
```

Alternatively, written in Java 8 lambdas:

``` java
future.thenApply(String::length)
    .thenAccept(integer -> System.out.println("Got value: " + integer));
```

The `thenApply()` method accepts a function that transforms the value
into another one. The final `thenAccept()` method consumes the value for
final processing.

You have already seen the `thenRun()` method from previous examples. The
`thenRun()` method can be used to handle future completions in case the
data is not crucial to your flow:

``` java
future.thenRun(new Runnable() {
    @Override
    public void run() {
        System.out.println("Finished the future.");
    }
});
```

Keep in mind to execute the `Runnable` on a custom `Executor` if you are
doing blocking calls within the `Runnable`.

Another chaining method worth mentioning is the either-or chaining. A
couple of `…​Either()` methods are available on a `CompletionStage<T>`,
see the [Java 8 API
docs](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)
for the full reference. The either-or pattern consumes the value from
the first future that is completed. A good example might be two services
returning the same data, for instance, a Master-Replica scenario, but
you want to return the data as fast as possible:

``` java
RedisStringAsyncCommands<String, String> master = masterClient.connect().async();
RedisStringAsyncCommands<String, String> replica = replicaClient.connect().async();

RedisFuture<String> future = master.get("key");
future.acceptEither(replica.get("key"), new Consumer<String>() {
    @Override
    public void accept(String value) {
      System.out.println("Got value: " + value);
    }
});
```

### Error handling

Error handling is an indispensable component of every real world
application and should to be considered from the beginning on. Futures
provide some mechanisms to deal with errors.

In general, you want to react in the following ways:

- Return a default value instead

- Use a backup future

- Retry the future

`RedisFuture<T>`s transport exceptions if any occurred. Calls to the
`get()` method throw the occurred exception wrapped within an
`ExecutionException` (this is different to Lettuce 3.x). You can find
more details within the Javadoc on
[CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html).

The following code falls back to a default value after it runs to an
exception by using the `handle()` method:

``` java
future.handle(new BiFunction<String, Throwable, String>() {
    @Override
    public Integer apply(String value, Throwable throwable) {
        if(throwable != null) {
          return "default value";
        }
        return value;
    }
}).thenAccept(new Consumer<String>() {
    @Override
    public void accept(String value) {
        System.out.println("Got value: " + value);
    }
});
```

More sophisticated code could decide on behalf of the throwable type
that value to return, as the shortcut example using the
`exceptionally()` method:

``` java
future.exceptionally(new Function<Throwable, String>() {
    @Override
    public String apply(Throwable throwable) {
        if (throwable instanceof IllegalStateException) {
            return "default value";
        }

        return "other default value";
    }
});
```

Retrying futures and recovery using futures is not part of the Java 8
`CompleteableFuture<T>`. See the [Reactive API](#reactive-api) for
comfortable ways handling with exceptions.

### Examples

``` java
RedisAsyncCommands<String, String> async = client.connect().async();
RedisFuture<String> set = async.set("key", "value");
RedisFuture<String> get = async.get("key");

set.get() == "OK"
get.get() == "value"
```

``` java
RedisAsyncCommands<String, String> async = client.connect().async();
RedisFuture<String> set = async.set("key", "value");
RedisFuture<String> get = async.get("key");

set.await(1, SECONDS) == true
set.get() == "OK"
get.get(1, TimeUnit.MINUTES) == "value"
```

``` java
RedisStringAsyncCommands<String, String> async = client.connect().async();
RedisFuture<String> set = async.set("key", "value");

Runnable listener = new Runnable() {
    @Override
    public void run() {
            ...;
    }
};

set.thenRun(listener);
```

## Reactive API

This guide helps you to understand the Reactive Stream pattern and aims
to give you a general understanding of how to build reactive
applications.

### Motivation

Asynchronous and reactive methodologies allow you to utilize better
system resources, instead of wasting threads waiting for network or disk
I/O. Threads can be fully utilized to perform other work instead.

A broad range of technologies exists to facilitate this style of
programming, ranging from the very limited and less usable
`java.util.concurrent.Future` to complete libraries and runtimes like
Akka. [Project Reactor](http://projectreactor.io/), has a very rich set
of operators to compose asynchronous workflows, it has no further
dependencies to other frameworks and supports the very mature Reactive
Streams model.

### Understanding Reactive Streams

Reactive Streams is an initiative to provide a standard for asynchronous
stream processing with non-blocking back pressure. This encompasses
efforts aimed at runtime environments (JVM and JavaScript) as well as
network protocols.

The scope of Reactive Streams is to find a minimal set of interfaces,
methods, and protocols that will describe the necessary operations and
entities to achieve the goal—asynchronous streams of data with
non-blocking back pressure.

It is an interoperability standard between multiple reactive composition
libraries that allow interaction without the need of bridging between
libraries in application code.

The integration of Reactive Streams is usually accompanied with the use
of a composition library that hides the complexity of bare
`Publisher<T>` and `Subscriber<T>` types behind an easy-to-use API.
Lettuce uses [Project Reactor](http://projectreactor.io/) that exposes
its publishers as `Mono` and `Flux`.

For more information about Reactive Streams see
<http://reactive-streams.org>.

### Understanding Publishers

Asynchronous processing decouples I/O or computation from the thread
that invoked the operation. A handle to the result is given back,
usually a `java.util.concurrent.Future` or similar, that returns either
a single object, a collection or an exception. Retrieving a result, that
was fetched asynchronously is usually not the end of processing one
flow. Once data is obtained, further requests can be issued, either
always or conditionally. With Java 8 or the Promise pattern, linear
chaining of futures can be set up so that subsequent asynchronous
requests are issued. Once conditional processing is needed, the
asynchronous flow has to be interrupted and synchronized. While this
approach is possible, it does not fully utilize the advantage of
asynchronous processing.

In contrast to the preceding examples, `Publisher<T>` objects answer the
multiplicity and asynchronous questions in a different fashion: By
inverting the `Pull` pattern into a `Push` pattern.

**A Publisher is the asynchronous/push “dual” to the synchronous/pull
Iterable**

| event          | Iterable (pull)  | Publisher (push)   |
|----------------|------------------|--------------------|
| retrieve data  | T next()         | onNext(T)          |
| discover error | throws Exception | onError(Exception) |
| complete       | !hasNext()       | onCompleted()      |

An `Publisher<T>` supports emission sequences of values or even infinite
streams, not just the emission of single scalar values (as Futures do).
You will very much appreciate this fact once you start to work on
streams instead of single values. Project Reactor uses two types in its
vocabulary: `Mono` and `Flux` that are both publishers.

A `Mono` can emit `0` to `1` events while a `Flux` can emit `0` to `N`
events.

A `Publisher<T>` is not biased toward some particular source of
concurrency or asynchronicity and how the underlying code is executed -
synchronous or asynchronous, running within a `ThreadPool`. As a
consumer of a `Publisher<T>`, you leave the actual implementation to the
supplier, who can change it later on without you having to adapt your
code.

The last key point of a `Publisher<T>` is that the underlying processing
is not started at the time the `Publisher<T>` is obtained, rather its
started at the moment an observer subscribes or signals demand to the
`Publisher<T>`. This is a crucial difference to a
`java.util.concurrent.Future`, which is started somewhere at the time it
is created/obtained. So if no observer ever subscribes to the
`Publisher<T>`, nothing ever will happen.

### A word on the lettuce Reactive API

All commands return a `Flux<T>`, `Mono<T>` or `Mono<Void>` to which a
`Subscriber` can subscribe to. That subscriber reacts to whatever item
or sequence of items the `Publisher<T>` emits. This pattern facilitates
concurrent operations because it does not need to block while waiting
for the `Publisher<T>` to emit objects. Instead, it creates a sentry in
the form of a `Subscriber` that stands ready to react appropriately at
whatever future time the `Publisher<T>` does so.

### Consuming `Publisher<T>`

The first thing you want to do when working with publishers is to
consume them. Consuming a publisher means subscribing to it. Here is an
example that subscribes and prints out all the items emitted:

``` java
Flux.just("Ben", "Michael", "Mark").subscribe(new Subscriber<String>() {
    public void onSubscribe(Subscription s) {
        s.request(3);
    }

    public void onNext(String s) {
        System.out.println("Hello " + s + "!");
    }

    public void onError(Throwable t) {

    }

    public void onComplete() {
        System.out.println("Completed");
    }
});
```

The example prints the following lines:

    Hello Ben
    Hello Michael
    Hello Mark
    Completed

You can see that the Subscriber (or Observer) gets notified of every
event and also receives the completed event. A `Publisher<T>` emits
items until either an exception is raised or the `Publisher<T>` finishes
the emission calling `onCompleted`. No further elements are emitted
after that time.

A call to the `subscribe` registers a `Subscription` that allows to
cancel and, therefore, do not receive further events. Publishers can
interoperate with the un-subscription and free resources once a
subscriber unsubscribed from the `Publisher<T>`.

Implementing a `Subscriber<T>` requires implementing numerous methods,
so lets rewrite the code to a simpler form:

``` java
Flux.just("Ben", "Michael", "Mark").doOnNext(new Consumer<String>() {
    public void accept(String s) {
        System.out.println("Hello " + s + "!");
    }
}).doOnComplete(new Runnable() {
    public void run() {
        System.out.println("Completed");
    }
}).subscribe();
```

alternatively, even simpler by using Java 8 Lambdas:

``` java
Flux.just("Ben", "Michael", "Mark")
        .doOnNext(s -> System.out.println("Hello " + s + "!"))
        .doOnComplete(() -> System.out.println("Completed"))
        .subscribe();
```

You can control the elements that are processed by your `Subscriber`
using operators. The `take()` operator limits the number of emitted
items if you are interested in the first `N` elements only.

``` java
Flux.just("Ben", "Michael", "Mark") //
        .doOnNext(s -> System.out.println("Hello " + s + "!"))
        .doOnComplete(() -> System.out.println("Completed"))
        .take(2)
        .subscribe();
```

The example prints the following lines:

    Hello Ben
    Hello Michael
    Completed

Note that the `take` operator implicitly cancels its subscription from
the `Publisher<T>` once the expected count of elements was emitted.

A subscription to a `Publisher<T>` can be done either by another `Flux`
or a `Subscriber`. Unless you are implementing a custom `Publisher`,
always use `Subscriber`. The used subscriber `Consumer` from the example
above does not handle `Exception`s so once an `Exception` is thrown you
will see a stack trace like this:

    Exception in thread "main" reactor.core.Exceptions$BubblingException: java.lang.RuntimeException: Example exception
        at reactor.core.Exceptions.bubble(Exceptions.java:96)
        at reactor.core.publisher.Operators.onErrorDropped(Operators.java:296)
        at reactor.core.publisher.LambdaSubscriber.onError(LambdaSubscriber.java:117)
        ...
    Caused by: java.lang.RuntimeException: Example exception
        at demos.lambda$example3Lambda$4(demos.java:87)
        at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:157)
        ... 23 more

It is always recommended to implement an error handler right from the
beginning. At a certain point, things can and will go wrong.

A fully implemented subscriber declares the `onCompleted` and `onError`
methods allowing you to react to these events:

``` java
Flux.just("Ben", "Michael", "Mark").subscribe(new Subscriber<String>() {
    public void onSubscribe(Subscription s) {
        s.request(3);
    }

    public void onNext(String s) {
        System.out.println("Hello " + s + "!");
    }

    public void onError(Throwable t) {
        System.out.println("onError: " + t);
    }

    public void onComplete() {
        System.out.println("Completed");
    }
});
```

### From push to pull

The examples from above illustrated how publishers can be set up in a
not-opinionated style about blocking or non-blocking execution. A
`Flux<T>` can be converted explicitly into an `Iterable<T>` or
synchronized with `block()`. Avoid calling `block()` in your code as you
start expressing the nature of execution inside your code. Calling
`block()` removes all non-blocking advantages of the reactive chain to
your application.

``` java
String last = Flux.just("Ben", "Michael", "Mark").last().block();
System.out.println(last);
```

The example prints the following line:

    Mark

A blocking call can be used to synchronize the publisher chain and find
back a way into the plain and well-known `Pull` pattern.

``` java
List<String> list = Flux.just("Ben", "Michael", "Mark").collectList().block();
System.out.println(list);
```

The `toList` operator collects all emitted elements and passes the list
through the `BlockingPublisher<T>`.

The example prints the following line:

    [Ben, Michael, Mark]

### Creating `Flux` and `Mono` using Lettuce

There are many ways to establish publishers. You have already seen
`just()`, `take()` and `collectList()`. Refer to the [Project Reactor
documentation](http://projectreactor.io/docs/) for many more methods
that you can use to create `Flux` and `Mono`.

Lettuce publishers can be used for initial and chaining operations. When
using Lettuce publishers, you will notice the non-blocking behavior.
This is because all I/O and command processing are handled
asynchronously using the netty EventLoop.

Connecting to Redis is insanely simple:

``` java
RedisClient client = RedisClient.create("redis://localhost");
RedisStringReactiveCommands<String, String> commands = client.connect().reactive();
```

In the next step, obtaining a value from a key requires the `GET`
operation:

``` java
commands.get("key").subscribe(new Consumer<String>() {

    public void accept(String value) {
        System.out.println(value);
    }
});
```

Alternatively, written in Java 8 lambdas:

``` java
commands
   .get("key")
   .subscribe(value -> System.out.println(value));
```

The execution is handled asynchronously, and the invoking Thread can be
used to processed in processing while the operation is completed on the
Netty EventLoop threads. Due to its decoupled nature, the calling method
can be left before the execution of the `Publisher<T>` is finished.

Lettuce publishers can be used within the context of chaining to load
multiple keys asynchronously:

``` java
Flux.just("Ben", "Michael", "Mark").
        flatMap(key -> commands.get(key)).
        subscribe(value -> System.out.println("Got value: " + value));
```

### Hot and Cold Publishers

There is a distinction between Publishers that was not covered yet:

- A cold Publishers waits for a subscription until it emits values and
  does this freshly for every subscriber.

- A hot Publishers begins emitting values upfront and presents them to
  every subscriber subsequently.

All Publishers returned from the Redis Standalone, Redis Cluster, and
Redis Sentinel API are cold, meaning that no I/O happens until they are
subscribed to. As such a subscriber is guaranteed to see the whole
sequence from the beginning. So just creating a Publisher will not cause
any network I/O thus creating and discarding Publishers is cheap.
Publishers created for a Publish/Subscribe emit `PatternMessage`s and
`ChannelMessage`s once they are subscribed to. Publishers guarantee
however to emit all items from the beginning until their end. While this
is true for Publish/Subscribe publishers, the nature of subscribing to a
Channel/Pattern allows missed messages due to its subscription nature
and less to the Hot/Cold distinction of publishers.

### Transforming publishers

Publishers can transform the emitted values in various ways. One of the
most basic transformations is `flatMap()` which you have seen from the
examples above that converts the incoming value into a different one.
Another one is `map()`. The difference between `map()` and `flatMap()`
is that `flatMap()` allows you to do those transformations with
`Publisher<T>` calls.

``` java
Flux.just("Ben", "Michael", "Mark")
        .flatMap(commands::get)
        .flatMap(value -> commands.rpush("result", value))
        .subscribe();
```

The first `flatMap()` function is used to retrieve a value and the
second `flatMap()` function appends the value to a Redis list named
`result`. The `flatMap()` function returns a Publisher whereas the
normal map just returns `<T>`. You will use `flatMap()` a lot when
dealing with flows like this, you’ll become good friends.

An aggregation of values can be achieved using the `reduce()`
transformation. It applies a function to each value emitted by a
`Publisher<T>`, sequentially and emits each successive value. We can use
it to aggregate values, to count the number of elements in multiple
Redis sets:

``` java
Flux.just("Ben", "Michael", "Mark")
        .flatMap(commands::scard)
        .reduce((sum, current) -> sum + current)
        .subscribe(result -> System.out.println("Number of elements in sets: " + result));
```

The aggregation function of `reduce()` is applied on each emitted value,
so three times in the example above. If you want to get the last value,
which denotes the final result containing the number of elements in all
Redis sets, apply the `last()` transformation:

``` java
Flux.just("Ben", "Michael", "Mark")
        .flatMap(commands::scard)
        .reduce((sum, current) -> sum + current)
        .last()
        .subscribe(result -> System.out.println("Number of elements in sets: " + result));
```

Now let’s take a look at grouping emitted items. The following example
emits three items and groups them by the beginning character.

``` java
Flux.just("Ben", "Michael", "Mark")
    .groupBy(key -> key.substring(0, 1))
    .subscribe(
        groupedFlux -> {
            groupedFlux.collectList().subscribe(list -> {
                System.out.println("First character: " + groupedFlux.key() + ", elements: " + list);
            });
        }
);
```

The example prints the following lines:

    First character: B, elements: [Ben]
    First character: M, elements: [Michael, Mark]

### Absent values

The presence and absence of values is an essential part of reactive
programming. Traditional approaches consider `null` as an absence of a
particular value. With Java 8, `Optional<T>` was introduced to
encapsulate nullability. Reactive Streams prohibits the use of `null`
values.

In the scope of Redis, an absent value is an empty list, a non-existent
key or any other empty data structure. Reactive programming discourages
the use of `null` as value. The reactive answer to absent values is just
not emitting any value that is possible due the `0` to `N` nature of
`Publisher<T>`.

Suppose we have the keys `Ben` and `Michael` set each to the value
`value`. We query those and another, absent key with the following code:

``` java
Flux.just("Ben", "Michael", "Mark")
        .flatMap(commands::get)
        .doOnNext(value -> System.out.println(value))
        .subscribe();
```

The example prints the following lines:

    value
    value

The output is just two values. The `GET` to the absent key `Mark` does
not emit a value.

The reactive API provides operators to work with empty results when you
require a value. You can use one of the following operators:

- `defaultIfEmpty`: Emit a default value if the `Publisher<T>` did not
  emit any value at all

- `switchIfEmpty`: Switch to a fallback `Publisher<T>` to emit values

- `Flux.hasElements`/`Flux.hasElement`: Emit a `Mono<Boolean>` that
  contains a flag whether the original `Publisher<T>` is empty

- `next`/`last`/`elementAt`: Positional operators to retrieve the
  first/last/`N`th element or emit a default value

### Filtering items

The values emitted by a `Publisher<T>` can be filtered in case you need
only specific results. Filtering does not change the emitted values
itself. Filters affect how many items and at which point (and if at all)
they are emitted.

``` java
Flux.just("Ben", "Michael", "Mark")
        .filter(s -> s.startsWith("M"))
        .flatMap(commands::get)
        .subscribe(value -> System.out.println("Got value: " + value));
```

The code will fetch only the keys `Michael` and `Mark` but not `Ben`.
The filter criteria are whether the `key` starts with a `M`.

You already met the `last()` filter to retrieve the last value:

``` java
Flux.just("Ben", "Michael", "Mark")
        .last()
        .subscribe(value -> System.out.println("Got value: " + value));
```

the extended variant of `last()` allows you to take the last `N` values:

``` java
Flux.just("Ben", "Michael", "Mark")
        .takeLast(3)
        .subscribe(value -> System.out.println("Got value: " + value));
```

The example from above takes the last `2` values.

The opposite to `next()` is the `first()` filter that is used to
retrieve the next value:

``` java
Flux.just("Ben", "Michael", "Mark")
        .next()
        .subscribe(value -> System.out.println("Got value: " + value));
```

### Error handling

Error handling is an indispensable component of every real world
application and should to be considered from the beginning on. Project
Reactor provides several mechanisms to deal with errors.

In general, you want to react in the following ways:

- Return a default value instead

- Use a backup publisher

- Retry the Publisher (immediately or with delay)

The following code falls back to a default value after it throws an
exception at the first emitted item:

``` java
Flux.just("Ben", "Michael", "Mark")
        .doOnNext(value -> {
            throw new IllegalStateException("Takes way too long");
        })
        .onErrorReturn("Default value")
        .subscribe();
```

You can use a backup `Publisher<T>` which will be called if the first
one fails.

``` java
Flux.just("Ben", "Michael", "Mark")
        .doOnNext(value -> {
            throw new IllegalStateException("Takes way too long");
        })
        .switchOnError(commands.get("Default Key"))
        .subscribe();
```

It is possible to retry the publisher by re-subscribing. Re-subscribing
can be done as soon as possible, or with a wait interval, which is
preferred when external resources are involved.

``` java
Flux.just("Ben", "Michael", "Mark")
        .flatMap(commands::get)
        .retry()
        .subscribe();
```

Use the following code if you want to retry with backoff:

``` java
Flux.just("Ben", "Michael", "Mark")
        .doOnNext(v -> {
            if (new Random().nextInt(10) + 1 == 5) {
                throw new RuntimeException("Boo!");
            }
        })
        .doOnSubscribe(subscription ->
        {
            System.out.println(subscription);
        })
        .retryWhen(throwableFlux -> Flux.range(1, 5)
                .flatMap(i -> {
                    System.out.println(i);
                    return Flux.just(i)
                            .delay(Duration.of(i, ChronoUnit.SECONDS));
                }))
        .blockLast();
```

The attempts get passed into the `retryWhen()` method delayed with the
number of seconds to wait. The delay method is used to complete once its
timer is done.

### Schedulers and threads

Schedulers in Project Reactor are used to instruct multi-threading. Some
operators have variants that take a Scheduler as a parameter. These
instruct the operator to do some or all of its work on a particular
Scheduler.

Project Reactor ships with a set of preconfigured Schedulers, which are
all accessible through the `Schedulers` class:

- Schedulers.parallel(): Executes the computational work such as
  event-loops and callback processing.

- Schedulers.immediate(): Executes the work immediately in the current
  thread

- Schedulers.elastic(): Executes the I/O-bound work such as asynchronous
  performance of blocking I/O, this scheduler is backed by a thread-pool
  that will grow as needed

- Schedulers.newSingle(): Executes the work on a new thread

- Schedulers.fromExecutor(): Create a scheduler from a
  `java.util.concurrent.Executor`

- Schedulers.timer(): Create or reuse a hash-wheel based TimedScheduler
  with a resolution of 50ms.

Do not use the computational scheduler for I/O.

Publishers can be executed by a scheduler in the following different
ways:

- Using an operator that makes use of a scheduler

- Explicitly by passing the Scheduler to such an operator

- By using `subscribeOn(Scheduler)`

- By using `publishOn(Scheduler)`

Operators like `buffer`, `replay`, `skip`, `delay`, `parallel`, and so
forth use a Scheduler by default if not instructed otherwise.

All of the listed operators allow you to pass in a custom scheduler if
needed. Sticking most of the time with the defaults is a good idea.

If you want the subscribe chain to be executed on a specific scheduler,
you use the `subscribeOn()` operator. The code is executed on the main
thread without a scheduler set:

``` java
Flux.just("Ben", "Michael", "Mark").flatMap(key -> {
            System.out.println("Map 1: " + key + " (" + Thread.currentThread().getName() + ")");
            return Flux.just(key);
        }
).flatMap(value -> {
            System.out.println("Map 2: " + value + " (" + Thread.currentThread().getName() + ")");
            return Flux.just(value);
        }
).subscribe();
```

The example prints the following lines:

    Map 1: Ben (main)
    Map 2: Ben (main)
    Map 1: Michael (main)
    Map 2: Michael (main)
    Map 1: Mark (main)
    Map 2: Mark (main)

This example shows the `subscribeOn()` method added to the flow (it does
not matter where you add it):

``` java
Flux.just("Ben", "Michael", "Mark").flatMap(key -> {
            System.out.println("Map 1: " + key + " (" + Thread.currentThread().getName() + ")");
            return Flux.just(key);
        }
).flatMap(value -> {
            System.out.println("Map 2: " + value + " (" + Thread.currentThread().getName() + ")");
            return Flux.just(value);
        }
).subscribeOn(Schedulers.parallel()).subscribe();
```

The output of the example shows the effect of `subscribeOn()`. You can
see that the Publisher is executed on the same thread, but on the
computation thread pool:

    Map 1: Ben (parallel-1)
    Map 2: Ben (parallel-1)
    Map 1: Michael (parallel-1)
    Map 2: Michael (parallel-1)
    Map 1: Mark (parallel-1)
    Map 2: Mark (parallel-1)

If you apply the same code to Lettuce, you will notice a difference in
the threads on which the second `flatMap()` is executed:

``` java
Flux.just("Ben", "Michael", "Mark").flatMap(key -> {
    System.out.println("Map 1: " + key + " (" + Thread.currentThread().getName() + ")");
    return commands.set(key, key);
}).flatMap(value -> {
    System.out.println("Map 2: " + value + " (" + Thread.currentThread().getName() + ")");
    return Flux.just(value);
}).subscribeOn(Schedulers.parallel()).subscribe();
```

The example prints the following lines:

    Map 1: Ben (parallel-1)
    Map 1: Michael (parallel-1)
    Map 1: Mark (parallel-1)
    Map 2: OK (lettuce-nioEventLoop-3-1)
    Map 2: OK (lettuce-nioEventLoop-3-1)
    Map 2: OK (lettuce-nioEventLoop-3-1)

Two things differ from the standalone examples:

1.  The values are set rather concurrently than sequentially

2.  The second `flatMap()` transformation prints the netty EventLoop
    thread name

This is because Lettuce publishers are executed and completed on the
netty EventLoop threads by default.

`publishOn` instructs an Publisher to call its observer’s `onNext`,
`onError`, and `onCompleted` methods on a particular Scheduler. Here,
the order matters:

``` java
Flux.just("Ben", "Michael", "Mark").flatMap(key -> {
    System.out.println("Map 1: " + key + " (" + Thread.currentThread().getName() + ")");
    return commands.set(key, key);
}).publishOn(Schedulers.parallel()).flatMap(value -> {
    System.out.println("Map 2: " + value + " (" + Thread.currentThread().getName() + ")");
    return Flux.just(value);
}).subscribe();
```

Everything before the `publishOn()` call is executed in main, everything
below in the scheduler:

    Map 1: Ben (main)
    Map 1: Michael (main)
    Map 1: Mark (main)
    Map 2: OK (parallel-1)
    Map 2: OK (parallel-1)
    Map 2: OK (parallel-1)

Schedulers allow direct scheduling of operations. Refer to the [Project
Reactor
documentation](https://projectreactor.io/core/docs/api/reactor/core/scheduler/Schedulers.html)
for further information.

### Redis Transactions

Lettuce provides a convenient way to use Redis Transactions in a
reactive way. Commands that should be executed within a transaction can
be executed after the `MULTI` command was executed. Functional chaining
allows to execute commands within a closure, and each command receives
its appropriate response. A cumulative response is also returned with
`TransactionResult` in response to `EXEC`.

See [Transactions](#transactions-using-the-reactive-api) for
further details.

#### Other examples

**Blocking example**

``` java
RedisStringReactiveCommands<String, String> reactive = client.connect().reactive();
Mono<String> set = reactive.set("key", "value");
set.block();
```

**Non-blocking example**

``` java
RedisStringReactiveCommands<String, String> reactive = client.connect().reactive();
Mono<String> set = reactive.set("key", "value");
set.subscribe();
```

**Functional chaining**

``` java
RedisStringReactiveCommands<String, String> reactive = client.connect().reactive();
Flux.just("Ben", "Michael", "Mark")
        .flatMap(key -> commands.sadd("seen", key))
        .flatMap(value -> commands.randomkey())
        .flatMap(commands::type)
        .doOnNext(System.out::println).subscribe();
```

**Redis Transaction**

    RedisReactiveCommands<String, String> reactive = client.connect().reactive();

    reactive.multi().doOnSuccess(s -> {
        reactive.set("key", "1").doOnNext(s1 -> System.out.println(s1)).subscribe();
        reactive.incr("key").doOnNext(s1 -> System.out.println(s1)).subscribe();
    }).flatMap(s -> reactive.exec())
            .doOnNext(transactionResults -> System.out.println(transactionResults.wasRolledBack()))
            .subscribe();

## Kotlin API

Kotlin Coroutines are using Kotlin lightweight threads allowing to write
non-blocking code in an imperative way. On language side, suspending
functions provides an abstraction for asynchronous operations while on
library side kotlinx.coroutines provides functions like `async { }` and
types like `Flow`.

Lettuce ships with extensions to provide support for idiomatic Kotlin
use.

### Dependencies

Coroutines support is available when `kotlinx-coroutines-core` and
`kotlinx-coroutines-reactive` dependencies are on the classpath:

``` xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
    <version>${kotlinx-coroutines.version}</version>
</dependency>
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-reactive</artifactId>
    <version>${kotlinx-coroutines.version}</version>
</dependency>
```

### How does Reactive translate to Coroutines?

`Flow` is an equivalent to `Flux` in Coroutines world, suitable for hot
or cold streams, finite or infinite streams, with the following main
differences:

- `Flow` is push-based while `Flux` is a push-pull hybrid

- Backpressure is implemented via suspending functions

- `Flow` has only a single suspending collect method and operators are
  implemented as extensions

- Operators are easy to implement thanks to Coroutines

- Extensions allow to add custom operators to Flow

- Collect operations are suspending functions

- `map` operator supports asynchronous operations (no need for
  `flatMap`) since it takes a suspending function parameter

### Coroutines API based on reactive operations

Example for retrieving commands and using it:

``` kotlin
val api: RedisCoroutinesCommands<String, String> = connection.coroutines()

val foo1 = api.set("foo", "bar")
val foo2 = api.keys("fo*")
```

> [!NOTE]
> Coroutine Extensions are experimental and require opt-in using
> `@ExperimentalLettuceCoroutinesApi`. The API ships with a reduced
> feature set. Deprecated methods and `StreamingChannel` are left out
> intentionally. Expect evolution towards a `Flow`-based API to consume
> large Redis responses.

### Extensions for existing APIs

#### Transactions DSL

Example for the synchronous API:

``` kotlin
val result: TransactionResult = connection.sync().multi {
    set("foo", "bar")
    get("foo")
}
```

Example for async with coroutines:

``` kotlin
val result: TransactionResult = connection.async().multi {
    set("foo", "bar")
    get("foo")
}
```

## Publish/Subscribe

Lettuce provides support for Publish/Subscribe on Redis Standalone and
Redis Cluster connections. The connection is notified on
message/subscribed/unsubscribed events after subscribing to channels or
patterns. [Synchronous](#basic-usage), [asynchronous](#asynchronous-api)
and [reactive](#reactive-api) API’s are provided to interact with Redis
Publish/Subscribe features.

### Subscribing

A connection can notify multiple listeners that implement
`RedisPubSubListener` (Lettuce provides a `RedisPubSubAdapter` for
convenience). All listener registrations are kept within the
`StatefulRedisPubSubConnection`/`StatefulRedisClusterConnection`.

``` java
StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub()
connection.addListener(new RedisPubSubListener<String, String>() { ... })

RedisPubSubCommands<String, String> sync = connection.sync();
sync.subscribe("channel");

// application flow continues
```

> [!NOTE]
> Don’t issue blocking calls (includes synchronous API calls to Lettuce)
> from inside of Pub/Sub callbacks as this would block the EventLoop. If
> you need to fetch data from Redis from inside a callback, please use
> the asynchronous API.

``` java
StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub()
connection.addListener(new RedisPubSubListener<String, String>() { ... })

RedisPubSubAsyncCommands<String, String> async = connection.async();
RedisFuture<Void> future = async.subscribe("channel");

// application flow continues
```

### Reactive API

The reactive API provides hot `Observable`s to listen on
`ChannelMessage`s and `PatternMessage`s. The `Observable`s receive all
inbound messages. You can do filtering using the observable chain if you
need to filter out the interesting ones, The `Observable` stops
triggering events when the subscriber unsubscribes from it.

``` java
StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub()

RedisPubSubReactiveCommands<String, String> reactive = connection.reactive();
reactive.subscribe("channel").subscribe();

reactive.observeChannels().doOnNext(patternMessage -> {...}).subscribe()

// application flow continues
```

### Redis Cluster

Redis Cluster support Publish/Subscribe but requires some attention in
general. User-space Pub/Sub messages (Calling `PUBLISH`) are broadcasted
across the whole cluster regardless of subscriptions to particular
channels/patterns. This behavior allows connecting to an arbitrary
cluster node and registering a subscription. The client isn’t required
to connect to the node where messages were published.

A cluster-aware Pub/Sub connection is provided by
`RedisClusterClient.connectPubSub()` allowing to listen for cluster
reconfiguration and reconnect if the topology changes.

``` java
StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub()
connection.addListener(new RedisPubSubListener<String, String>() { ... })

RedisPubSubCommands<String, String> sync = connection.sync();
sync.subscribe("channel");
```

Redis Cluster also makes a distinction between user-space and key-space
messages. Key-space notifications (Pub/Sub messages for key-activity)
stay node-local and are not broadcasted across the Redis Cluster. A
notification about, e.g. an expiring key, stays local to the node on
which the key expired.

Clients that are interested in keyspace notifications must subscribe to
the appropriate node (or nodes) to receive these notifications. You can
either use `RedisClient.connectPubSub()` to establish Pub/Sub
connections to the individual nodes or use `RedisClusterClient`'s
message propagation and NodeSelection API to get a managed set of
connections.

``` java
StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub()
connection.addListener(new RedisClusterPubSubListener<String, String>() { ... })
connection.setNodeMessagePropagation(true);

RedisPubSubCommands<String, String> sync = connection.sync();
sync.masters().commands().subscribe("__keyspace@0__:*");
```

There are two things to pay special attention to:

1.  Replication: Keys replicated to replica nodes, especially
    considering expiry, generate keyspace events on all nodes holding
    the key. If a key expires and it is replicated, it will expire on
    the master and all replicas. Each Redis server will emit keyspace
    events. Subscribing to non-master nodes, therefore, will let your
    application see multiple events of the same type for the same key
    because of Redis distributed nature.

2.  Topology Changes: Subscriptions are issued either by using the
    NodeSelection API or by calling `subscribe(…)` on the individual
    cluster node connections. Subscription registrations are not
    propagated to new nodes that are added on a topology change.

## Transactions/Multi

Transactions allow the execution of a group of commands in a single
step. Transactions can be controlled using `WATCH`, `UNWATCH`, `EXEC`,
`MULTI` and `DISCARD` commands. Synchronous, asynchronous, and reactive
APIs allow the use of transactions.

> [!NOTE]
> Transactional use requires external synchronization when a single
> connection is used by multiple threads/processes. This can be achieved
> either by serializing transactions or by providing a dedicated
> connection to each concurrent process. Lettuce itself does not
> synchronize transactional/non-transactional invocations regardless of
> the used API facade.

Redis responds to commands invoked during a transaction with a `QUEUED`
response. The response related to the execution of the command is
received at the moment the `EXEC` command is processed, and the
transaction is executed. The particular APIs behave in different ways:

- Synchronous: Invocations to the commands return `null` while they are
  invoked within a transaction. The `MULTI` command carries the response
  of the particular commands.

- Asynchronous: The futures receive their response at the moment the
  `EXEC` command is processed. This happens while the `EXEC` response is
  received.

- Reactive: An `Obvervable<T>` triggers `onNext`/`onCompleted` at the
  moment the `EXEC` command is processed. This happens while the `EXEC`
  response is received.

As soon as you’re within a transaction, you won’t receive any responses
on triggering the commands

``` java
redis.multi() == "OK"
redis.set(key, value) == null
redis.exec() == list("OK")
```

You’ll receive the transactional response when calling `exec()` on the
end of your transaction.

``` java
redis.multi() == "OK"
redis.set(key1, value) == null
redis.set(key2, value) == null
redis.exec() == list("OK", "OK")
```

### Transactions using the asynchronous API

Asynchronous use of Redis transactions is very similar to
non-transactional use. The asynchronous API returns `RedisFuture`
instances that eventually complete and they are handles to a future
result. Regular commands complete as soon as Redis sends a response.
Transactional commands complete as soon as the `EXEC` result is
received.

Each command is completed individually with its own result so users of
`RedisFuture` will see no difference between transactional and
non-transactional `RedisFuture` completion. That said, transactional
command results are available twice: Once via `RedisFuture` of the
command and once through `List<Object>` (`TransactionResult` since
Lettuce 5) of the `EXEC` command future.

``` java
RedisAsyncCommands<String, String> async = client.connect().async();

RedisFuture<String> multi = async.multi();

RedisFuture<String> set = async.set("key", "value");

RedisFuture<List<Object>> exec = async.exec();

List<Object> objects = exec.get();
String setResult = set.get();

objects.get(0) == setResult
```

### Transactions using the reactive API

The reactive API can be used to execute multiple commands in a single
step. The nature of the reactive API encourages nesting of commands. It
is essential to understand the time at which an `Observable<T>` emits a
value when working with transactions. Redis responds with `QUEUED` to
commands invoked during a transaction. The response related to the
execution of the command is received at the moment the `EXEC` command is
processed, and the transaction is executed. Subsequent calls in the
processing chain are executed after the transactional end. The following
code starts a transaction, executes two commands within the transaction
and finally executes the transaction.

``` java
RedisReactiveCommands<String, String> reactive = client.connect().reactive();
reactive.multi().subscribe(multiResponse -> {
    reactive.set("key", "1").subscribe();
    reactive.incr("key").subscribe();
    reactive.exec().subscribe();
});
```

### Transactions on clustered connections

Clustered connections perform a routing by default. This means, that you
can’t be really sure, on which host your command is executed. So if you
are working in a clustered environment, use rather a regular connection
to your node, since then you’ll bound to that node knowing which hash
slots are handled by it.

### Examples

**Multi with executing multiple commands**

``` java
redis.multi();

redis.set("one", "1");
redis.set("two", "2");
redis.mget("one", "two");
redis.llen(key);

redis.exec(); // result: list("OK", "OK", list("1", "2"), 0L)
```

**Mult executing multiple asynchronous commands**

``` java
redis.multi();

RedisFuture<String> set1 = redis.set("one", "1");
RedisFuture<String> set2 = redis.set("two", "2");
RedisFuture<String> mget = redis.mget("one", "two");
RedisFuture<Long> llen = mgetredis.llen(key);


set1.thenAccept(value -> …); // OK
set2.thenAccept(value -> …); // OK

RedisFuture<List<Object>> exec = redis.exec(); // result: list("OK", "OK", list("1", "2"), 0L)

mget.get(); // list("1", "2")
llen.thenAccept(value -> …); // 0L
```

**Using WATCH**

``` java
redis.watch(key);

RedisConnection<String, String> redis2 = client.connect();
redis2.set(key, value + "X");
redis2.close();

redis.multi();
redis.append(key, "foo");
redis.exec(); // result is an empty list because of the changed key
```

## Scripting and Functions

Redis functionality can be extended through many ways, of which [Lua
Scripting](https://redis.io/topics/eval-intro) and
[Functions](https://redis.io/topics/functions-intro) are two approaches
that do not require specific pre-requisites on the server.

### Lua Scripting

[Lua](https://redis.io/topics/lua-api) is a powerful scripting language
that is supported at the core of Redis. Lua scripts can be invoked
dynamically by providing the script contents to Redis or used as stored
procedure by loading the script into Redis and using its digest to
invoke it.

<div class="informalexample">

``` java
String helloWorld = redis.eval("return ARGV[1]", STATUS, new String[0], "Hello World");
```

</div>

Using Lua scripts is straightforward. Consuming results in Java requires
additional details to consume the result through a matching type. As we
do not know what your script will return, the API uses call-site
generics for you to specify the result type. Additionally, you must
provide a `ScriptOutputType` hint to `EVAL` so that the driver uses the
appropriate output parser. See [Output Formats](#output-formats) for
further details.

Lua scripts can be stored on the server for repeated execution.
Dynamically-generated scripts are an anti-pattern as each script is
stored in Redis' script cache. Generating scripts during the application
runtime may, and probably will, exhaust the host’s memory resources for
caching them. Instead, scripts should be as generic as possible and
provide customized execution via their arguments. You can register a
script through `SCRIPT LOAD` and use its SHA digest to invoke it later:

<div class="informalexample">

``` java
String digest = redis.scriptLoad("return ARGV[1]", STATUS, new String[0], "Hello World");

// later
String helloWorld = redis.evalsha(digest, STATUS, new String[0], "Hello World");
```

</div>

### Redis Functions

[Redis Functions](https://redis.io/topics/functions-intro) is an
evolution of the scripting API to provide extensibility beyond Lua.
Functions can leverage different engines and follow a model where a
function library registers functionality to be invoked later with the
`FCALL` command.

<div class="informalexample">

``` java
redis.functionLoad("FUNCTION LOAD "#!lua name=mylib\nredis.register_function('knockknock', function() return 'Who\\'s there?' end)");

String response = redis.fcall("knockknock", STATUS);
```

</div>

Using Functions is straightforward. Consuming results in Java requires
additional details to consume the result through a matching type. As we
do not know what your function will return, the API uses call-site
generics for you to specify the result type. Additionally, you must
provide a `ScriptOutputType` hint to `EVAL` so that the driver uses the
appropriate output parser. See [Output Formats](#output-formats) for
further details.

### Output Formats

You can choose from one of the following:

- `BOOLEAN`: Boolean output, expects a number `0` or `1` to be converted
  to a boolean value.

- `INTEGER`: 64-bit Integer output, represented as Java `Long`.

- `MULTI`: List of flat arrays.

- `STATUS`: Simple status value such as `OK`. The Redis response is
  parsed as ASCII.

- `VALUE`: Value return type decoded through `RedisCodec`.

- `OBJECT`: RESP3-defined object output supporting all Redis response
  structures.

### Leveraging Scripting and Functions through Command Interfaces

Using dynamic functionality without a documented response structure can
impose quite some complexity on your application. If you consider using
scripting or functions, then you can use [Command
Interfaces](Working-with-dynamic-Redis-Command-Interfaces.md) to declare
an interface along with methods that represent your scripting or
function landscape. Declaring a method with input arguments and a
response type not only makes it obvious how the script or function is
supposed to be called, but also how the response structure should look
like.

Let’s take a look at a simple function call first:

<div class="informalexample">

``` lua
local function my_hlastmodified(keys, args)
  local hash = keys[1]
  return redis.call('HGET', hash, '_last_modified_')
end
```

</div>

<div class="informalexample">

``` java
Long lastModified = redis.fcall("my_hlastmodified", INTEGER, "my_hash");
```

</div>

This example calls the `my_hlastmodified` function expecting some `Long`
response an input argument. Calling a function from a single place in
your code isn’t an issue on its own. The arrangement becomes problematic
once the number of functions grows or you start calling the functions
with different arguments from various places in your code. Without the
function code, it becomes impossible to investigate how the response
mechanics work or determine the argument semantics, as there is no
single place to document the function behavior.

Let’s apply the Command Interface pattern to see how the the declaration
and call sites change:

<div class="informalexample">

``` java
interface MyCustomCommands extends Commands {

    /**
     * Retrieve the last modified value from the hash key.
     * @param hashKey the key of the hash.
     * @return the last modified timestamp, can be {@code null}.
     */
    @Command("FCALL my_hlastmodified 1 :hashKey")
    Long getLastModified(@Param("my_hash") String hashKey);

}

MyCustomCommands myCommands = …;
Long lastModified = myCommands.getLastModified("my_hash");
```

</div>

By declaring a command method, you create a place that allows for
storing additional documentation. The method declaration makes clear
what the function call expects and what you get in return.

