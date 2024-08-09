# Frequently Asked Questions

## I’m seeing `RedisCommandTimeoutException`

**Symptoms:**

`RedisCommandTimeoutException` with a stack trace like:

    io.lettuce.core.RedisCommandTimeoutException: Command timed out after 1 minute(s)
    at io.lettuce.core.ExceptionFactory.createTimeoutException(ExceptionFactory.java:51)
    at io.lettuce.core.LettuceFutures.awaitOrCancel(LettuceFutures.java:114)
    at io.lettuce.core.FutureSyncInvocationHandler.handleInvocation(FutureSyncInvocationHandler.java:69)
    at io.lettuce.core.internal.AbstractInvocationHandler.invoke(AbstractInvocationHandler.java:80)
    at com.sun.proxy.$Proxy94.set(Unknown Source)

**Diagnosis:**

1.  Check the debug log (log level `DEBUG` or `TRACE` for the logger
    `io.lettuce.core.protocol`)

2.  Take a Thread dump to investigate Thread activity

3.  Investigate Lettuce usage, specifically for
    `setAutoFlushCommands(false)` calls

4.  Do you use a custom `RedisCodec`?

**Cause:**

Command timeouts are caused by the fact that a command was not completed
within the configured timeout. Timeouts may be caused for various
reasons:

1.  Redis server has crashed/network partition happened and your Redis
    service didn’t recover within the configured timeout

2.  Command was not finished in time. This can happen if your Redis
    server is overloaded or if the connection is blocked by a command
    (e.g. `BLPOP 0`, long-running Lua script). See also
    [gives](#blpopdurationzero--gives-rediscommandtimeoutexception).

3.  Configured timeout does not match Redis’s performance.

4.  If you block the `EventLoop` (e.g. calling blocking methods in a
    `RedisFuture` callback or in a Reactive pipeline). That can easily
    happen when calling Redis commands in a Pub/Sub listener or a
    `RedisConnectionStateListener`.

5.  If you manually control the flushing behavior of commands
    (`setAutoFlushCommands(true/false)`), you should have a good reason
    to do so. In multi-threaded environments, race conditions may easily
    happen, and commands are not flushed. Updating a missing or
    misplaced `flushCommands()` call might solve the problem.

6.  If you’re using a custom `RedisCodec` that can fail during encoding,
    this will desynchronize the protocol state.

**Action:**

Check for the causes above. If the configured timeout does not match
your Redis latency characteristics, consider increasing the timeout.
Never block the `EventLoop` from your code. Make sure that your
`RedisCodec` doesn’t fail on encode.

## `blpop(Duration.ZERO, …)` gives `RedisCommandTimeoutException`

**Symptoms:**

Calling `blpop`, `brpop` or any other blocking command followed by
`RedisCommandTimeoutException` with a stack trace like:

    io.lettuce.core.RedisCommandTimeoutException: Command timed out after 1 minute(s)
    at io.lettuce.core.ExceptionFactory.createTimeoutException(ExceptionFactory.java:51)
    at io.lettuce.core.LettuceFutures.awaitOrCancel(LettuceFutures.java:114)
    at io.lettuce.core.FutureSyncInvocationHandler.handleInvocation(FutureSyncInvocationHandler.java:69)
    at io.lettuce.core.internal.AbstractInvocationHandler.invoke(AbstractInvocationHandler.java:80)
    at com.sun.proxy.$Proxy94.set(Unknown Source)

**Cause:**

The configured command timeout applies without considering
command-specific timeouts.

**Action:**

There are various options:

1.  Configure a higher default timeout.

2.  Consider a timeout that meets the default timeout when calling
    blocking commands.

3.  Configure `TimeoutOptions` with a custom `TimeoutSource`

``` java
TimeoutOptions timeoutOptions = TimeoutOptions.builder().timeoutSource(new TimeoutSource() {
    @Override
    public long getTimeout(RedisCommand<?, ?, ?> command) {

        if (command.getType() == CommandType.BLPOP) {
            return TimeUnit.MILLISECONDS.toNanos(CommandArgsAccessor.getFirstInteger(command.getArgs()));
        }

        // -1 indicates fallback to the default timeout
        return -1;
    }
}).build();
```

Note that commands that timed out may block the connection until either
the timeout exceeds or Redis sends a response.

## Excessive Memory Usage or `RedisException` while disconnected

**Symptoms:**

`RedisException` with one of the following messages:

    io.lettuce.core.RedisException: Request queue size exceeded: n. Commands are not accepted until the queue size drops.

    io.lettuce.core.RedisException: Internal stack size exceeded: n. Commands are not accepted until the stack size drops.

Or excessive memory allocation.

**Diagnosis:**

1.  Check Redis connectivity

2.  Inspect memory usage

**Cause:**

Lettuce auto-reconnects by default to Redis to minimize service
disruption. Commands issued while there’s no Redis connection are
buffered and replayed once the server connection is reestablished. By
default, the queue is unbounded which can lead to memory exhaustion.

**Action:**

You can configure disconnected behavior and the request queue size
through `ClientOptions` for your workload profile. See [Client
Options](advanced-usage.md#client-options) for further reference.

## Performance Degradation using the Reactive API with a single connection

**Symptoms:**

Performance degradation when using the Reactive API with a single
connection (i.e. non-pooled connection arrangement).

**Diagnosis:**

1.  Inspect Thread affinity of reactive signals

**Cause:**

Netty’s threading model assigns a single Thread to each connection which
makes I/O for a single `Channel` effectively single-threaded. With a
significant computation load and without further thread switching, the
system leverages a single thread and therefore leads to contention.

**Action:**

You can configure signal multiplexing for the reactive API through
`ClientOptions` by enabling `publishOnScheduler(true)`. See [Client
Options](advanced-usage.md#client-options) for further reference. Alternatively, you can
configure `Scheduler` on each result stream through
`publishOn(Scheduler)`. Note that the asynchronous API features the same
behavior and you might want to use `then…Async(…)`, `run…Async(…)`,
`apply…Async(…)`, or `handleAsync(…)` methods along with an `Executor`
object.
