# High-Availability and Sharding

## Primary/Replica

Redis can increase availability and read throughput by using
replication. Lettuce provides dedicated Primary/Replica support since 4.2
for topologies and ReadFrom-Settings.

Redis Primary/Replica can be run standalone or together with Redis
Sentinel, which provides automated failover and primary promotion.
Failover and primary promotion is supported in Lettuce already since
version 3.1 for primary connections.

Connections can be obtained from the `PrimaryReplica` connection provider
by supplying the client, Codec, and one or multiple RedisURIs.

### Redis Sentinel

Primary/Replica using Redis Sentinel uses Redis
Sentinel as registry and notification source for topology events.
Details about the primary and its replicas are obtained from Redis
Sentinel. Lettuce subscribes to Redis Sentinel events for notifications to all supplied
Sentinels.

### Standalone Primary/Replica

Running a Standalone Primary/Replica setup requires one seed address to
establish a Redis connection. Providing one `RedisURI` will discover
other nodes which belong to the Primary/Replica setup and use the
discovered addresses for connections. The initial URI can point either
to a primary or a replica node.

### Static Primary/Replica with predefined node addresses

In some cases, topology discovery shouldn’t be enabled, or the
discovered Redis addresses are not suited for connections. AWS
ElastiCache falls into this category. Lettuce allows to specify one or
more Redis addresses as `List` and predefine the node topology.
Primary/Replica URIs will be treated in this case as static topology, and
no additional hosts are discovered in such case. Redis Standalone
Primary/Replica will discover the roles of the supplied `RedisURI`s and
issue commands to the appropriate node.

### Topology discovery

Primary-Replica topologies are either static or semi-static. Redis
Standalone instances with attached replicas provide no failover/HA
mechanism. Redis Sentinel managed instances are controlled by Redis
Sentinel and allow failover (which include primary promotion). The
`PrimaryReplica` API supports both mechanisms. The topology is provided
by a `TopologyProvider`:

- `ReplicaTopologyProvider`: Dynamic topology lookup using the
  `INFO REPLICATION` output. Replicas are listed as replicaN=…​ entries.
  The initial connection can either point to a primary or a replica, and
  the topology provider will discover nodes. The connection needs to be
  re-established outside of Lettuce in a case of a Primary/Replica
  failover or topology changes.

- `StaticMasterReplicaTopologyProvider`: Topology is defined by the list
  of URIs and the ROLE output. PrimaryReplica uses only the supplied
  nodes and won’t discover additional nodes in the setup. The connection
  needs to be re-established outside of Lettuce in case of a
  Primary/Replica failover or topology changes.

- `SentinelTopologyProvider`: Dynamic topology lookup using the Redis
  Sentinel API. In particular, `SENTINEL MASTER` and `SENTINEL REPLICAS`
  output. Primary/Replica failover is handled by Lettuce.

### Topology Updates

- Standalone Primary/Replica: Performs a one-time topology lookup which
  remains static afterward

- Redis Sentinel: Subscribes to all Sentinels and listens for Pub/Sub
  messages to trigger topology refreshing

#### Transactions

Since version 5.1, transactions and commands during a transaction are
routed to the primary node to ensure atomic transaction execution on a
single node. Transactions can contain read- and write-operations so the
driver cannot decide upfront which node can be used to run the actual
transaction.

#### Examples

``` java
RedisClient redisClient = RedisClient.create();

StatefulRedisPrimaryReplicaConnection<String, String> connection = PrimaryReplica.connect(redisClient, StringCodec.UTF8,
        RedisURI.create("redis://localhost"));
connection.setReadFrom(ReadFrom.PRIMARY_PREFERRED);

System.out.println("Connected to Redis");

connection.close();
redisClient.shutdown();
```

``` java
RedisClient redisClient = RedisClient.create();

StatefulRedisPrimaryReplicaConnection<String, String> connection = PrimaryReplica.connect(redisClient, StringCodec.UTF8,
        RedisURI.create("redis-sentinel://localhost:26379,localhost:26380/0#myprimary"));
connection.setReadFrom(ReadFrom.PRIMARY_PREFERRED);

System.out.println("Connected to Redis");

connection.close();
redisClient.shutdown();
```

``` java
RedisClient redisClient = RedisClient.create();

List<RedisURI> nodes = Arrays.asList(RedisURI.create("redis://host1"),
        RedisURI.create("redis://host2"),
        RedisURI.create("redis://host3"));

StatefulRedisPrimaryReplicaConnection<String, String> connection = PrimaryReplica
        .connect(redisClient, StringCodec.UTF8, nodes);
connection.setReadFrom(ReadFrom.PRIMARY_PREFERRED);

System.out.println("Connected to Redis");

connection.close();
redisClient.shutdown();
```

