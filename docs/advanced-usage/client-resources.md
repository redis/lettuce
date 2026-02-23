# Configuring Client resources

Client resources are configuration settings for the client related to
performance, concurrency, and events. A vast part of Client resources
consists of thread pools (`EventLoopGroup`s and a `EventExecutorGroup`)
which build the infrastructure for the connection workers. In general,
it is a good idea to reuse instances of `ClientResources` across
multiple clients.

Client resources are stateful and need to be shut down if they are
supplied from outside the client.

## Creating Client resources

Client resources are required to be immutable. You can create instances
using two different patterns:

**The `create()` factory method**

By using the `create()` method on `DefaultClientResources` you create
`ClientResources` with default settings:

``` java
ClientResources res = DefaultClientResources.create();
```

This approach fits the most needs.

**Resources builder**

You can build instances of `DefaultClientResources` by using the
embedded builder. It is designed to configure the resources to your
needs. The builder accepts the configuration in a fluent fashion and
then creates the ClientResources at the end:

``` java
ClientResources res = DefaultClientResources.builder()
                        .ioThreadPoolSize(4)
                        .computationThreadPoolSize(4)
                        .build()
```

## Using and reusing `ClientResources`

A `RedisClient` and `RedisClusterClient` can be created without passing
`ClientResources` upon creation. The resources are exclusive to the
client and are managed itself by the client. When calling `shutdown()`
of the client instance `ClientResources` are shut down.

``` java
RedisClient client = RedisClient.create();
...
client.shutdown();
```

If you require multiple instances of a client or you want to provide
existing thread infrastructure, you can configure a shared
`ClientResources` instance using the builder. The shared Client
resources can be passed upon client creation:

``` java
ClientResources res = DefaultClientResources.create();
RedisClient client = RedisClient.create(res);
RedisClusterClient clusterClient = RedisClusterClient.create(res, seedUris);
...
client.shutdown();
clusterClient.shutdown();
res.shutdown();
```

Shared `ClientResources` are never shut down by the client. Same applies
for shared `EventLoopGroupProvider`s that are an abstraction to provide
`EventLoopGroup`s.

### Why `Runtime.getRuntime().availableProcessors()` \* 3?

Netty requires different `EventLoopGroup`s for NIO (TCP) and for EPoll
(Unix Domain Socket) connections. One additional `EventExecutorGroup` is
used to perform computation tasks. `EventLoopGroup`s are started lazily
to allocate Threads on-demand.

### Shutdown

Every client instance requires a call to `shutdown()` to clear used
resources. Clients with dedicated `ClientResources` (i.e. no
`ClientResources` passed within the constructor/`create`-method) will
shut down `ClientResources` on their own.

Client instances with using shared `ClientResources` (i.e.
`ClientResources` passed using the constructor/`create`-method) won't
shut down the `ClientResources` on their own. The `ClientResources`
instance needs to be shut down once it's not used anymore.

## Configuration settings

The basic configuration options are listed in the table below:

| Name                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Method                       | Default     |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|-------------|
| **I/O Thread Pool Size**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | `ioThreadPoolSize`           | `See below` |
| The number of threads in the I/O thread pools. Every thread represents an internal event loop where all I/O tasks are run. The number does not reflect the actual number of I/O threads because the client requires different thread pools for Network (NIO) and Unix Domain Socket (EPoll) connections. The minimum I/O threads are `2`. |                              |             |
| **Computation Thread Pool Size**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | `computationThreadPoolSize` | `See below` |
| The number of threads in the computation thread pool. Every thread represents an internal event loop where all computation tasks are run. The minimum computation threads are `2`.                                                                                                                                                     |                              |             |


### Default thread pool size

Unless configured otherwise by the settings above, the number of threads (for both computation and I/O) is determined in the following order:
* if there is an environment variable setting for `io.netty.eventLoopThreads` we use it as default setting
* otherwise we take the number of available processors, retrieved by `Runtime.getRuntime().availableProcessors()` _(which, as a well-known fact, sometimes does not represent the actual number of processors)_
* in any case if the chosen number is lower than the minimum, which is `2` threads, then we use the minimum.

