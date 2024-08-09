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

See [Transactions](transactions-multi.md#transactions-using-the-reactive-api) for
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

``` java
    RedisReactiveCommands<String, String> reactive = client.connect().reactive();

    reactive.multi().doOnSuccess(s -> {
        reactive.set("key", "1").doOnNext(s1 -> System.out.println(s1)).subscribe();
        reactive.incr("key").doOnNext(s1 -> System.out.println(s1)).subscribe();
    }).flatMap(s -> reactive.exec())
            .doOnNext(transactionResults -> System.out.println(transactionResults.wasRolledBack()))
            .subscribe();
```