lettuce 3.4 RELEASE NOTES
=========================

lettuce 3.4 is here. This release contains numerous features and bugfixes. Lettuce 3.4 introduces reusable
client-resources, an EventBus, client metrics, and support for newly introduced commands. This version
works with Redis 3.2 RC3 but Redis expects a change in the format of CLUSTER NODES. So watch out for a new
release of lettuce as soon as Redis 3.2 RC4 or a final is released.

lettuce is available in two major versions. The 3.x stream and the 4.x stream. Both streams are maintained. 
The 4.x stream introduced breaking changes, as a user of 3.x you might be interested in the migration guide from 3.x to 4.x.
See https://github.com/mp911de/lettuce/wiki/Migration-from-3.x-to-4.x

After this release, the 4.x branch will be promoted to the default branch.
Following rules should give a guidance for the stream in which a particular change is done:

**Changes affecting both streams**

* New Redis commands (such as HSTRLEN)
* Bugfixes

**Changes for the 4.x stream only**

* New Redis paradigms
* Enriching the API (such as multi-key command execution in the Cluster API)
* Technical improvements to the client (such as the Reactive API)

The 3.x stream will be maintained at least until end of 2016.


Reusable ClientResources
---------------------------
lettuce requires a threading infrastructure to operate. Threads are grouped within `EventLoopGroup`s and are 
expensive resources. They need to be spun up and shut down. Prior to this release, each instance of 
`RedisClient` and `RedisClusterClient` created its own `EventLoopGroup`s and `EventExecutorGroup`.
If you had two instances of `RedisClient` or `RedisClusterClient`, lettuce used up to 
2 * (`Runtime.getRuntime().availableProcessors()` * 4 * 3) threads. 

Two things changed now:

1. Lettuce uses at least 3 threads but at most the number of 
   available processors `Runtime.getRuntime().availableProcessors()` * 3 threads.
2. `EventLoopGroup`s are hosted within `ClientResources` 
   and can be reused across multiple `RedisClient` or `RedisClusterClient` instances

By default, each `RedisClient` and `RedisClusterClient` instance have their own, dedicated `ClientResources`.
Shared `ClientResources` can be supplied upon client creation (see below). In general, it 
is a good idea to reuse instances of `ClientResources` across multiple clients.

Shared client resources are required to be shutdown once they are no longer used. 

You can create instances using two different patterns:

**The create() factory method**

By using the `create()` method on `DefaultClientResources` you create `ClientResources` with default settings:

```java
ClientResources res = DefaultClientResources.create();

RedisClient client = RedisClient.create(res);
RedisClusterClient clusterClient = RedisClusterClient.create(res, seedUris);
...
client.shutdown();
clusterClient.shutdown();

res.shutdown();
```

This approach fits the most needs.

**Resources builder**

You can build instances of DefaultClientResources by using the embedded builder. 
It is designed to configure the resources to your needs. The builder accepts the configuration 
in a fluent fashion and then creates the `ClientResources` at the end:

```java
ClientResources res = new DefaultClientResources.Builder().
                        .ioThreadPoolSize(4)
                        .computationThreadPoolSize(4)
                        .build();

RedisClient client = RedisClient.create(res);
RedisClusterClient clusterClient = RedisClusterClient.create(res, seedUris);
...
client.shutdown();
clusterClient.shutdown();

res.shutdown();
```

A `RedisClient` and `RedisClusterClient` can be created without passing `ClientResources` upon creation. 
The resources are exclusive to the client and are managed itself by the client. 
When calling `shutdown()` of the client instance `ClientResources` are shut down.


Read more: https://github.com/mp911de/lettuce/wiki/Configuring-Client-resources


`create` methods to construct the client
----------------------------------------

lettuce 4.1 introduces new `create` methods to create client instances. The `create` methods replace
the deprecated constructors of `RedisClient` and `RedisClusterClient`. The `create` methods come with 
various signatures to support a conslidated style of client creation:

**Create a client**

```java
RedisClient client = RedisClient.create();
RedisClusterClient clusterClient = RedisClusterClient.create(seedUris);
...
```

**Create a client using shared `ClientResources` **

```java
ClientResources res = DefaultClientResources.create();
RedisClient client = RedisClient.create(res);
RedisClusterClient clusterClient = RedisClusterClient.create(res, seedUris);
...
```

EventBus and Client Events
--------------------------
lettuce introduces a new event bus to publish events. The previous client events were restricted to `Connected`, 
`Disconnected` and `ExceptionCaught` and required structural changes in the event listener. With the event bus any
event type can be published. To listen to client events you need to subscribe to the `EventBus` which is available from the
`ClientResources`.

```java
RedisClient client = RedisClient.create();
EventBus eventBus = client.getResources().eventBus();

Subscription subscription = eventBus.get()
                .filter(redisEvent -> redisEvent instanceof ConnectedEvent)
                .cast(ConnectedEvent.class)
                .subscribe(e -> System.out.println(e.localAddress()));

...
subscription.unsubscribe();
client.shutdown();
```

The event bus uses rx-java to publish events. Following events are available:
 