## Advanced settings

Values for the advanced options are listed in the table below and should
not be changed unless there is a truly good reason to do so.

<table style="width:97%;">
<colgroup>
<col style="width: 31%" />
<col style="width: 31%" />
<col style="width: 33%" />
</colgroup>
<thead>
<tr>
<th>Name</th>
<th>Method</th>
<th>Default</th>
</tr>
</thead>
<tbody>
<tr>
<td><strong>Provider for EventLoopGroup</strong></td>
<td><code>eventLoopGroupProvider</code></td>
<td><code>none</code></td>
</tr>
<tr>
<td colspan="3">For those who want to reuse existing netty infrastructure or the
total control over the thread pools, the
<code>EventLoopGroupProvider</code> API provides a way to do so.
<code>EventLoopGroup</code>s are obtained and managed by an
<code>EventLoopGroupProvider</code>. A provided
<code>EventLoopGroupProvider</code> is not managed by the client and
needs to be shut down once you no longer need the resources.</td>
</tr>
<tr>
<td><strong>Provided EventExecutorGroup</strong></td>
<td><code>eventExecutorGroup</code></td>
<td><code>none</code></td>
</tr>
<tr>
<td colspan="3">For those who want to reuse existing netty infrastructure or the
total control over the thread pools can provide an existing
<code>EventExecutorGroup</code> to the Client resources. A provided
<code>EventExecutorGroup</code> is not managed by the client and needs
to be shut down once you no longer need the resources.</td>
</tr>
<tr>
<td><strong>Event bus</strong></td>
<td><code>eventBus</code></td>
<td><code>DefaultEventBus</code></td>
</tr>
<tr>
<td colspan="3">The event bus system is used to transport events from the client to
subscribers. Events are about connection state changes, metrics, and
more. Events are published using a RxJava subject and the default
implementation drops events on backpressure. Learn more about the <a href="../user-guide/reactive-api.md">Reactive API</a>. You can also publish your own
events. If you wish to do so, make sure that your events implement the
<code>Event</code> marker interface.</td>
</tr>
<tr>
<td><strong>Command latency collector options</strong></td>
<td><code>commandLatencyCollectorOptions</code></td>
<td><code>DefaultCommandLatencyCollectorOptions</code></td>
</tr>
<tr>
<td colspan="3">The client can collect latency metrics during while dispatching
commands. The options allow configuring the percentiles, level of
metrics (per connection or server) and whether the metrics are
cumulative or reset after obtaining these. Command latency collection is
enabled by default and can be disabled by setting
<code>commandLatencyPublisherOptions(…)</code> to
<code>DefaultEventPublisherOptions.disabled()</code>. Latency
collector requires <code>LatencyUtils</code> to be on your class
path.</td>
</tr>
<tr>
<td><strong>Command latency collector</strong></td>
<td><code>commandLatencyCollector</code></td>
<td><code>DefaultCommandLatencyCollector</code></td>
</tr>
<tr>
<td colspan="3">The client can collect latency metrics during while dispatching
commands. Command latency metrics is collected on connection or server
level. Command latency collection is enabled by default and can be
disabled by setting <code>commandLatencyCollectorOptions(…)</code> to
<code>DefaultCommandLatencyCollectorOptions.disabled()</code>.</td>
</tr>
<tr>
<td><strong>Latency event publisher options</strong></td>
<td><code>commandLatencyPublisherOptions</code></td>
<td><code>DefaultEventPublisherOptions</code></td>
</tr>
<tr>
<td colspan="3">Command latencies can be published using the event bus. Latency
events are emitted by default every 10 minutes. Event publishing can be
disabled by setting <code>commandLatencyPublisherOptions(…)</code> to
<code>DefaultEventPublisherOptions.disabled()</code>.</td>
</tr>
<tr>
<td><strong>DNS Resolver</strong></td>
<td><code>dnsResolver</code></td>
<td><code>DnsResolvers.JVM_DEFAULT ( or netty if present)</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.5, 4.2. Deprecated: 6.4</p>
<p>Configures a DNS resolver to resolve hostnames to a
<code>java.net.InetAddress</code>. Defaults to the JVM DNS resolution
that uses blocking hostname resolution and caching of lookup results.
Users of DNS-based Redis-HA setups (e.g. AWS ElastiCache) might want to
configure a different DNS resolver. Lettuce comes with
<code>DirContextDnsResolver</code> that uses Java's
<code>DnsContextFactory</code> to resolve hostnames.
<code>DirContextDnsResolver</code> allows using either the system DNS
or custom DNS servers without caching of results so each hostname lookup
yields in a DNS lookup.</p>
<p>Since 4.4: Defaults to <code>DnsResolvers.UNRESOLVED</code> to use
netty's <code>AddressResolver</code> that resolves DNS names on
<code>Bootstrap.connect()</code> (requires netty 4.1)</p></td>
</tr>
<tr>
<td><strong>Address Resolver Group</strong></td>
<td><code>addressResolverGroup</code></td>
<td><code>DefaultAddressResolverGroup.INSTANCE ( or netty DnsAddressResolverGroup if present)</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 6.1</p>
<p>Sets the <code>AddressResolverGroup</code> for DNS resolution. This option is only effective if
<code>DnsResolvers#UNRESOLVED</code> is used as <code>DnsResolver</code>. Defaults to
<code>io.netty.resolver.DefaultAddressResolverGroup#INSTANCE</code> if <code>netty-dns-resolver</code>
is not available, otherwise defaults to <code>io.netty.resolver.dns.DnsAddressResolverGroup</code></p>
<p>Users of DNS-based Redis-HA setups (e.g. AWS ElastiCache) might want to configure a different DNS
resolver group. For example:

