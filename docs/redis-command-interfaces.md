# Working with dynamic Redis Command Interfaces

The Redis Command Interface abstraction provides a dynamic way for
typesafe Redis command invocation. It allows you to declare an interface
with command methods to significantly reduce boilerplate code required
to invoke a Redis command.

## Introduction

Redis is a data store supporting over 190 documented commands and over
450 command permutations. The community supports actively Redis
development; each major Redis release comes with new commands. Command
growth and keeping track with upcoming modules are challenging for
client developers and Redis user as there is no full command coverage
for each module in a single Redis client.

The central interface in Lettuce Command Interface abstraction is
`Commands`. This interface acts primarily as a marker interface to help
you to discover interfaces that extend this one. The `KeyCommands`
interface below declares some command methods.

``` java
public interface KeyCommands extends Commands {

    String get(String key);                  

    String set(String key, String value);    

    String set(String key, byte[] value);    
}
```

- Retrieves a key by its name.

- Sets a key and value.

- Sets a key and a value by using bytes.

The interface from above declares several methods. Let’s take a brief
look at `String set(String key, String value)`. We can derive from that
declaration certain things:

- It should be executed synchronously – there’s no
  [asynchronous](#asynchronous-future-execution) or
  [reactive](#reactive-execution) wrapper declared in the result type.

- The Redis command method returns a `String` - that reveals something
  regarding the command result expectation. This command expects a reply
  that can be represented as `String`.

- The method is named `set` so the derived command will be named `set`.

- There are two parameters defined: `String key` and `String value`.
  Although Redis does not take any other parameter types than bulk
  strings, we still can apply a transformation to the parameters – we
  can conclude their serialization from the declared type.

The `set` command from above called would look like:

    commands.set("key", "value");

This command translates to:

    SET key value

## Command methods

With Lettuce, declaring command methods becomes a four-step process:

1.  Declare an interface extending `Commands`.

    ``` java
    interface KeyCommands extends Commands { … }
    ```

2.  Declare command methods on the interface.

    ``` java
    interface KeyCommands extends Commands {
        String get(String key);
    }
    ```

3.  Set up Lettuce to create proxy instances for those interfaces.

    ``` java
    RedisClient client = …
    RedisCommandFactory factory = new RedisCommandFactory(client.connect());
    ```

4.  Get the commands instance and use it.

    ``` java
    public class SomeClient {

        KeyCommands commands;

        public SomeClient(RedisCommandFactory factory) {
            commands = factory.getCommands(KeyCommands.class);
        }

        public void doSomething() {
            String value = commands.get("Walter");
        }
    }
    ```

The sections that follow explain each step in detail.

## Defining command methods

As a first step, you define a specific command interface. The interface
must extend `Commands`.

Command methods are declared inside the commands interface like regular
methods (probably not that much of a surprise). Lettuce derives commands
(name, arguments, and response) from each declared method.

### Command naming

The commands proxy has two ways to derive a Redis command from the
method name. It can derive the command name from the method name
directly, or by using a manually defined `@Command` annotation. However,
there’s got to be a strategy that decides what actual command is
created. Let’s have a look at the available options.

``` java
public interface MixedCommands extends Commands {

    List<String> mget(String... keys);                    

    @Command("MGET")
    List<Value<String> mgetAsValues(String... keys);      

    @CommandNaming(strategy = DOT)
    double nrRun(String key, int... indexes)              
}
```

- Plain command method. Lettuce will derive to the `MGET` command.

- Command method annotated with `@Command`. Lettuce will execute `MGET`
  since annotations have a higher precedence than method-based name
  derivation.

- Redis commands consist of one or multiple command parts or follow a
  different naming strategy. The recommended pattern for commands
  provided by modules is using dot notation. Command methods can derive
  from "camel humps" that style by placing a dot (`.`) between name
  parts.

!!! NOTE
    Command names are attempted to be resolved against `CommandType` to
    participate in settings for known commands. These are primarily used
    to determine a command intent (whether a command is a read-only one).
    Commands are resolved case-sensitive. Use lower-case command names in
    `@Command` to resolve to an unknown command to e.g. enforce
    master-routing.

### CamelCase in method names

Command methods use by default the method name command type. This is
ideal for commands like `GET`, `SET`, `ZADD` and so on. Some commands,
such as `CLIENT SETNAME` consist of multiple command segments and
passing `SETNAME` as argument to a method `client(…)` feels rather
clunky.

Camel case is a natural way to express word boundaries in method names.
These "camel humps" (changes in letter casing) can be interpreted in
different ways. The most common case is to translate a change in case
into a space between command segments.

``` java
interface ServerCommands extends Commands {
    String clientSetname(String name);
}
```

Invoking `clientSetname(…)` will execute the Redis command
`CLIENT SETNAME name`.

#### `@CommandNaming`

Camel humps are translated to whitespace-delimited command segments by
default. Methods and the commands interface can be annotated with
`@CommandNaming` to apply a different strategy.

``` java
@CommandNaming(strategy = Strategy.DOT)
interface MixedCommands extends Commands {

    @CommandNaming(strategy = Strategy.SPLIT)
    String clientSetname(String name);

    @CommandNaming(strategy = Strategy.METHOD_NAME)
    String mSet(String key1, String value1, String key2, String value2);

    double nrRun(String key, int... indexes)
}
```

You can choose amongst multiple strategies:

- `SPLIT`: Splits camel-case method names into multiple command
  segments: `clientSetname` executes `CLIENT SETNAME`. This is the
  default strategy.

- `METHOD_NAME`: Uses the method name as-is: `mSet` executes `MSET`.

- `DOT`: Translates camel-case method names into dot-notation that is
  the recommended pattern for module-provided commands. `nrRun` executes
  `NR.RUN`.

### `@Command` annotation

You already learned, that method names are used as command type any by
default all arguments are appended to the command. Some cases, such as
the example from above, require in Java declaring a method with a
different name because of variance in the return type. `mgetAsValues`
would execute a non-existent command `MGETASVALUES`.

Annotating command methods with `@Command` lets you take control over
implicit conventions. The annotation value overrides the command name
and provides command segments to command methods. Command segments are
parts of a command that are sent to Redis. The semantics of a command
segment depend on context and the command itself.
`@Command("CLIENT SETNAME")` denotes a subcommand of the `CLIENT`
command while a method annotated with `@Command("SET key")` invokes
`SET`, using `mykey` as key. `@Command` lets you specify whole command
strings and reference [parameters](#parameters) to construct custom
commands.

``` java
interface MixedCommands extends Commands {

    @Command("CLIENT SETNAME")
    String setName(String name);

    @Command("MGET")
    List<Value<String> mgetAsValues(String... keys);

    @Command("SET mykey")
    String set(String value);

    @Command("NR.OBSERVE ?0 ?1 -> ?2 TRAIN")
    List<Integer> nrObserve(String key, int[] in, int... out)
}
```

### Parameters

Most Redis commands take one or more parameters to operate with your
data. Using command methods with Redis appends all parameters in their
specified order to the command as arguments. You have already seen
commands annotated with `@Command("MGET")` or with no annotation at all.
Commands append their parameters as command arguments as declared in the
method signature.

``` java
interface MixedCommands extends Commands {

    @Command("SET ?1 ?0")
    String set(String value, String key);

    @Command("NR.OBSERVE :key :in -> :out TRAIN")
    List<Integer> nrObserve(@Param("key") String key, @Param("in") int[] in, @Param("out") int... out)
}
```

`@Command`-annotated command methods allow references to parameters. You
can use index-based or name-based parameter references. Index-based
references (`?0`, `?1`, …) are zero-based. Name-based parameters
(`:key`, `:in`) reference parameters by their name. Java 8 provides
access to parameter names if the code was compiled with
`javac -parameters`. Parameter names can be supplied alternatively by
`@Param`. Please note that all parameters are required to be annotated
if using `@Param`.

!!! NOTE
    The same parameter can be referenced multiple times. Not referenced
    parameters are appended as arguments after the last command segment.

#### Keys and values

Redis commands are usually less concerned about key and value type since
all data is bytes anyway. In the context of Redis Cluster, the very
first key affects command routing. Keys and values are discovered by
verifying their declared type assignability to `RedisCodec` key and
value types. In some cases, where keys and values are indistinguishable
from their types, it might be required to hint command methods about
keys and values. You can annotate key and value parameters with `@Key`
and `@Value` to control which parameters should be treated as keys or
values.

``` java
interface KeyCommands extends Commands {

    String set(@Key String key, @Value String value);
}
```

Hinting command method parameters influences
[`RedisCodec`](#codecs) selection.

#### Parameter types

Command method parameter types are just limited by the
[`RedisCodec`s](#codecs) that are supplied to
`RedisCommandFactory`. Command methods, however, support a basic set of
parameter types that are agnostic to the selected codec. If a parameter
is identified as key or value and the codec supports that parameter,
this specific parameter is encoded by applying codec conversion.

Built-in parameter types:

- `String` - encoded to bytes using `ASCII`.

- `byte[]`

- `double`/`Double`

- `ProtocolKeyword` - using its byte-representation. `ProtocolKeyword`
  is useful to declare/reuse commonly used Redis keywords, see
  `io.lettuce.core.protocol.CommandType` and
  `io.lettuce.core.protocol.CommandKeyword`.

- `Map` - key and value encoding of key-value pairs using `RedisCodec`.

- types implementing `io.lettuce.core.CompositeParameter` - Lettuce
  comes with a set of command argument types such as `BitFieldArgs`,
  `SetArgs`, `SortArgs`, … that can be used as parameter. Providing
  `CompositeParameter` will ontribute multiple command arguments by
  invoking the `CompositeParameter.build(CommandArgs)` method.

- `Value`, `KeyValue`, and `ScoredValue` that are encoded to their
  value, key and value and score and value representation using
  `RedisCodec`.

- `GeoCoordinates` - contribute longitude and latitude command arguments

- `Limit` - used together with `ZRANGEBYLEX`/`ZRANGEBYSCORE` commands.
  Will add `LIMIT (offset) (count)` segments to the command.

- `Range` - used together with `ZCOUNT`/`ZRANGEBYLEX`/`ZRANGEBYSCORE`
  commands. Numerical commands are converted to numerical boundaries
  (`` inf`, `(1.0`, `[1.0`). Value-typed `Range` parameters are encoded to their value boundary representation (` ``,
  `-`, `[value`, `(value`).

Command methods accept other, special parameter types such as `Timeout`
or `FlushMode` that control [execution-model
specific](#execution-models) behavior. Those parameters are filtered
from command arguments.

### Codecs

Redis command interfaces use `RedisCodec`s for key/value encoding and
decoding. Each command method performs `RedisCodec` resolution so each
command method can use a different `RedisCodec`. Codec resolution is
based on key and value types declared in the command method signature.
Key and value parameters can be annotated with `@Key`/`@Value`
annotations to hint codec resolution to the appropriate types. Codec
resolution checks all annotated parameters for compatibility. If types
are assignable to codec types, the codec is selected for a particular
command method.

Codec resolution without annotation is based on a compatible type
majority. A command method resolves to the codec accepting the most
compatible types. See also [Keys and values](#keys-and-values) for
details on key/value encoding. Depending on provided codecs and the
command method signature it’s possible that no codec can be resolved.
You need to provide either a compatible `RedisCodec` or adjust parameter
types in the method signature to provide a compatible method signature.
`RedisCommandFactory` uses `StringCodec` (UTF-8) and `ByteArrayCodec` by
default.

``` java
RedisCommandFactory factory = new RedisCommandFactory(connection, Arrays.asList(new ByteArrayCodec(), new StringCodec(LettuceCharsets.UTF8)));
```

The resolved codec is also applied to command response deserialization
that allows you to use parametrized command response types.

### Response types

Another aspect of command methods is their response type. Redis command
responses consist of simple strings, bulk strings (byte streams) or
arrays with nested elements depending on the issued command.

You can choose amongst various return types that map to a particular
{custom-commands-command-output-link}. A command output can return
either its return type directly (`List<String>` for `StringListOutput`)
or stream individual elements (`String` for `StringListOutput` as it
implements `StreamingOutput<String>`). Command output resolution depends
on whether the declared return type supports streaming. The currently
only supported streaming output are reactive wrappers such as `Flux`.

`RedisCommandFactory` comes with built-in command outputs that are
resolved from `OutputRegistry`. You can choose from built-in command
output types or register your own `CommandOutput`.

A command method can return its response directly or wrapped in a
response wrapper. See [Execution models](#execution-models) for
execution-specific wrapper types.

| `CommandOutput` class | return type | streaming type |
|----|----|----|
| `ListOfMapsOutput` | `List<Map<K, V>>` |  |
| `ArrayOutput` | `List<Object>` |  |
| `DoubleOutput` | `Double`, `double` |  |
| `ByteArrayOutput` | `byte[]` |  |
| `IntegerOutput` | `Long`, `long` |  |
| `KeyOutput` | `K` (Codec key type) |  |
| `KeyListOutput` | `List<K>` (Codec key type) | `K` (Codec key type) |
| `ValueOutput` | `V` (Codec value type) |  |
| `ValueListOutput` | `List<V>` (Codec value type) | `V` (Codec value type) |
| `ValueSetOutput` | `Set<V>` (Codec value type) |  |
| `MapOutput` | `Map<K, V>` |  |
| `BooleanOutput` | `Boolean`, `boolean` |  |
| `BooleanListOutput` | `List<Boolean>` | `Boolean` |
| `GeoCo ordinatesListOutput` | `GeoCoordinates` |  |
| `GeoCoordin atesValueListOutput` | `List<Val ue<GeoCoordinates>>` | `V alue<GeoCoordinates>` |
| `Sc oredValueListOutput` | `L ist<ScoredValue<V>>` | `ScoredValue<V>` |
| `St ringValueListOutput` (ASCII) | `List<Value<String>>` | `Value<String>` |
| `StringListOutput` (ASCII) | `List<String>` | `String` |
| `V alueValueListOutput` | `List<Value<V>>` | `Value<V>` |
| `VoidOutput` | `Void`, `void` |  |

Built-in command output types

## Execution models

Each declared command methods requires a synchronization mode, more
specific an execution model. Lettuce uses an event-driven command
execution model to send commands, process responses, and signal
completion. Command methods can execute their commands in a synchronous,
[asynchronous](user-guide/async-api.md) or [reactive](user-guide/reactive-api.md) way.

The choice of a particular execution model is made on return type level,
more specific on the return type wrapper. Each command method may use a
different execution model so command methods within a command interface
may mix different execution models.

### Synchronous (Blocking) Execution

Declaring a non-wrapped return type (like `List<V>`, `String`) will
execute commands synchronously. See
{custom-commands-command-exec-model-link} on more details on synchronous
command execution.

Blocking command execution applies by default timeouts set on connection
level. Command methods support timeouts on invocation level by defining
a special `Timeout` parameter. The parameter position does not affect
command segments since special parameters are filtered from the command
arguments. Supplying `null` will apply connection defaults.

``` java
interface KeyCommands extends Commands {

    String get(String key, Timeout timeout);
}

KeyCommands commands = …

commands.get("key", Timeout.create(10, TimeUnit.SECONDS));
```

### Asynchronous (Future) Execution

Command methods wrapping their response in `Future`,
`CompletableFuture`, `CompletionStage` or `RedisFuture` will execute
their commands asynchronously. Invoking an asynchronous command method
will send the command to Redis at invocation time and return a return
handle that allows you to synchronize or chain command execution.

``` java
interface KeyCommands extends Commands {

    RedisFuture<String> get(String key, Timeout timeout);
}
```

### Reactive Execution

You can declare command methods that wrap their response in a reactive
type for reactive command execution. Invoking a reactive command method
will not send the command to Redis until the resulting subscriber
signals demand for data to its subscription. Using reactive wrapper
types allow [result streaming](#response-types) by emitting data as it’s
received from the I/O channel.

Currently supported reactive types:

- Project Reactor `Mono` and `Flux` (native)

- RxJava 1 `Single` and `Observable` (via `rxjava-reactive-streams`)

- RxJava 2 `Single`, `Maybe` and `Flowable` (via `rxjava` 2.0)

See [Reactive API](user-guide/reactive-api.md) for more details.

``` java
interface KeyCommands extends Commands {

    @Command("GET")
    Mono<String> get(String key);

    @Command("GET")
    Maybe<String> getRxJava2Maybe(String key);

    Flowable<String> lrange(String key, long start, long stop);
}
```

### Batch Execution

Command interfaces support command batching to collect multiple commands
in a batch queue and flush the batch in a single write to the transport.
Command batching executes commands in a deferred nature. This means that
at the time of invocation no result is available. Batching can be only
used with synchronous methods without a return value (`void`) or
asynchronous methods returning a `RedisFuture`. Reactive command
batching is not supported because reactive executed commands maintain an
own subscription lifecycle that is decoupled from command method
batching.

Command batching can be enabled on two levels:

- On class level by annotating the command interface with `@BatchSize`.
  All methods participate in command batching.

- On method level by adding `CommandBatching` to the arguments. Method
  participates selectively in command batching.

``` java
@BatchSize(50)
interface StringCommands extends Commands {

    void set(String key, String value);

    RedisFuture<String> get(String key);

    RedisFuture<String> get(String key, CommandBatching batching);
}

StringCommands commands = …

commands.set("key", "value"); // queued until 50 command invocations reached.
                              // The 50th invocation flushes the queue.

commands.get("key", CommandBatching.queue()); // invocation-level queueing control
commands.get("key", CommandBatching.flush()); // invocation-level queueing control,
                                              // flushes all queued commands
```

Batching can be controlled on per invocation by passing a
`CommandBatching` argument. `CommandBatching` has precedence over
`@BatchSize`.

To flush queued commands at any time (without further command
invocation), add `BatchExecutor` to your interface definition.

``` java
@BatchSize(50)
interface StringCommands extends Commands, BatchExecutor {

    RedisFuture<String> get(String key);
}

StringCommands commands = …

commands.set("key");

commands.flush() // force-flush
```

#### Batch execution synchronization

Queued command batches are flushed either on reaching the batch size or
force flush (via `BatchExecutor.flush()` or `CommandBatching.flush()`).
Errors are transported through `RedisFuture`. Synchronous commands don’t
receive any result/exception signal except if the batch is flushed
through a synchronous method call. Synchronous flushing throws
`BatchException` containing the failed commands.

