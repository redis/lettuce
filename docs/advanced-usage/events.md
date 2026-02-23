# Events

## Before 3.4/4.1

lettuce can notify its users of certain events:

- Connected

- Disconnected

- Exceptions in the connection handler pipeline

You can subscribe to these events using `RedisClient#addListener()` and
unsubscribe with `RedisClient.removeListener()`. Both methods accept a
`RedisConnectionStateListener`.

`RedisConnectionStateListener` receives as connection the async
implementation of the connection. This means if you use a sync way (e.
g. `RedisConnection`) you will receive the `RedisAsyncConnectionImpl`
instance

**Example**

``` java
RedisClient client = new RedisClient(host, port);
client.addListener(new RedisConnectionStateListener()
{
    @Override
    public void onRedisConnected(RedisChannelHandler<?, ?> connection)
    {

    }
    @Override
    public void onRedisDisconnected(RedisChannelHandler<?, ?> connection)
    {

    }
    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause)
    {

    }
});
```

## Since 3.4/4.1

The client produces events during its operation and uses an event bus
for the transport. The `EventBus` can be configured and obtained from
the [client resources](client-resources.md) and is used for
client- and custom events.

Following events are sent by the client:

- Connection events

- Metrics events

- Cluster topology events

### Subscribing to events

The simple-most approach to subscribing to the client events is
obtaining the event bus from the client’s client resources.

``` java
RedisClient client = RedisClient.create()
EventBus eventBus = client.getResources().eventBus();

eventBus.get().subscribe(e -> System.out.println(event));

...
client.shutdown();
```

Calls to the `subscribe()` method will return a `Subscription`. If you
plan to unsubscribe from the event stream, you can do so by calling the
`Subscription.unsubscribe()` method. The event bus utilizes
[RxJava](http://reactivex.io) and the {reactive-api} to transport events
from the publisher to its subscribers.

A thread of the computation thread pool (can be configured using [client
resources](client-resources.md)) transports the events.

### Connection events

When working with events, multiple events occur. These can be used to
monitor connections or react to these. Connection events transport the
local and the remote connection points. The regular order of connection
events is:

1.  Connected: The transport-layer connection is established (TCP or
    Unix Domain Socket connection established). Event type:
    `ConnectedEvent`

2.  Connection activated: The logical connection is activated and can be
    used to dispatch Redis commands (SSL handshake complete, PING before
    activating response received). Event type:
    `ConnectionActivatedEvent`

3.  Disconnected: The transport-layer connection is closed/reset. That
    event occurs on regular connection shutdowns and connection
    interruptions (outage). Event type: `DisconnectedEvent`

4.  Connection deactivated: The logical connection is deactivated. The
    internal processing state is reset and the `isOpen()` flag is set to
    `false` That event occurs on regular connection shutdowns and
    connection interruptions (outage). Event type:
    `ConnectionDeactivatedEvent`

5.  Since 5.3: Reconnect failed: A reconnect attempt failed. Contains
    the reconnect failure and the retry counter. Event type:
    `ReconnectFailedEvent`

### Metrics events

Client command metrics is published using the event bus. The current
event carries command latency metrics. Latency metrics is segregated by
connection or server and command which means you can get detailed
statistics on every command. Connection distinction allows seeing how
particular connections perform. Server distinction how particular
servers perform. You can configure metrics collection using [client
resources](client-resources.md).

In detail, two command latencies are recorded:

1.  RTT from dispatching the command until the first command response is
    processed (first response)

2.  RTT from dispatching the command until the full command response is
    processed and at the moment the command is completed (completion)

The latency metrics provide following statistics:

- Number of commands

- min latency

- max latency

- latency percentiles

**First Response Latency**

The first response latency measuring begins at the moment the command
sending begins (command flush on the netty event loop). That is not the
time at when at which the command was issued from the client API. The
latency time recording ends at the moment the client receives the first
command bytes and starts to process the command response. Both
conditions must be met to end the latency recording. The client could be
busy with processing the previous command while the first bytes are
already available to read. That scenario would be a good time to file an
[issue](https://github.com/mp911de/lettuce/issues) for improving the
client performance. The first response latency value is good to
determine the lag/network performance and can give a hint on the client
and server performance.

**Completion Latency**

The completion latency begins at the same time as the first response
latency but lasts until the time where the client is just about to call
the `complete()` method to signal command completion. That means all
command response bytes arrived and were decoded/processed, and the
response data structures are ready for consumption for the user of the
client. On completion callback duration (such as async or observable
callbacks) are not part of the completion latency.

### Cluster events

When using Redis Cluster, you might want to know when the cluster
topology changes. As soon as the cluster client discovers the cluster
topology change, a `ClusterTopologyChangedEvent` event is published to
the event bus. The time at which the event is published is not
necessarily the time the topology change occurred. That is because the
client polls the topology from the cluster.

The cluster topology changed event carries the topology view before and
after the change.

Make sure, you enabled cluster topology refresh in the [Client
options](#cluster-specific-options).

## Java Flight Recorder Events (since 6.1)

Lettuce emits Connection and Cluster events as Java Flight Recorder
events. `EventBus` emits all events to `EventRecorder` and the actual
event bus.

`EventRecorder` verifies whether your runtime provides the required JFR
classes (available as of JDK 8 update 262 or later) and if so, then it
creates Flight Recorder variants of the event and commits these to JFR.

The following events are supported out of the box:

**Redis Connection Events**

- Connection Attempt

- Connect, Disconnect, Connection Activated, Connection Deactivated

- Reconnect Attempt and Reconnect Failed

**Redis Cluster Events**

- Topology Refresh initiated

- Topology Changed

- ASK and MOVED redirects

**Redis Master/Replica Events**

- Sentinel Topology Refresh initiated

- Master/Replica Topology Changed

Events come with a rich set of event attributes such as channelId, epId
(endpoint Id), Redis URI and many more.

You can record data by starting your application with:

``` shell
java -XX:StartFlightRecording:filename=recording.jfr,duration=10s …
```

You can disable JFR events use through system properties. Set
`io.lettuce.core.jfr` to `false`.
