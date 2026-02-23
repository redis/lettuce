# Custom commands

Lettuce covers nearly all Redis commands. Redis development is an
ongoing process and the Redis Module system is intended to introduce new
commands which are not part of the Redis Core. This requirement
introduces the need to invoke custom commands or use custom outputs.
Custom commands can be dispatched on the one hand using Lua and the
`eval()` command, on the other side Lettuce 4.x allows you to trigger
own commands. That API is used by Lettuce itself to dispatch commands
and requires some knowledge of how commands are constructed and
dispatched within Lettuce.

Lettuce provides two levels of command dispatching:

1.  Using the synchronous, asynchronous or reactive API wrappers which
    invoke commands according to their nature

2.  Using the bare connection to influence the command nature and
    synchronization (advanced)

**Example using `dispatch()` on the synchronous API**

``` java
RedisCodec<String, String> codec = StringCodec.UTF8;
RedisCommands<String, String> commands = ...

String response = redis.dispatch(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec)
                       .addKey(key)
                       .addValue(value));
```

**Example using `dispatch()` on the asynchronous API**

``` java
RedisCodec<String, String> codec = StringCodec.UTF8;
RedisAsyncCommands<String, String> commands = ...

RedisFuture<String> response = redis.dispatch(CommandType.SET, new StatusOutput<>(codec),
                                    new CommandArgs<>(codec)
                                        .addKey(key)
                                        .addValue(value));
```

**Example using `dispatch()` on the reactive API**

``` java
RedisCodec<String, String> codec = StringCodec.UTF8;
RedisReactiveCommands<String, String> commands = ...

Observable<String> response = redis.dispatch(CommandType.SET, new StatusOutput<>(codec),
                                    new CommandArgs<>(codec)
                                        .addKey(key)
                                        .addValue(value));
```

**Example using a `RedisFuture` command wrapper**

``` java
StatefulRedisConnection<String, String> connection = redis.getStatefulConnection();

RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                                        new StatusOutput<>(StringCodec.UTF8));

AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
connection.dispatch(async);

// async instanceof CompletableFuture == true
```

## Mechanics of Lettuce commands

Lettuce uses the command pattern to implement to execute commands. Every
time a command is invoked, Lettuce creates a command object (`Command`
or types implementing `RedisCommand`). Commands can carry arguments
(`CommandArgs`) and an output (subclasses of `CommandOutput`). Both are
optional. The two mandatory properties are the command type (see
`CommandType` or a type implementing `ProtocolKeyword`) and a
`RedisCodec`. If you dispatch commands by yourself, do not reuse command
instances to dispatch commands more than once. Commands that were
executed once have the completed flag set and cannot be reused.

### Arguments

`CommandArgs` is a container for command arguments that follow the
command keyword (`CommandType`). A `PING` or `QUIT` command do not
require commands whereas the `GET` or `SET` commands require arguments
in the form of keys and values.

**The `PING` command**

``` java
RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                                        new StatusOutput<>(StringCodec.UTF8));
```

**The `SET` command**

``` java
StringCodec codec = StringCodec.UTF8;
RedisCommand<String, String, String> command = new Command<>(CommandType.SET,
                new StatusOutput<>(codec), new CommandArgs<>(codec)
                                                  .addKey("key")
                                                  .addValue("value"));
```

`CommandArgs` allow to add one or more:

- key and arrays of keys

- value and arrays of values

- `String`, `long` (the Redis integer), `double`

- byte array

- `CommandType`, `CommandKeyword` and generic `ProtocolKeyword`

The sequence of args and keywords is not validated by Lettuce beyond the
supported data types, meaning Redis will report errors if the command
syntax is not correct.

### Outputs

Commands producing an output are required to consume the output. Lettuce
supports type-safe conversion of the response into the appropriate
result types. The output handlers derive from the `CommandOutput` base
class. Lettuce provides a wide range of output types (see the
`com.lambdaworks.redis.output` package for details). Command outputs are
mostly used to return the result as the whole object. The response is
available as soon as the whole command output is processed. There are
cases, where you might want to stream the response instead of allocating
a significant amount of memory and return the whole response as one.
These types are called streaming outputs. Following implementations ship
with Lettuce:

- `KeyStreamingOutput`

- `KeyValueScanStreamingOutput`

- `KeyValueStreamingOutput`

- `ScoredValueStreamingOutput`

- `ValueScanStreamingOutput`

- `ValueStreamingOutput`

Those outputs take a streaming channel (see `ValueStreamingChannel`) and
invoke the callback method (e.g. `onValue(V value)`) for every data
element.

Implementing an own output is, in general, a good idea when you want to
support a different data type, or you want to work with different types
than the basic collection, map, String, and primitive types. You might
get an impression of the custom types idea by taking a look on
`GeoWithinListOutput`, which takes a bunch of strings and nested lists
to construct a list of `GeoWithin` instances.