* ConnectedEvent
* ConnectionActivatedEvent
* DisconnectedEvent
* ConnectionDeactivatedEvent
* ClusterTopologyChangedEvent
* CommandLatencyEvent (see Command Latency Metrics for further details)

All of the built-in events carry additional details (see JavaDoc for details). 

Events are published within the scope of the `EventBus` instance that is by default part of the `ClientResources`.
The event bus will be used by multiple client instances if client resources are shared. 

Listeners implementing the `RedisConnectionStateListener` still work. 

Read more: https://github.com/mp911de/lettuce/wiki/Connection-Events 


Command Latency Metrics
-----------------------
Command latency metrics give insight about command execution and latencies. 
Metrics are collected for every completed command and are enabled by default.

Each command is tracked with:

* Execution count
* Latency to first response (min, max, percentiles)
* Latency to complete (min, max, percentiles)

Command latencies are tracked on remote endpoint (distinction by host and port or socket path) and command type level 
(`GET`, `SET`, ...). It is possible to track command latencies on a per-connection level 
(see `DefaultCommandLatencyCollectorOptions`).
 
Command latencies are transported using Events on the `EventBus`. The `EventBus` can be obtained from 
the `ClientResources` of the client instance. Please keep in mind that the `EventBus` is used for various 
event types. Filter on the event type if you're interested only in particular event types.

```java
RedisClient client = RedisClient.create();
EventBus eventBus = client.getResources().eventBus();

Subscription subscription = eventBus.get()
                .filter(redisEvent -> redisEvent instanceof CommandLatencyEvent)
                .cast(CommandLatencyEvent.class)
                .subscribe(e -> System.out.println(e.getLatencies()));
```

The `EventBus` uses rx-java to publish events. This example prints the received latencies to `stdout`. The interval and
the collection of command latency metrics can be configured in the `ClientResources`.

### Disabling command latency metrics

To disable metrics collection, use own `ClientResources` with a disabled `DefaultCommandLatencyCollectorOptions`:

```java
ClientResources res = new DefaultClientResources
        .Builder()
        .commandLatencyCollectorOptions(DefaultCommandLatencyCollectorOptions.disabled())
        .build();
        
RedisClient client = RedisClient.create(res);
```


ReadFrom Settings/Redis Cluster slave reads
-------------------------------------------
The `ReadFrom` setting describes how lettuce routes read operations to the members of a Redis Cluster.

By default, lettuce routes its read operations to the master node. Reading from the master returns the most recent 
version of the data because write operations are issued to the single master node. Reading from 
masters guarantees strong consistency.

The `ReadFrom` setting can be set to one of the following presets:

* `MASTER` Default mode. Read from the current master node.
* `MASTER_PREFERRED` Read from the master, but if it is unavailable, read from slave nodes.
* `SLAVE` Read from slave nodes.
* `NEAREST` Read from any node of the cluster with the lowest latency.

Custom read settings can be implemented by extending the `com.lambdaworks.redis.ReadFrom` class.

Read more: https://github.com/mp911de/lettuce/wiki/ReadFrom-Settings

lettuce 3.4 requires Java 8 and can be used with Java 6 and 7.

If you need any support, meet lettuce at:

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
* Gitter: https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues

Enhancements
------------
* Support CLUSTER SETSLOT <slot> STABLE command #160
* Support FLUSHALL [ASYNC]/FLUSHDB [ASYNC]/UNLINK commands #146
* Adjust logging when running into Exceptions (exceptionCaught()) #140
* Implement an EventBus system to publish events and metrics #124 (Thanks to @pulse00)
* Implement a CompressionCodec for GZIP and Deflate compression #127
* Provide a reusable client configuration for ThreadPools and other expensive resources #110
* Use much faster JDK utility for converting an int to a byte sequence #163 (Thanks to @CodingFabian)
* Cluster ReadFrom #114
* Allow limiting the request queue size #115
* Extend support for CLUSTER commands #111
* Dispatch CLUSTER commands based on the slot #112
* Support changed CLUSTER SLOTS #183

Fixes
-----
* Do not cache InetSocketAddress/SocketAddress in RedisURI #144
* pfmerge invokes PFADD instead of PFMERGE #158 (Thanks to @christophstrobl)
* Fix set with args method signature #159 (Thanks to @joshdurbin)
* fix NOAUTH error when connecting to a cluster with password protection #171 (Thanks to @liufl)
* Enable PING before connect with secured Redis servers #167
* Include BaseRedis interface in synchronous RedisCluster API #166
* Allow state-changing commands on pooled connections #162
* NullPointer with bad cluster node list #173 (Thanks to @taer)
* Avoid infinite reconnect loops when the connection is closed #126

Other
------
* Improve Redis URI documentation and query parameter handling #153
* Added configurable timeout to connection string #152

lettuce requires a minimum of Java 8 to build and Java 6 run. It is tested
continuously against Redis 3.0 and the unstable branch.

If you need any support, meet lettuce at

* Google Group: https://groups.google.com/d/forum/lettuce-redis-client-users
                or lettuce-redis-client-users@googlegroups.com
* Join the chat at https://gitter.im/mp911de/lettuce
* Github Issues: https://github.com/mp911de/lettuce/issues
* Wiki: https://github.com/mp911de/lettuce/wiki
