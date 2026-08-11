# Client-Side Caching

Lettuce supports server-assisted client-side caching as described in the
[Redis client-side caching documentation](https://redis.io/docs/latest/develop/reference/client-side-caching/).
The `ClientSideCaching` utility creates a `CacheFrontend` that represents a
two-level cache: values are first looked up in a local, application-provided
cache and only fetched from Redis on a cache miss. Redis notifies the client
through `CLIENT TRACKING` invalidation messages when a cached key is modified,
and the corresponding local entry is evicted.

## Standalone connections

```java
Map<String, String> clientCache = new ConcurrentHashMap<>();

StatefulRedisConnection<String, String> connection = redisClient.connect();

CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
        TrackingArgs.Builder.enabled());

String value = frontend.get(key);
```

The `CacheFrontend` is associated with the Redis connection. Close the
frontend through `CacheFrontend.close()` to release the connection after use.

With RESP3, invalidation messages are delivered as push messages on the
connection itself. With RESP2, use `TrackingArgs` redirection
(`TrackingArgs.Builder.enabled().redirect(clientId)`) to deliver invalidations
to a Pub/Sub connection.

## Redis Cluster connections

Since version 7.7, client-side caching is also supported for Redis Cluster
connections:

```java
Map<String, String> clientCache = new ConcurrentHashMap<>();

StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();

CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
        TrackingArgs.Builder.enabled());

String value = frontend.get(key);
```

`CLIENT TRACKING` is enabled on each cluster node connection because
invalidation messages for a key are emitted by the node that serves the key's
slot. Upstream (master) nodes are tracked through their write-intent
connections. When a `ReadFrom` setting routes reads to replicas, the replicas
that the read policy can select are tracked through their read-intent
connections as well.

The following constraints apply to Redis Cluster:

- **RESP3 is required.** Enabling the cache fails with an
  `IllegalStateException` if a node connection did not negotiate RESP3.
- **`REDIRECT` is not supported** as invalidation messages can originate from
  any node. Redirected `TrackingArgs` are rejected with an
  `IllegalArgumentException`.
- **`OPTIN` is not supported** as the cache frontend does not issue
  `CLIENT CACHING yes` before reads. Opt-in `TrackingArgs` are rejected with an
  `IllegalArgumentException`.
- **Topology changes are not tracked.** Nodes added to the cluster after
  enabling the cache do not have tracking enabled. Applications that must
  observe topology changes should re-enable tracking after a topology refresh.

`FLUSHALL` and `FLUSHDB` emit a full invalidation, which clears the entire
client-side cache.