Please note that using an output that does not fit the command output
can jam the response processing and lead to not usable connections. Use
either `ArrayOutput` or `NestedMultiOutput` when in doubt, so you
receive a list of objects (nested lists).

**Output for the `PING` command**

``` java
Command<String, String, String> command = new Command<>(CommandType.PING,
                                        new StatusOutput<>(StringCodec.UTF8));
```

**Output for the `HGETALL` command**

``` java
StringCodec codec = StringCodec.UTF8;
Command<String, String, Map<String, String>> command = new Command<>(CommandType.HGETALL,
                                    new MapOutput<>(codec),
                                    new CommandArgs<>(codec).addKey(key));
```

**Output for the `HKEYS` command**

``` java
StringCodec codec = StringCodec.UTF8;
Command<String, String, List<String>> command = new Command<>(CommandType.HKEYS,
                                    new KeyListOutput<>(codec),
                                    new CommandArgs<>(codec).addKey(key));
```

## Synchronous, asynchronous and reactive

Great, that you made it up to here. You might want to know now, how to
synchronize the command completion, work with `Future`s or how about the
reactive API. The simple way is using the `dispatch(…)` method of the
according wrapper. If this is not sufficient, then continue on reading.

The `dispatch()` method on a stateful Redis connection is not
opinionated at all how you are using Lettuce, whether it is synchronous
or reactive. The only thing this method does is dispatching the command.
The response handler handles decoding the command and completing the
command once it’s done. The asynchronous command processing is the only
operating mode of Lettuce.

The `RedisCommand` interface provides methods to `complete()`,
`cancel()` and `completeExceptionally()` the command. The `complete()`
methods are called by the response handler as soon as the command is
completed. Redis commands can be wrapped and augmented by that way.
Wrapping is used when using transactions (`MULTI`) or Redis Cluster.

You are free to implement your command type or use one of the provided
commands:

- Command (default implementation)

- AsyncCommand (the `CompleteableFuture` wrapper for `RedisCommand`)

- CommandWrapper (generic wrapper)

- TransactionalCommand (wraps `RedisCommand`s when `MULTI` is active)

### Fire & Forget

Fire&Forget is the simple-most way to dispatch commands. You just
trigger it and then you do not care what happens, whether the command
completes or not, and you don’t have access to the command output:

``` java
StatefulRedisConnection<String, String> connection = redis.getStatefulConnection();

connection.dispatch(CommandType.PING, VoidOutput.create());
```

!!! NOTE
    `VoidOutput.create()` swallows also Redis error responses. If you want
    to just avoid response decoding, create a `VoidCodec` instance using
    its constructor to retain error response decoding.

### Asynchronous

The asynchronous API works in general with the `AsyncCommand` wrapper
that extends `CompleteableFuture`. `AsyncCommand` can be synchronized by
`await()` or `get()` which corresponds with the asynchronous pull style.
By using the methods from the `CompletionStage` interface (such as
`handle()` or `thenAccept()`) the response handler will trigger the
functions ("listeners") on command completion. Learn more about
asynchronous usage in the [Asynchronous API](user-guide/async-api.md) topic.

``` java
StatefulRedisConnection<String, String> connection = redis.getStatefulConnection();

RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                                        new StatusOutput<>(StringCodec.UTF8));

AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
connection.dispatch(async);

// async instanceof CompletableFuture == true
```

### Synchronous

The synchronous API of Lettuce uses future synchronization to provide a
synchronous view.

### Reactive

Reactive commands are dispatched at the moment of subscription (see
[Reactive API](user-guide/reactive-api.md) for more details on reactive APIs). In the
context of Lettuce this means, you need to start before calling the
`dispatch()` method. The reactive API uses internally an
`ObservableCommand`, but that is internal stuff. If you want to dispatch
commands the reactive way, you’ll need to wrap commands (or better:
command supplier to be able to retry commands) with the
`ReactiveCommandDispatcher`. The dispatcher implements the `OnSubscribe`
API to create an `Observable<T>`, handles command dispatching at the
time of subscription and can dissolve collection types to particular
elements. An instance of `ReactiveCommandDispatcher` allows creating
multiple `Observable`s as long as you use a `Supplier<RedisCommand>`.
Commands that were executed once have the completed flag set and cannot
be reused.

``` java
StatefulRedisConnection<String, String> connection = redis.getStatefulConnection();

RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                new StatusOutput<>(StringCodec.UTF8));
ReactiveCommandDispatcher<String, String, String> dispatcher = new ReactiveCommandDispatcher<>(command,
                connection, false);

Observable<String> observable = Observable.create(dispatcher);
String result = observable.toBlocking().first();

result == "PONG"
```
