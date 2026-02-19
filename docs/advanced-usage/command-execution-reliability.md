# Command execution reliability

Lettuce is a thread-safe and scalable Redis client that allows multiple
independent connections to Redis.

## General

Lettuce provides two levels of consistency; these are the rules for
Redis command sends:

### Depending on the chosen consistency level

- **at-most-once execution**, i.e. no guaranteed execution

- **at-least-once execution**, i.e. guaranteed execution (with [some
  exceptions](#exceptions-to-at-least-once))

### Always

- command ordering in the order of invocations

## What does *at-most-once* mean?

When it comes to describing the semantics of an execution mechanism,
there are three basic categories:

- **at-most-once** execution means that for each command handed to the
  mechanism, that command is execution zero or one time; in more casual
  terms it means that commands may be lost.

- **at-least-once** execution means that for each command handed to the
  mechanism potentially multiple attempts are made at execution it, such
  that at least one succeeds; again, in more casual terms this means
  that commands may be duplicated but not lost.

- **exactly-once** execution means that for each command handed to the
  mechanism exactly one execution is made; the command can neither be
  lost nor duplicated.

The first one is the cheapest - the highest performance, least
implementation overhead - because it can be done without tracking
whether the command was sent or got lost within the transport mechanism.
The second one requires retries to counter transport losses, which means
keeping the state at the sending end and having an acknowledgment
mechanism at the receiving end. The third is most expensive—and has
consequently worst performance—because also to the second it requires a
state to be kept at the receiving end to filter out duplicate
executions.

## Why No Guaranteed Delivery?

At the core of the problem lies the question what exactly this guarantee
shall mean:

1.  The command is sent out on the network?

2.  The command is received by the other host?

3.  The command is processed by Redis?

4.  The command response is sent by the other host?

5.  The command response is received by the network?

6.  The command response is processed successfully?

Each one of these have different challenges and costs, and it is obvious
that there are conditions under which any command sending library would
be unable to comply. Think for example about how a network partition
would affect point three, or even what it would mean to decide upon the
“successfully” part of point six.

The only meaningful way for a client to know whether an interaction was
successful is by receiving a business-level acknowledgment command,
which is not something Lettuce could make up on its own.

Lettuce allows two levels of consistency; each one has its costs and
benefits, and therefore it does not try to lie and emulate a leaky
abstraction.

## Message Ordering

The rule more specifically is that commands sent are not be executed
out-of-order.

The following illustrates the guarantee:

- Thread `T1` sends commands `C1`, `C2`, `C3` to Redis

- Thread `T2` sends commands `C4`, `C5`, `C6` to Redis

This means that:

- If `C1` is executed, it must be executed before `C2` and `C3`.

- If `C2` is executed, it must be executed before `C3`.

- If `C4` is executed, it must be executed before `C5` and `C6`.

- If `C5` is executed, it must be executed before `C6`.

- Redis executes commands from `T1` interleaved with commands from `T2`.

- If there is no guaranteed delivery, any of the commands may be
  dropped, i.e. not arrive at Redis.

## Failures and *at-least-once* execution

Lettuce’s *at-least-once* execution is scoped to the lifecycle of a
logical connection. Redis commands are not persisted to be executed
after a JVM or client restart. All Redis command state is held in
memory. A retry mechanism re-executes commands that are not successfully
completed if a network failure occurs. In more casual terms, when Redis
is available again, the retry mechanism fires all queued commands.
Commands that are issued as long as the failure persists are buffered.

*at-least-once* execution ensures a higher consistency level than
*at-most-once* but comes with some caveats:

- Commands can be executed more than once

- Higher usage of resources since commands are buffered and sent again
  after reconnect

### Exceptions to *at-least-once*

Lettuce does not lose commands while sending them. A command execution
can, however, fail for the same reasons as a normal method call can on
the JVM:

- `StackOverflowError`

- `OutOfMemoryError`

- other `Error`s

Also, executions can fail in specific ways:

- The command runs into a timeout

- The command cannot be encoded

- The command cannot be decoded, because:

- The output is not compatible with the command output

- Exceptions occur while command decoding/processing. This may happen a
  `StreamingChannel` results in an error, or a consumer of Pub/Sub
  events fails while listener notification.

While the first is clearly a matter of configuration, the second
deserves some thought: The command execution does not get feedback if
there was a timeout. This is in general not distinguishable from a lost
message. By using the Sync API, commands that exceeded their timeout are
canceled. This behavior cannot be changed. When using the Async API,
users can decide, how to proceed with the command, whether the command
should be canceled.

Commands which run into `Exception`s while encoding or decoding reach a
non-recoverable state. Commands that cannot be *encoded* are **not**
executed but get canceled. Commands that cannot be *decoded* were
already executed; only the result is not available. These errors are
caused mostly due to a wrong implementation. The result of a command,
which cannot be *decoded* is that the command gets canceled, and the
causing `Exception` is available in the result. The command is cleared
from the response queue, and the connection stays usable.

In general, when `Errors` occur while operating on a connection, you
should close the connection and use a new one. Connections, that
experienced such severe failures get into an unrecoverable state, and no
further response processing is possible.

Executing commands more than once

In terms of consistency, Redis commands can be grouped into two
categories:

- Idempotent commands

- Non-idempotent commands

Idempotent commands are commands that lead to the same state if they are
executed more than once. Read commands are a good example for
idempotency since they do not change the state of data. Another set of
idempotent commands are commands that write a whole data structure/entry
at once such as `SET`, `DEL` or `CLIENT SETNAME`. Those commands change
the data to the desired state. Subsequent executions of the same command
leave the data in the same state.

Non-idempotent commands change the state with every execution. This
means, if you execute a command twice, each resulting state is different
in comparison to the previous. Examples for non-idempotent Redis
commands are such as `LPUSH`, `PUBLISH` or `INCR`.

Note: When using master-replica replication, different rules apply to
*at-least-once* consistency. Replication between Redis nodes works
asynchronously. A command can be processed successfully from Lettuce’s
client perspective, but the result is not necessarily replicated to the
replica yet. If a failover occurs at that moment, a replica takes over,
and the not yet replicated data is lost. Replication behavior is
Redis-specific. Further documentation about failover and consistency
from Redis perspective is available within the Redis docs:
<https://redis.io/docs/latest/operate/oss_and_stack/management/replication/>

## Switching between *at-least-once* and *at-most-once* operations

Lettuce’s consistency levels are bound to retries on reconnects and the
connection state. By default, Lettuce operates in the *at-least-once*
mode. Auto-reconnect is enabled and as soon as the connection is
re-established, queued commands are re-sent for execution. While a
connection failure persists, issued commands are buffered.

To change into *at-most-once* consistency level, disable auto-reconnect
mode. Connections can no longer be reconnected and thus no retries are
issued. Unsuccessful commands are canceled. New commands are rejected.

### Controlling replay of commands in *at-least-once* mode

!!! NOTE
    This feature is only available since Lettuce 6.6

One can achieve a more fine-grained control over the commands that are
replayed after a reconnection by using the option to specify a filter
predicate. This option is part of the ClientOptions configuration. See 
[Client Options](client-options.md) for further reference.

``` java
Predicate<RedisCommand<?, ?, ?> > filter = cmd -> 
    cmd.getType().toString().equalsIgnoreCase("DECR");

client.setOptions(ClientOptions.builder()
    .autoReconnect(true)
    .replayFilter(filter)
    .build());
```

The code above would filter out all `DECR` commands from being replayed
after a reconnection. Another, perhaps more popular example, would be:

``` java
Predicate<RedisCommand<?, ?, ?> > filter = cmd -> true;

client.setOptions(ClientOptions.builder()
    .autoReconnect(true)
    .replayFilter(filter)
    .build());
```

... which disables any command replay, but still allows the driver to
re-connect, basically providing a way to have auto-reconnect without
auto-replay of commands.

## Clustered operations

Lettuce sticks in clustered operations to the same rules as for
standalone operations but with one exception:

Command execution on master nodes, which is rejected by a `MOVED`
response are tried to re-execute with the appropriate connection.
`MOVED` errors occur on master nodes when a slot’s responsibility is
moved from one cluster node to another node. Afterwards *at-least-once*
and *at-most-once* rules apply.

When the cluster topology changes, generally spoken, the cluster slots
or master/replica state is reconfigured, following rules apply:

- **at-most-once** If the connection is disconnected, queued commands
  are canceled and buffered commands, which were not sent, are executed
  by using the new cluster view

- **at-least-once** If the connection is disconnected, queued and
  buffered commands, which were not sent, are executed by using the new
  cluster view

- If the connection is not disconnected, queued commands are finished
  and buffered commands, which were not sent, are executed by using the
  new cluster view