## Redis Sentinel

When using Lettuce, you can interact with Redis Sentinel and Redis
Sentinel-managed nodes in multiple ways:

1.  [Direct connection to Redis
    Sentinel](#direct-connection-redis-sentinel-nodes), for issuing
    Redis Sentinel commands

2.  Using Redis Sentinel to [connect to a
    primary](#redis-discovery-using-redis-sentinel)

3.  Using Redis Sentinel to connect to primary nodes and replicas through
    the `PrimaryReplica` API.

In both cases, you need to supply a `RedisURI` since the Redis Sentinel
integration supports multiple Sentinel hosts to provide high
availability.

Please note: Redis Sentinel (Lettuce 3.x) integration provides only
asynchronous connections and no connection pooling.

### Direct connection Redis Sentinel nodes

Lettuce exposes an API to interact with Redis Sentinel nodes directly.
This is useful for performing administrative tasks using Lettuce. You
can monitor new primary nodes, query primary addresses, replicas and much
more. A connection to a Redis Sentinel node is established by
`RedisClient.connectSentinel()`. Use a [Publish/Subscribe
connection](user-guide/pubsub.md) to subscribe to Sentinel events.

### Redis discovery using Redis Sentinel

One or more Redis Sentinels can monitor Redis instances . These Redis
instances are usually operated together with a replica of the Redis
instance. Once the primary goes down, the replica is promoted to a
primary. Once a primary instance is not reachable anymore, the failover
process is started by the Redis Sentinels. Usually, the client
connection is terminated. The disconnect can result in any of the
following options:

1.  The primary comes back: The connection is restored to the Redis
    instance

2.  A replica is promoted to a primary: Lettuce performs an address
    lookup using the `primaryId`. As soon as the Redis Sentinel provides
    an address the connection is restored to the new Redis instance

Read more at <https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel>

### Examples

``` java
RedisURI redisUri = RedisURI.create("redis://sentinelhost1:26379");
RedisClient client = new RedisClient(redisUri);

RedisSentinelAsyncConnection<String, String>  connection = client.connectSentinelAsync();

Map<String, String> map = connection.primary("myprimary").get();
```

``` java
RedisURI redisUri = RedisURI.Builder.sentinelPrimary("sentinelhost1", "myprimary").withSentinel("sentinelhost2").build();
RedisClient client = RedisClient.create(redisUri);

RedisConnection<String, String> connection = client.connect();
```

!!! NOTE
    Every time you connect to a Redis instance using Redis Sentinel, the
    Redis primary is looked up using a new connection to a Redis Sentinel.
    This can be time-consuming, especially when multiple Redis Sentinels
    are used and one or more of them are not reachable.

## Redis Cluster

Lettuce supports Redis Cluster with:

- Support of all `CLUSTER` commands

- Command routing based on the hash slot of the commands' key

- High-level abstraction for selected cluster commands

- Execution of commands on multiple cluster nodes

- `MOVED` and `ASK` redirection handling

- Obtaining direct connections to cluster nodes by slot and host/port
  (since 3.3)

- SSL and authentication (since 4.2)

- Periodic and adaptive cluster topology updates

- Publish/Subscribe

Connecting to a Redis Cluster requires one or more initial seed nodes.
The full cluster topology view (partitions) is obtained on the first
connection so you’re not required to specify all cluster nodes.
Specifying multiple seed nodes helps to improve resiliency as Lettuce is
able to connect the cluster even if a seed node is not available.
Lettuce holds multiple connections, which are opened on demand. You are
free to operate on these connections.

Connections can be bound to specific hosts or nodeIds. Connections bound
to a nodeId will always stick to the nodeId, even if the nodeId is
handled by a different host. Requests to unknown nodeId’s or host/ports
that are not part of the cluster are rejected. Do not close the
connections. Otherwise, unpredictable behavior will occur. Keep also in
mind that the node connections are used by the cluster connection itself
to perform cluster operations: If you block one connection all other
users of the cluster connection might be affected.

### Command routing

The [concept of Redis Cluster](https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/)
bases on sharding. Every primary node within the cluster handles one or
more slots. Slots are the [unit of
sharding](https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/#redis-cluster-data-sharding)
and calculated from the commands' key using `CRC16 MOD 16384`. Hash
slots can also be specified using hash tags such as `{user:1000}.foo`.

Every request, which incorporates at least one key is routed based on
its hash slot to the corresponding node. Commands without a key are
executed on the *default* connection that points most likely to the
first provided `RedisURI`. The same rule applies to commands operating
on multiple keys but with the limitation that all keys have to be in the
same slot. Commands operating on multiple slots will be terminated with
a `CROSSSLOT` error.

### Cross-slot command execution and cluster-wide execution for selected commands

Regular Redis Cluster commands are limited to single-slot keys operation
– either single key commands or multi-key commands that share the same
hash slot.

The cross slot limitation can be mitigated by using the advanced cluster
API for *a set of selected* multi-key commands. Commands that operate on
keys with different slots are decomposed into multiple commands. The
single commands are fired in a fork/join fashion. The commands are
issued concurrently to avoid synchronous chaining. Results are
synchronized before the command is completed.

Following commands are supported for cross-slot command execution:

- `DEL`: Delete the `KEY`s. Returns the number of keys that were
  removed.

- `EXISTS`: Count the number of `KEY`s that exist across the primary
  nodes being responsible for the particular key.

- `MGET`: Get the values of all given `KEY`s. Returns the values in the
  order of the keys.

- `MSET`: Set multiple key/value pairs for all given `KEY`s. Returns
  always `OK`.

- `TOUCH`: Alters the last access time of all given `KEY`s. Returns the
  number of keys that were touched.

- `UNLINK`: Delete the `KEY`s and reclaiming memory in a different
  thread. Returns the number of keys that were removed.

Following commands are executed on multiple cluster nodes operations:

- `CLIENT SETNAME`: Set the client name on all known cluster node
  connections. Returns always `OK`.

- `KEYS`: Return/Stream all keys that are stored on all primaries.

- `DBSIZE`: Return the number of keys that are stored on all primaries.

- `FLUSHALL`: Flush all data on the cluster primaries. Returns always
  `OK`.

- `FLUSHDB`: Flush all data on the cluster primaries. Returns always `OK`.

- `RANDOMKEY`: Return a random key from a random primary.

- `SCAN`: Scan the keyspace across the whole cluster according to
  `ReadFrom` settings.

- `SCRIPT FLUSH`: Remove all the scripts from the script cache on all
  cluster nodes.

- `SCRIPT LOAD`: Load the script into the Lua script cache on all nodes.

- `SCRIPT KILL`: Kill the script currently in execution on all cluster
  nodes. This call does not fail even if no scripts are running.

- `SHUTDOWN`: Synchronously save the dataset to disk and then shut down
  all nodes of the cluster.

Cross-slot command execution is available on the following APIs:

- `RedisAdvancedClusterCommands`

- `RedisAdvancedClusterAsyncCommands`

- `RedisAdvancedClusterReactiveCommands`

### Execution of commands on one or multiple cluster nodes

Sometimes commands have to be executed on multiple cluster nodes. The
advanced cluster API allows to select a set of nodes (e.g. all primaries,
all replicas) and trigger a command on this set.

``` java
RedisAdvancedClusterAsyncCommands<String, String> async = clusterClient.connect().async();
AsyncNodeSelection<String, String> replicas = connection.replicas();

AsyncExecutions<List<String>> executions = replicas.commands().keys("*");
executions.forEach(result -> result.thenAccept(keys -> System.out.println(keys)));
```

The commands are triggered concurrently. This API is currently only
available for async commands. Commands are dispatched to the nodes
within the selection, the result (CompletionStage) is available through
`AsyncExecutions`.

A node selection can be either dynamic or static. A dynamic node
selection updates its node set upon a [cluster topology view
refresh](#refreshing-the-cluster-topology-view). Node
selections can be constructed by the following presets:

- primaries

- replicas (operate on connections with activated `READONLY` mode)

- all nodes

A custom selection of nodes is available by implementing [custom
predicates](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/cluster/api/async/RedisAdvancedClusterAsyncCommands.html#nodes-java.util.function.Predicate-)
or lambdas.

The particular results map to a cluster node (`RedisClusterNode`) that
was involved in the node selection. You can obtain the set of involved
`RedisClusterNode`s and all results as `CompletableFuture` from
`AsyncExecutions`.

The node selection API is a technical preview and can change at any
time. That approach allows powerful operations but it requires further
feedback from the users. So feel free to contribute.

### Refreshing the cluster topology view

The Redis Cluster configuration may change at runtime. New nodes can be
added, the primary for a specific slot can change. Lettuce handles
`MOVED` and `ASK` redirects transparently but in case too many commands
run into redirects, you should refresh the cluster topology view. The
topology is bound to a `RedisClusterClient` instance. All cluster
connections that are created by one `RedisClusterClient` instance share
the same cluster topology view. The view can be updated in three ways:

1.  Either by calling `RedisClusterClient.reloadPartitions`

2.  [Periodic updates](advanced-usage.md#cluster-specific-options) in the background
    based on an interval

3.  [Adaptive updates](advanced-usage.md#cluster-specific-options) in the background
    based on persistent disconnects and `MOVED`/`ASK` redirections

By default, commands follow `-ASK` and `-MOVED` redirects [up to 5
times](advanced-usage.md#cluster-specific-options) until the command execution is
considered to be failed. Background topology updating starts with the
first connection obtained through `RedisClusterClient`.

### Connection Count for a Redis Cluster Connection Object

With Standalone Redis, a single connection object correlates with a
single transport connection. Redis Cluster works differently: A
connection object with Redis Cluster consists of multiple transport
connections. These are:

- Default connection object (Used for key-less commands and for Pub/Sub
  message publication)

- Connection per node (read/write connection to communicate with
  individual Cluster nodes)

- When using `ReadFrom`: Read-only connection per read replica node
  (read-only connection to read data from read replicas)

Connections are allocated on demand and not up-front to start with a
minimal set of connections. Formula to calculate the maximum number of
transport connections for a single connection object:

    1 + (N * 2)

Where `N` is the number of cluster nodes.

Apart of connection objects, `RedisClusterClient` uses additional
connections for topology refresh. These are created on topology refresh
and closed after obtaining the topology:

- Set of connections for cluster topology refresh (a connection to each
  cluster node)

### Client-options

See [Cluster-specific Client options](advanced-usage.md#cluster-specific-options).

#### Examples

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost").withPassword("authentication").build();

RedisClusterClient clusterClient = RedisClusterClient.create(redisUri);
StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
RedisAdvancedClusterCommands<String, String> syncCommands = connection.sync();

...

connection.close();
clusterClient.shutdown();
```

``` java
RedisURI node1 = RedisURI.create("node1", 6379);
RedisURI node2 = RedisURI.create("node2", 6379);

RedisClusterClient clusterClient = RedisClusterClient.create(Arrays.asList(node1, node2));
StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
RedisAdvancedClusterCommands<String, String> syncCommands = connection.sync();

...

connection.close();
clusterClient.shutdown();
```

``` java
RedisClusterClient clusterClient = RedisClusterClient.create(RedisURI.create("localhost", 6379));

ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                                .enablePeriodicRefresh(10, TimeUnit.MINUTES)
                                .build();

clusterClient.setOptions(ClusterClientOptions.builder()
                                .topologyRefreshOptions(topologyRefreshOptions)
                                .build());
...

clusterClient.shutdown();
```

``` java
RedisURI node1 = RedisURI.create("node1", 6379);
RedisURI node2 = RedisURI.create("node2", 6379);

RedisClusterClient clusterClient = RedisClusterClient.create(Arrays.asList(node1, node2));

ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                                .enableAdaptiveRefreshTrigger(RefreshTrigger.MOVED_REDIRECT, RefreshTrigger.PERSISTENT_RECONNECTS)
                                .adaptiveRefreshTriggersTimeout(30, TimeUnit.SECONDS)
                                .build();

clusterClient.setOptions(ClusterClientOptions.builder()
                                .topologyRefreshOptions(topologyRefreshOptions)
                                .build());
...

clusterClient.shutdown();
```

``` java
RedisURI node1 = RedisURI.create("node1", 6379);
RedisURI node2 = RedisURI.create("node2", 6379);

RedisClusterClient clusterClient = RedisClusterClient.create(Arrays.asList(node1, node2));
StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

RedisClusterCommands<String, String> node1 = connection.getConnection("host", 7379).sync();

...
// do not close node1

connection.close();
clusterClient.shutdown();
```

## ReadFrom Settings

The ReadFrom setting describes how Lettuce routes read operations to
replica nodes.

By default, Lettuce routes its read operations in multi-node connections
to the primary node. Reading from the primary returns the most recent
version of the data because write operations are issued to the single
primary node. Reading from primaries guarantees strong consistency.

You can reduce latency or improve read throughput by distributing reads
to replica members for applications that do not require fully up-to-date
data.

Be careful if using other ReadFrom settings than `PRIMARY`. Settings
other than `PRIMARY` may return stale data because the replication is
asynchronous. Data in the replicas may not hold the most recent data.

### Redis Cluster

Redis Cluster is a multi-node operated Redis setup that uses one or more
primary nodes and allows to setup replica nodes. Redis Cluster
connections allow to set a `ReadFrom` setting on connection level. This
setting applies for all read operations on this connection.

``` java
RedisClusterClient client = RedisClusterClient.create(RedisURI.create("host", 7379));
StatefulRedisClusterConnection<String, String> connection = client.connect();
connection.setReadFrom(ReadFrom.REPLICA);

RedisAdvancedClusterCommands<String, String> sync = connection.sync();
sync.set(key, "value");

sync.get(key); // replica read

connection.close();
client.shutdown();
```

### Primary/Replica connections (legacy master/slave)

Redis nodes can be operated in a Primary/Replica setup to achieve
availability and performance. Primary/Replica setups can be run either
Standalone or managed using Redis Sentinel. Lettuce allows to use
replica nodes for read operations by using the `PrimaryReplica` API that
supports both Primary/Replica setups:

1.  Redis Standalone Primary/Replica (no failover)

2.  Redis Sentinel Primary/Replica (Sentinel-managed failover)

The resulting connection uses in any case the primary connection-point
to dispatch non-read operations.

#### Redis Sentinel

Primary/Replica with Redis Sentinel is very similar to regular Redis
Sentinel operations. When the primary fails over, a replica is promoted
by Redis Sentinel to the new primary and the client obtains the new
topology from Redis Sentinel.

Connections to Primary/Replica require one or more Redis Sentinel
connection points and a primary name. The primary connection point is the
Sentinel monitored primary node.

``` java
RedisURI sentinelUri = RedisURI.Builder.sentinelPrimary("sentinel-host", 26379, "primary-name").build();
RedisClient client = RedisClient.create();

StatefulRedisPrimaryReplicaConnection<String, String> connection = PrimaryReplica.connect(
            client,
            StringCodec.UTF8
            sentinelUri);

connection.setReadFrom(ReadFrom.REPLICA);

connection.sync().get("key"); // Replica read

connection.close();
client.shutdown();
```

#### Redis Standalone

Primary/Replica with Redis Standalone is very similar to regular Redis
Standalone operations. A Redis Standalone Primary/Replica setup is static
and provides no built-in failover. Replicas are read from the Redis
primary node’s `INFO` command.

Connecting to Redis Standalone Primary/Replica nodes requires connections
to use the Redis primary for the `RedisURI`. The node used within the
`RedisURI` is the primary connection point.

``` java
RedisURI primaryUri = RedisURI.Builder.redis("primary-host", 6379).build();
RedisClient client = RedisClient.create();

StatefulRedisPrimaryReplicaConnection<String, String> connection = PrimaryReplica.connect(
            client,
            StringCodec.UTF8,
            primaryUri);

connection.setReadFrom(ReadFrom.REPLICA);

connection.sync().get("key"); // Replica read

connection.close();
client.shutdown();
```

### Use Cases for non-primary reads

The following use cases are common for using non-primary read settings
and encourage eventual consistency:

- Providing local reads for geographically distributed applications. If
  you have Redis and application servers in multiple data centers, you
  may consider having a geographically distributed cluster. Using the
  `LOWEST_LATENCY` setting allows the client to read from the
  lowest-latency members, rather than always reading from the primary
  node.

- Maintaining availability during a failover. Use `PRIMARY_PREFERRED` if
  you want an application to read from the primary by default, but to
  allow stale reads from replicas when the primary node is unavailable.
  `PRIMARY_PREFERRED` allows a "read-only mode" for your application
  during a failover.

- Increase read throughput by allowing stale reads If you want to
  increase your read throughput by adding additional replica nodes to
  your cluster Use `REPLICA` to read explicitly from replicas and reduce
  read load on the primary node. Using replica reads can highly lead to
  stale reads.

### Read from settings

All `ReadFrom` settings except `PRIMARY` may return stale data because
replicas replication is asynchronous and requires some delay. You need
to ensure that your application can tolerate stale data.

| Setting             | Description                                                                    |
|---------------------|--------------------------------------------------------------------------------|
| `PRIMARY`           | Default mode. Read from the current primary node.                              |
| `PRIMARY_PREFERRED` | Read from the primary, but if it is unavailable, read from replica nodes.      |
| `REPLICA`           | Read from replica nodes.                                                       |
| `REPLICA_PREFERRED` | Read from the replica nodes, but if none is unavailable, read from the primary. |
| `LOWEST_LATENCY`    | Read from any node of the cluster with the lowest latency.                     |
| `ANY`               | Read from any node of the cluster.                                             |
| `ANY_REPLICA`       | Read from any replica of the cluster.                                          |

!!! TIP
    The latency of the nodes is determined upon the cluster topology
    refresh. If the topology view is never refreshed, values from the
    initial cluster nodes read are used.

Custom read settings can be implemented by extending the
`io.lettuce.core.ReadFrom` class.
