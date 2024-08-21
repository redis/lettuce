## Transactions/Multi

Transactions allow the execution of a group of commands in a single
step. Transactions can be controlled using `WATCH`, `UNWATCH`, `EXEC`,
`MULTI` and `DISCARD` commands. Synchronous, asynchronous, and reactive
APIs allow the use of transactions.

!!! note
    Transactional use requires external synchronization when a single
    connection is used by multiple threads/processes. This can be achieved
    either by serializing transactions or by providing a dedicated
    connection to each concurrent process. Lettuce itself does not
    synchronize transactional/non-transactional invocations regardless of
    the used API facade.

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

