## Publish/Subscribe

Lettuce provides support for Publish/Subscribe on Redis Standalone and
Redis Cluster connections. The connection is notified on
message/subscribed/unsubscribed events after subscribing to channels or
patterns. [Synchronous](connecting-redis.md#basic-usage), [asynchronous](async-api.md)
and [reactive](reactive-api.md) API’s are provided to interact with Redis
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

!!! NOTE
    Don’t issue blocking calls (includes synchronous API calls to Lettuce)
    from inside of Pub/Sub callbacks as this would block the EventLoop. If
    you need to fetch data from Redis from inside a callback, please use
    the asynchronous API.

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