```java
new DnsAddressResolverGroup(
  new DnsNameResolverBuilder(dnsEventLoop)
      .channelType(NioDatagramChannel.class)
      .resolveCache(NoopDnsCache.INSTANCE)
      .cnameCache(NoopDnsCnameCache.INSTANCE)
      .authoritativeDnsServerCache(NoopAuthoritativeDnsServerCache.INSTANCE)
      .consolidateCacheSize(0)
);
```

</p>
</td>
</tr>
<tr>
<td><strong>Reconnect Delay</strong></td>
<td><code>reconnectDelay</code></td>
<td><code>Delay.exponential()</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.2</p>
<p>Configures a reconnect delay used to delay reconnect attempts.
Defaults to binary exponential delay with an upper boundary of
<code>30 SECONDS</code>. See <code>Delay</code> for more delay
implementations.</p></td>
</tr>
<tr>
<td><strong>Netty Customizer</strong></td>
<td><code>NettyCustomizer</code></td>
<td><code>none</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.4</p>
<p>Configures a netty customizer to enhance netty components. Allows
customization of <code>Bootstrap</code> after <code>Bootstrap</code>
configuration by Lettuce and <code>Channel</code> customization after
all Lettuce handlers are added to <code>Channel</code>. The customizer
allows custom SSL configuration (requires RedisURI in plain-text mode,
otherwise Lettuce configures SSL), adding custom handlers or setting
customized <code>Bootstrap</code> options. Misconfiguring
<code>Bootstrap</code> or <code>Channel</code> can cause connection
failures or undesired behavior.</p></td>
</tr>
<tr>
<td><strong>Tracing</strong></td>
<td><code>tracing</code></td>
<td><code>disabled</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 5.1</p>
<p>Configures a <code>tracing</code> instance to trace Redis calls.
Lettuce wraps Brave data models to support tracing in a vendor-agnostic
way if Brave is on the class path. A Brave <code>tracing</code> instance
can be created using <code>BraveTracing.create(clientTracing);</code>,
where <code>clientTracing</code> is a created or existent Brave tracing
instance .</p></td>
</tr>
</tbody>
</table>

