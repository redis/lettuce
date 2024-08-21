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
known as [Pipelining](https://redis.io/docs/latest/develop/use/pipelining/). The following
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
see the [Java 8 API docs](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)
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
`CompleteableFuture<T>`. See the [Reactive API](reactive-api.md) for
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