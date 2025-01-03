# Advanced usage

## Configuring Client resources

Client resources are configuration settings for the client related to
performance, concurrency, and events. A vast part of Client resources
consists of thread pools (`EventLoopGroup`s and a `EventExecutorGroup`)
which build the infrastructure for the connection workers. In general,
it is a good idea to reuse instances of `ClientResources` across
multiple clients.

Client resources are stateful and need to be shut down if they are
supplied from outside the client.

### Creating Client resources

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

### Using and reusing `ClientResources`

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

#### Why `Runtime.getRuntime().availableProcessors()` \* 3?

Netty requires different `EventLoopGroup`s for NIO (TCP) and for EPoll
(Unix Domain Socket) connections. One additional `EventExecutorGroup` is
used to perform computation tasks. `EventLoopGroup`s are started lazily
to allocate Threads on-demand.

#### Shutdown

Every client instance requires a call to `shutdown()` to clear used
resources. Clients with dedicated `ClientResources` (i.e. no
`ClientResources` passed within the constructor/`create`-method) will
shut down `ClientResources` on their own.

Client instances with using shared `ClientResources` (i.e.
`ClientResources` passed using the constructor/`create`-method) won’t
shut down the `ClientResources` on their own. The `ClientResources`
instance needs to be shut down once it’s not used anymore.

### Configuration settings

The basic configuration options are listed in the table below:

| Name                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Method                       | Default                |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|------------------------|
| **I/O Thread Pool Size**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | `ioThreadPoolSize`           | `Number of processors` |
| The number of threads in the I/O thread pools. The number defaults to the number of available processors that the runtime returns (which, as a well-known fact, sometimes does not represent the actual number of processors). Every thread represents an internal event loop where all I/O tasks are run. The number does not reflect the actual number of I/O threads because the client requires different thread pools for Network (NIO) and Unix Domain Socket (EPoll) connections. The minimum I/O threads are `3`. A pool with fewer threads can cause undefined behavior. |                              |                        |
| **Computation Thread Pool Size**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | `comput ationThreadPoolSize` | `Number of processors` |
| The number of threads in the computation thread pool. The number defaults to the number of available processors that the runtime returns (which, as a well-known fact, sometimes does not represent the actual number of processors). Every thread represents an internal event loop where all computation tasks are run. The minimum computation threads are `3`. A pool with fewer threads can cause undefined behavior.                                                                                                                                                        |                              |                        |

### Advanced settings

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
to be shut down once you do not longer need the resources.</td>
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
implementation drops events on backpressure. Learn more about the <a href="user-guide/reactive-api.md">Reactive API</a>. You can also publish your own
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
disabled by setting <code>commandLatency CollectorOptions(…)</code> to
<code>DefaultCom mandLatencyCollector Options.disabled()</code>.</td>
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
Users of DNS-based Redis-HA setups (e.g. AWS ElastiCache) might want to
configure a different DNS resolver. Lettuce comes with
<code>DirContextDnsResolver</code> that uses Java’s
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
<p>Users of DNS-based Redis-HA setups (e.g. AWS ElastiCache) might want to configure a different DNS 
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
otherwise Lettuce’s configures SSL), adding custom handlers or setting
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

## Client Options

Client options allow controlling behavior for some specific features.

Client options are immutable. Connections inherit the current options at
the moment the connection is created. Changes to options will not affect
existing connections.

``` java
client.setOptions(ClientOptions.builder()
                       .autoReconnect(false)
                       .pingBeforeActivateConnection(true)
                       .build());
```

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
<td>PING before activating connection</td>
<td><code>pingBeforeActivateConnection</code></td>
<td><code>true</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.1, 4.0</p>
<p>Perform a lightweight <code>PING</code> connection handshake when
establishing a Redis connection. If true (default is <code>true</code>),
every connection and reconnect will issue a <code>PING</code> command
and await its response before the connection is activated and enabled
for use. If the check fails, the connect/reconnect is treated as a
failure. This option has no effect unless forced to use the RESP 2
protocol version. RESP 3/protocol discovery performs a
<code>HELLO</code> handshake.</p>
<p>Failed <code>PING</code>'s on reconnect are handled as protocol
errors and can suspend reconnection if
<code>suspendReconnectOnProtocolFailure</code> is enabled.</p>
<p>The <code>PING</code> handshake validates whether the other end of
the connected socket is a service that behaves like a Redis
server.</p></td>
</tr>
<tr>
<td>Auto-Reconnect</td>
<td><code>autoReconnect</code></td>
<td><code>true</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.1, 4.0</p>
<p>Controls auto-reconnect behavior on connections. As soon as a
connection gets closed/reset without the intention to close it, the
client will try to reconnect, activate the connection and re-issue any
queued commands.</p>
<p>This flag also has the effect that disconnected connections will
refuse commands and cancel these with an exception.</p></td>
</tr>
<tr>
<td>Replay filter</td>
<td><code>replayFilter</code></td>
<td><code>(cmd) -> false</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 6.6</p>
<p>Controls which commands are to be filtered out in case the driver
attempts to reconnect to the server. Returning <code>false</code> means
that the command would not be filtered out.</p>
<p>This flag has no effect in case the autoReconnect feature is not
enabled.</p></td>
</tr>
<tr>
<td>Cancel commands on reconnect failure</td>
<td><code>cancelCommandsOnReconnectFailure</code></td>
<td><code>false</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.1, 4.0</p>
<p><strong>This flag is deprecated and should not be used as it can lead
to race conditions and protocol offsets. SSL is natively supported by
Lettuce and does no longer requires the use of SSL tunnels where
protocol traffic can get out of sync.</strong></p>
<p>If this flag is <code>true</code> any queued commands will be
canceled when a reconnect fails within the activation sequence. The
reconnect itself has two phases: Socket connection and
protocol/connection activation. In case a connect timeout occurs, a
connection reset, host lookup fails, this does not affect the
cancellation of commands. In contrast, where the protocol/connection
activation fails due to SSL errors or PING before activating connection
failure, queued commands are canceled.</p></td>
</tr>
<tr>
<td>Policy how to reclaim decode buffer memory</td>
<td><code>decodeBufferPolicy</code></td>
<td><code>ratio-based at 75%</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 6.0</p>
<p>Policy to discard read bytes from the decoding aggregation buffer to
reclaim memory. See <code>DecodeBufferPolicies</code> for available
strategies.</p></td>
</tr>
<tr>
<td>Suspend reconnect on protocol failure</td>
<td><code>suspendReconnectOnProtocolFailure</code></td>
<td><code>false (was introduced in 3. 1 with default true)</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.1, 4.0</p>
<p>If this flag is <code>true</code> the reconnect will be suspended on
protocol errors. The reconnect itself has two phases: Socket connection
and protocol/connection activation. In case a connect timeout occurs, a
connection reset, host lookup fails, this does not affect the
cancellation of commands. In contrast, where the protocol/connection
activation fails due to SSL errors or PING before activating connection
failure, queued commands are canceled.</p>
<p>Reconnection can be activated again, but there is no public API to
obtain the <code>ConnectionWatchdog</code> instance.</p></td>
</tr>
<tr>
<td>Request queue size</td>
<td><code>requestQueueSize</code></td>
<td><code>2147483647  (Integer#MAX_VALUE)</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.4, 4.1</p>
<p>Controls the per-connection request queue size. The command
invocation will lead to a <code>RedisException</code> if the queue size
is exceeded. Setting the <code>requestQueueSize</code> to a lower value
will lead earlier to exceptions during overload or while the connection
is in a disconnected state. A higher value means hitting the boundary
will take longer to occur, but more requests will potentially be queued,
and more heap space is used.</p></td>
</tr>
<tr>
<td>Disconnected behavior</td>
<td><code>disconnectedBehavior</code></td>
<td><code>DEFAULT</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.4, 4.1</p>
<p>A connection can behave in a disconnected state in various ways. The
auto-connect feature allows in particular to retrigger commands that
have been queued while a connection is disconnected. The disconnected
behavior setting allows fine-grained control over the behavior.
Following settings are available:</p>
<p><code>DEFAULT</code>: Accept commands when auto-reconnect is enabled,
reject commands when auto-reconnect is disabled.</p>
<p><code>ACCEPT_COMMANDS</code>: Accept commands in disconnected
state.</p>
<p><code>REJECT_COMMANDS</code>: Reject commands in disconnected
state.</p></td>
</tr>
<tr>
<td>Protocol Version</td>
<td><code>protocolVersion</code></td>
<td><code>Latest/Auto-discovery</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 6.0</p>
<p>Configuration of which protocol version (RESP2/RESP3) to use. Leaving
this option unconfigured performs a protocol discovery to use the
latest available protocol.</p></td>
</tr>
<tr>
<td>Script Charset</td>
<td><code>scriptCharset</code></td>
<td><code>UTF-8</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 6.0</p>
<p>Charset to use for Luascripts.</p></td>
</tr>
<tr>
<td>Socket Options</td>
<td><code>socketOptions</code></td>
<td><code>10 seconds Connecti on-Timeout, no keep-a live, no TCP noDelay</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.3</p>
<p>Options to configure low-level socket options for the connections
kept to Redis servers.</p></td>
</tr>
<tr>
<td>SSL Options</td>
<td><code>sslOptions</code></td>
<td><code>(non e), use JDK defaults</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.3</p>
<p>Configure SSL options regarding SSL providers (JDK/OpenSSL) and key
store/trust store.</p></td>
</tr>
<tr>
<td>Timeout Options</td>
<td><code>timeoutOptions</code></td>
<td><code>Do not timeout commands.</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 5.1</p>
<p>Options to configure command timeouts applied to timeout commands
after dispatching these (active connections, queued while disconnected,
batch buffer). By default, the synchronous API times out commands using
<code>RedisURI.getTimeout()</code>.</p></td>
</tr>
<tr>
<td>Publish Reactive Signals on Scheduler</td>
<td><code>publishOnScheduler</code></td>
<td><code>Use I/O thread.</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 5.1.4</p>
<p>Use a dedicated <code>Scheduler</code> to emit reactive data signals.
Enabling this option can be useful for reactive sequences that require a
significant amount of processing with a single/a few Redis connections
performance suffers from a single-thread-like behavior. Enabling this
option uses <code>EventExecutorGroup</code> configured through
<code>ClientResources</code> for data/completion signals. The used
<code>Thread</code> is sticky across all signals for a single
<code>Publisher</code> instance.</p></td>
</tr>
</tbody>
</table>

### Cluster-specific options

Cluster client options extend the regular client options by some cluster
specifics.

Cluster client options are immutable. Connections inherit the current
options at the moment the connection is created. Changes to options will
not affect existing connections.

``` java
ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(refreshPeriod(10, TimeUnit.MINUTES))
                .enableAllAdaptiveRefreshTriggers()
                .build();

client.setOptions(ClusterClientOptions.builder()
                       .topologyRefreshOptions(topologyRefreshOptions)
                       .build());
```

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
<td>Periodic cluster topology refresh</td>
<td><code>enablePeriodicRefresh</code></td>
<td><code>false</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.1, 4.0</p>
<p>Enables or disables periodic cluster topology refresh. The refresh is
handled in the background. Partitions, the view on the Redis cluster
topology, are valid for a whole <code>RedisClusterClient</code>
instance, not a connection. All connections created by this client
operate on the one cluster topology.</p>
<p>The refresh job is regularly executed, the period between the runs
can be set with <code>refreshPeriod</code>. The refresh job starts after
either opening the first connection with the job enabled or by calling
<code>reloadPartitions</code>. The job can be disabled without
discarding the full client by setting new client options.</p></td>
</tr>
<tr>
<td>Cluster topology refresh period</td>
<td><code>refreshPeriod</code></td>
<td><code>60 SECONDS</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.1, 4.0</p>
<p>Set the period between the refresh job runs. The effective interval
cannot be changed once the refresh job is active. Changes to the value
will be ignored.</p></td>
</tr>
<tr>
<td>Adaptive cluster topology refresh</td>
<td><code>enableAda ptiveRefreshTrigger</code></td>
<td><code>(none)</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.2</p>
<p>Enables selectively adaptive topology refresh triggers. Adaptive
refresh triggers initiate topology view updates based on events happened
during Redis Cluster operations. Adaptive triggers lead to an immediate
topology refresh. These refreshes are rate-limited using a timeout since
events can happen on a large scale. Adaptive refresh triggers are
disabled by default. Following triggers can be enabled:</p>
<p><code>MOVED_REDIRECT</code>, <code>ASK_REDIRECT</code>,
<code>PER SISTENT_RECONNECTS</code>, <code>UNKNOWN_NODE</code> (since
5.1), and <code>UNCOVERED_SLOT</code> (since 5.2) (see also reconnect
attempts for the reconnect trigger)</p></td>
</tr>
<tr>
<td>Adaptive refresh triggers timeout</td>
<td><code>adaptiveRef reshTriggersTimeout</code></td>
<td><code>30 SECONDS</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.2</p>
<p>Set the timeout between the adaptive refresh job runs. Multiple
triggers within the timeout will be ignored, only the first enabled
trigger leads to a topology refresh. The effective period cannot be
changed once the refresh job is active. Changes to the value will be
ignored.</p></td>
</tr>
<tr>
<td>Reconnect attempts (Adaptive topology refresh trigger)</td>
<td><code>refreshTrigge rsReconnectAttempts</code></td>
<td><code>5</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.2</p>
<p>Set the threshold for the <code>PE RSISTENT_RECONNECTS</code> refresh
trigger. Topology updates based on persistent reconnects lead only to a
refresh if the reconnect process tries at least the number of specified
attempts. The first reconnect attempt starts with
<code>1</code>.</p></td>
</tr>
<tr>
<td>Dynamic topology refresh sources</td>
<td><code>dy namicRefreshSources</code></td>
<td><code>true</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.2</p>
<p>Discover cluster nodes from the topology and use only the discovered
nodes as the source for the cluster topology. Using dynamic refresh will
query all discovered nodes for the cluster topology details. If set to
<code>false</code>, only the initial seed nodes will be used as sources
for topology discovery and the number of clients will be obtained only
for the initial seed nodes. This can be useful when using Redis Cluster
with many nodes.</p>
<p>Note that enabling dynamic topology refresh sources uses node
addresses reported by Redis <code>CLUSTER NODES</code> output which
typically contains IP addresses.</p></td>
</tr>
<tr>
<td>Close stale connections</td>
<td><code>cl oseStaleConnections</code></td>
<td><code>true</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.3, 4.1</p>
<p>Stale connections are existing connections to nodes which are no
longer part of the Redis Cluster. If this flag is set to
<code>true</code>, then stale connections are closed upon topology
refreshes. It’s strongly advised to close stale connections as open
connections will attempt to reconnect nodes if the node is no longer
available and open connections require system resources.</p></td>
</tr>
<tr>
<td>Limitation of cluster redirects</td>
<td><code>maxRedirects</code></td>
<td><code>5</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.1, 4.0</p>
<p>When the assignment of a slot-hash is moved in a Redis Cluster and a
client requests a key that is located on the moved slot-hash, the
Cluster node responds with a <code>-MOVED</code> response. In this case,
the client follows the redirection and queries the cluster specified
within the redirection. Under some circumstances, the redirection can be
endless. To protect the client and also the Cluster, a limit of max
redirects can be configured. Once the limit is reached, the
<code>-MOVED</code> error is returned to the caller. This limit also
applies for <code>-ASK</code> redirections in case a slot is set to
<code>MIGRATING</code> state.</p></td>
</tr>
<tr>
<td>Filter nodes from Topology</td>
<td><code>nodeFilter</code></td>
<td><code>no filter</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 6.1.6</p>
<p>When providing a <code>nodeFilter</code>, then
<code>RedisClusterNode</code>s can be filtered from the topology view to
remove unwanted nodes (e.g. failed replicas). Note that the filter is
applied only after obtaining the topology so the filter does not prevent
trying to connect the node during topology discovery.</p></td>
</tr>
<tr>
<td>Validate cluster node membership</td>
<td><code>validateCl usterNodeMembership</code></td>
<td><code>true</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.3, 4.0</p>
<p>Validate the cluster node membership before allowing connections to
that node. The current implementation performs redirects using
<code>MOVED</code> and <code>ASK</code> and allows obtaining connections
to the particular cluster nodes. The validation was introduced during
the development of version 3.3 to prevent security breaches and only
allow connections to the known hosts of the <code>CLUSTER NODES</code>
output.</p>
<p>There are some scenarios, where the strict validation is an
obstruction:</p>
<p><code>MOVED</code>/<code>ASK</code> redirection but the cluster
topology view is stale Connecting to cluster nodes using different
IPs/hostnames (e.g. private/public IPs)</p>
<p>Connecting to non-cluster members to reconfigure those while using
the RedisClusterClient connection.</p></td>
</tr>
</tbody>
</table>

### Request queue size and cluster

Clustered operations use multiple connections. The resulting
overall-queue limit is
`requestQueueSize * ((number of cluster nodes * 2) + 1)`.

## SSL Connections

Lettuce supports SSL connections since version 3.1 on Redis Standalone
connections and since version 4.2 on Redis Cluster. [Redis supports SSL since version 6.0](https://redis.io/docs/latest/operate/oss_and_stack/management/security/encryption/).

First, you need to [enable SSL on your Redis server](https://redis.io/docs/latest/operate/oss_and_stack/management/security/encryption/).

Next step is connecting lettuce over SSL to Redis.

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost")
                                 .withSsl(true)
                                 .withPassword("authentication")
                                 .withDatabase(2)
                                 .build();

RedisClient client = RedisClient.create(redisUri);
```

``` java
RedisURI redisUri = RedisURI.create("rediss://authentication@localhost/2");
RedisClient client = RedisClient.create(redisUri);
```

``` java
RedisURI redisUri = RedisURI.Builder.redis("localhost")
                                 .withSsl(true)
                                 .withPassword("authentication")
                                 .build();

RedisClusterClient client = RedisClusterClient.create(redisUri);
```

### Limitations

Lettuce supports SSL only on Redis Standalone and Redis Cluster
connections and since 5.2, also for Master resolution using Redis
Sentinel or Redis Master/Replicas.

### Connection Procedure and Reconnect

When connecting using SSL, Lettuce performs an SSL handshake before you
can use the connection. Plain text connections do not perform a
handshake. Errors during the handshake throw
`RedisConnectionException`s.

Reconnection behavior is also different to plain text connections. If an
SSL handshake fails on reconnect (because of peer/certification
verification or peer does not talk SSL) reconnection will be disabled
for the connection. You will also find an error log entry within your
logs.

### Certificate Chains/Root Certificate/Self-Signed Certificates

Lettuce uses Java defaults for the trust store that is usually `cacerts`
in your `jre/lib/security` directory and comes with customizable SSL
options via [client options](#client-options). If you need to add you
own root certificate, so you can configure `SslOptions`, import it
either to `cacerts` or you provide an own trust store and set the
necessary system properties:

``` java
SslOptions sslOptions = SslOptions.builder()
        .jdkSslProvider()
        .truststore(new File("yourtruststore.jks"), "changeit")
        .build();

ClientOptions clientOptions = ClientOptions.builder().sslOptions(sslOptions).build();
```

``` java
System.setProperty("javax.net.ssl.trustStore", "yourtruststore.jks");
System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
```

### Host/Peer Verification

By default, Lettuce verifies the certificate against the validity and
the common name (Name validation not supported on Java 1.6, only
available on Java 1.7 and higher) of the Redis host you are connecting
to. This behavior can be turned off:

``` java
RedisURI redisUri = ...
redisUri.setVerifyPeer(false);
```

or

``` java
RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort())
                                 .withSsl(true)
                                 .withVerifyPeer(false)
                                 .build();
```

### StartTLS

If you need to issue a StartTLS before you can use SSL, set the
`startTLS` property of `RedisURI` to `true`. StartTLS is disabled by
default.

``` java
RedisURI redisUri = ...
redisUri.setStartTls(true);
```

or

``` java
RedisURI redisUri = RedisURI.Builder.redis(host(), sslPort())
                                 .withSsl(true)
                                 .withStartTls(true)
                                 .build();
```

## Native Transports

Netty provides three platform-specific JNI transports:

- epoll on Linux

- io_uring on Linux (Incubator)

- kqueue on macOS/BSD

Lettuce defaults to native transports if the appropriate library is
available within its runtime. Using a native transport adds features
specific to a particular platform, generate less garbage and generally
improve performance when compared to the NIO based transport. Native
transports are required to connect to Redis via [Unix Domain
Sockets](#unix-domain-sockets) and are suitable for TCP connections as
well.

Native transports are available with:

- Linux **epoll** x86_64 systems with a minimum netty version of
  `4.0.26.Final`, requiring `netty-transport-native-epoll`, classifier
  `linux-x86_64`

  ``` xml
  <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-transport-native-epoll</artifactId>
      <version>${netty-version}</version>
      <classifier>linux-x86_64</classifier>
  </dependency>
  ```

- Linux **io_uring** x86_64 systems with a minimum netty version of
  `4.1.54.Final`, requiring `netty-incubator-transport-native-io_uring`,
  classifier `linux-x86_64`. Note that this transport is still
  experimental.

  ``` xml
  <dependency>
      <groupId>io.netty.incubator</groupId>
      <artifactId>netty-incubator-transport-native-io_uring</artifactId>
      <version>0.0.1.Final</version>
      <classifier>linux-x86_64</classifier>
  </dependency>
  ```

- macOS **kqueue** x86_64 systems with a minimum netty version of
  `4.1.11.Final`, requiring `netty-transport-native-kqueue`, classifier
  `osx-x86_64`

  ``` xml
  <dependency>
      <groupId>io.netty</groupId>
      <artifactId>netty-transport-native-kqueue</artifactId>
      <version>${netty-version}</version>
      <classifier>osx-x86_64</classifier>
  </dependency>
  ```

You can disable native transport use through system properties. Set
`io.lettuce.core.epoll`, `io.lettuce.core.iouring` respective
`io.lettuce.core.kqueue` to `false` (default is `true`, if unset).

### Limitations

Native transport support does not work with the shaded version of
Lettuce because of two reasons:

1.  `netty-transport-native-epoll` and `netty-transport-native-kqueue`
    are not packaged into the shaded jar. So adding the jar to the
    classpath will resolve in different netty base classes (such as
    `io.netty.channel.EventLoopGroup` instead of
    `com.lambdaworks.io.netty.channel.EventLoopGroup`)

2.  Support for using epoll/kqueue with shaded netty requires netty 4.1
    and all parts of netty to be shaded.

See also Netty [documentation on native
transports](http://netty.io/wiki/native-transports.html).

## Unix Domain Sockets

Lettuce supports since version 3.2 Unix Domain Sockets for local Redis
connections.

``` java
RedisURI redisUri = RedisURI.Builder
                             .socket("/tmp/redis")
                             .withPassword("authentication")
                             .withDatabase(2)
                             .build();

RedisClient client = RedisClient.create(redisUri);
```

``` java
RedisURI redisUri = RedisURI.create("redis-socket:///tmp/redis");
RedisClient client = RedisClient.create(redisUri);
```

Unix Domain Sockets are inter-process communication channels on POSIX
compliant systems. They allow exchanging data between processes on the
same host operating system. When using Redis, which is usually a network
service, Unix Domain Sockets are usable only if connecting locally to a
single instance. Redis Sentinel and Redis Cluster, maintain tables of
remote or local nodes and act therefore as a registry. Unix Domain
Sockets are not beneficial with Redis Sentinel and Redis Cluster.

Using `RedisClusterClient` with Unix Domain Sockets would connect to the
local node using a socket and open TCP connections to all the other
hosts. A good example is connecting locally to a standalone or a single
cluster node to gain performance.

See [Native Transports](#native-transports) for more details and
limitations.

## Streaming API

Redis can contain a huge set of data. Collections can burst your memory,
when the amount of data is too massive for your heap. Lettuce can return
your collection data either as List/Set/Map or can push the data on
`StreamingChannel` interfaces.

`StreamingChannel`s are similar to callback methods. Every method, which
can return bulk data (except transactions/multi and some config methods)
specifies beside a regular method with a collection return class also
method which accepts a `StreamingChannel`. Lettuce interacts with a
`StreamingChannel` as the data arrives so data can be processed while
the command is running and is not yet completed.

There are 4 StreamingChannels accepting different data types:

- [KeyStreamingChannel](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/output/KeyStreamingChannel.html)

- [ValueStreamingChannel](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/output/ValueStreamingChannel.html)

- [KeyValueStreamingChannel](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/output/KeyValueStreamingChannel.html)

- [ScoredValueStreamingChannel](https://www.javadoc.io/static/io.lettuce/lettuce-core/6.4.0.RELEASE/io/lettuce/core/output/ScoredValueStreamingChannel.html)

The result of the steaming methods is the count of keys/values/key-value
pairs as `long` value.

!!! NOTE
    Don’t issue blocking calls (includes synchronous API calls to Lettuce)
    from inside of callbacks such as the streaming API as this would block
    the EventLoop. If you need to fetch data from Redis from inside a
    `StreamingChannel` callback, please use the asynchronous API or use
    the reactive API directly.

``` java
Long count = redis.hgetall(new KeyValueStreamingChannel<String, String>()
    {
        @Override
        public void onKeyValue(String key, String value)
        {
            ...
        }
    }, key);
```

Streaming happens real-time to the redis responses. The method call
(future) completes after the last call to the StreamingChannel.

### Examples

``` java
redis.lpush("key", "one")
redis.lpush("key", "two")
redis.lpush("key", "three")

Long count = redis.lrange(new ValueStreamingChannel<String, String>()
    {
        @Override
        public void onValue(String value)
        {
            System.out.println("Value: " + value);
        }
    }, "key",  0, -1);

System.out.println("Count: " + count);
```

will produce the following output:

    Value: one
    Value: two
    Value: three
    Count: 3

## Events

### Before 3.4/4.1

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

### Since 3.4/4.1

The client produces events during its operation and uses an event bus
for the transport. The `EventBus` can be configured and obtained from
the [client resources](#configuring-client-resources) and is used for
client- and custom events.

Following events are sent by the client:

- Connection events

- Metrics events

- Cluster topology events

#### Subscribing to events

The simple-most approach to subscribing to the client events is
obtaining the event bus from the client’s client resources.

``` java
RedisClient client = RedisClient.create()
EventBus eventBus = client.getresources().eventBus();

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
resources](#configuring-client-resources)) transports the events.

#### Connection events

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
    the reconnect failure and and the retry counter. Event type:
    `ReconnectFailedEvent`

#### Metrics events

Client command metrics is published using the event bus. The current
event carries command latency metrics. Latency metrics is segregated by
connection or server and command which means you can get detailed
statistics on every command. Connection distinction allows seeing how
particular connections perform. Server distinction how particular
servers perform. You can configure metrics collection using [client
resources](#configuring-client-resources).

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

#### Cluster events

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

### Java Flight Recorder Events (since 6.1)

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

## Observability

The following section explains Lettuces metrics and tracing
capabilities.

### Metrics

Command latency metrics give insight into command execution and
latencies. Metrics are collected for every completed command. Lettuce
has two mechanisms to collect latency metrics:

- [Built-in](#built-in-latency-tracking) (since version 3.4 using
  HdrHistogram and LatencyUtils. Enabled by default if both libraries
  are available on the classpath.)

- [Micrometer](#micrometer) (since version 6.1)

### Built-in latency tracking

Each command is tracked with:

- Execution count

- Latency to first response (min, max, percentiles)

- Latency to complete (min, max, percentiles)

Command latencies are tracked on remote endpoint (distinction by host
and port or socket path) and command type level (`GET`, `SET`, …​). It is
possible to track command latencies on a per-connection level (see
`DefaultCommandLatencyCollectorOptions`).

Command latencies are transported using Events on the `EventBus`. The
`EventBus` can be obtained from the [client
resources](#configuring-client-resources) of the client instance. Please
keep in mind that the `EventBus` is used for various event types. Filter
on the event type if you’re interested only in particular event types.

``` java
RedisClient client = RedisClient.create();
EventBus eventBus = client.getResources().eventBus();

Subscription subscription = eventBus.get()
                .filter(redisEvent -> redisEvent instanceof CommandLatencyEvent)
                .cast(CommandLatencyEvent.class)
                .subscribe(e -> System.out.println(e.getLatencies()));
```

The `EventBus` uses Reactor Processors to publish events. This example
prints the received latencies to `stdout`. The interval and the
collection of command latency metrics can be configured in the
`ClientResources`.

#### Prerequisites

Lettuce requires the LatencyUtils dependency (at least 2.0) to provide
latency metrics. Make sure to include that dependency on your classpath.
Otherwise, you won’t be able using latency metrics.

If using Maven, add the following dependency to your pom.xml:

``` xml
<dependency>
    <groupId>org.latencyutils</groupId>
    <artifactId>LatencyUtils</artifactId>
    <version>2.0.3</version>
</dependency>
```

#### Disabling command latency metrics

To disable metrics collection, use own `ClientResources` with a disabled
`DefaultCommandLatencyCollectorOptions`:

``` java
ClientResources res = DefaultClientResources
        .builder()
        .commandLatencyCollectorOptions( DefaultCommandLatencyCollectorOptions.disabled())
        .build();

RedisClient client = RedisClient.create(res);
```

#### CommandLatencyCollector Options

The following settings are available to configure from
`DefaultCommandLatencyCollectorOptions`:

| Name                                                                                                                                                                                                                                                                                                                                                                                  | Method                      | Default                         |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------|---------------------------------|
| **Disable metrics tracking**                                                                                                                                                                                                                                                                                                                                                          | `disable`                   | `false`                         |
| Disables tracking of command latency metrics.                                                                                                                                                                                                                                                                                                                                         |                             |                                 |
| **Latency time unit**                                                                                                                                                                                                                                                                                                                                                                 | `targetUnit`                | `MICROSECONDS`                  |
| The target unit for command latency values. All values in the `CommandLatencyEvent` and a `CommandMetrics` instance are `long` values scaled to the `targetUnit`.                                                                                                                                                                                                                     |                             |                                 |
| **Latency percentiles**                                                                                                                                                                                                                                                                                                                                                               | `targetPercentiles`         | `50.0, 90 .0, 95.0, 99.0, 99.9` |
| A `double`-array of percentiles for latency metrics. The `CommandMetrics` contains a map that holds the percentile value and the latency value according to the percentile. Note that percentiles here must be specified in the range between 0 and 100.                                                                                                                              |                             |                                 |
| **Reset latencies after publish**                                                                                                                                                                                                                                                                                                                                                     | `reset LatenciesAfterEvent` | `true`                          |
| Allows controlling whether the latency metrics are reset to zero one they were published. Setting `reset LatenciesAfterEvent` allows accumulating metrics over a long period for long-term analytics.                                                                                                                                                                                 |                             |                                 |
| **Local socket distinction**                                                                                                                                                                                                                                                                                                                                                          | `localDistinction`          | `false`                         |
| Enables per connection metrics tracking instead of per host/port. If `true`, multiple connections to the same host/connection point will be recorded separately which allows to inspection of every connection individually. If `false`, multiple connections to the same host/connection point will be recorded together. This allows a consolidated view on one particular service. |                             |                                 |

#### EventPublisher Options

The following settings are available to configure from
`DefaultEventPublisherOptions`:

| Name                                              | Method                   | Default   |
|---------------------------------------------------|--------------------------|-----------|
| **Disable event publisher**                       | `disable`                | `false`   |
| Disables event publishing.                        |                          |           |
| **Event publishing time unit**                    | `ev entEmitIntervalUnit` | `MINUTES` |
| The `TimeUnit` for the event publishing interval. |                          |           |
| **Event publishing interval**                     | `eventEmitInterval`      | `10`      |
| The interval for the event publishing.            |                          |           |

### Micrometer

Commands are tracked by using two Micrometer `Timer`s:
`lettuce.command.firstresponse` and `lettuce.command.completion`. The
following tags are attached to each timer:

- `command`: Name of the command (`GET`, `SET`, …​)

- `local`: Local socket (`localhost/127.0.0.1:45243` or `ANY` when local
  distinction is disabled, which is the default behavior)

- `remote`: Remote socket (`localhost/127.0.0.1:6379`)

Command latencies are reported using the provided `MeterRegistry`.

``` java
MeterRegistry meterRegistry = …;
MicrometerOptions options = MicrometerOptions.create();
ClientResources resources = ClientResources.builder().commandLatencyRecorder(new MicrometerCommandLatencyRecorder(meterRegistry, options)).build();

RedisClient client = RedisClient.create(resources);
```

#### Prerequisites

Lettuce requires Micrometer (`micrometer-core`) to integrate with
Micrometer.

If using Maven, add the following dependency to your pom.xml:

``` xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>${micrometer.version}</version> <!-- e.g. micrometer.version==1.6.0 -->
</dependency>
```

#### Micrometer Options

The following settings are available to configure from
`MicrometerOptions`:

| Name                                                                                                                                                                                                                                                                                                                                                                               | Method              | Default                                                                            |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|------------------------------------------------------------------------------------|
| **Disable metrics tracking**                                                                                                                                                                                                                                                                                                                                                       | `disable`           | `false`                                                                            |
| Disables tracking of command latency metrics.                                                                                                                                                                                                                                                                                                                                      |                     |                                                                                    |
| **Histogram**                                                                                                                                                                                                                                                                                                                                                                      | `histogram`         | `false`                                                                            |
| Enable histogram buckets used to generate aggregable percentile approximations in monitoring systems that have query facilities to do so.                                                                                                                                                                                                                                          |                     |                                                                                    |
| **Local socket distinction**                                                                                                                                                                                                                                                                                                                                                       | `localDistinction`  | `false`                                                                            |
| Enables per connection metrics tracking instead of per host/port. If `true`, multiple connections to the same host/connection point will be recorded separately which allows inspection of every connection individually. If `false`, multiple connections to the same host/connection point will be recorded together. This allows a consolidated view on one particular service. |                     |                                                                                    |
| **Maximum Latency**                                                                                                                                                                                                                                                                                                                                                                | `maxLatency`        | `5 Minutes`                                                                        |
| Sets the maximum value that this timer is expected to observe. Applies only if Histogram publishing is enabled.                                                                                                                                                                                                                                                                    |                     |                                                                                    |
| **Minimum Latency**                                                                                                                                                                                                                                                                                                                                                                | `minLatency`        | `1ms`                                                                              |
| Sets the minimum value that this timer is expected to observe. Applies only if Histogram publishing is enabled.                                                                                                                                                                                                                                                                    |                     |                                                                                    |
| **Additional Tags**                                                                                                                                                                                                                                                                                                                                                                | `tags`              | `Tags.empty()`                                                                     |
| Extra tags to add to the generated metrics.                                                                                                                                                                                                                                                                                                                                        |                     |                                                                                    |
| **Latency percentiles**                                                                                                                                                                                                                                                                                                                                                            | `targetPercentiles` | `0.5, 0.9, 0.95,  0.99, 0.999 (corresp onding with 50.0, 90. 0, 95.0, 99.0, 99.9)` |
| A `double`-array of percentiles for latency metrics. Values must be supplied in the range of `0.0` (0th percentile) up to `1.0` (100th percentile). The `CommandMetrics` contains a map that holds the percentile value and the latency value according to the percentile. This applies only if Histogram publishing is enabled.                                                   |                     |                                                                                    |

### Tracing

Tracing gives insights about individual Redis commands sent to Redis to
trace their frequency, duration and to trace of which commands a
particular activity consists. Lettuce provides a tracing SPI to avoid
mandatory tracing library dependencies. Lettuce ships integrations with
[Micrometer Tracing](https://github.com/micrometer-metrics/tracing) and
[Brave](https://github.com/openzipkin/brave) which can be configured
through [client resources](#configuring-client-resources).

#### Micrometer Tracing

With Micrometer tracing enabled, Lettuce creates an observation for each
Redis command resulting in spans per Command and corresponding Meters if
configured in Micrometer’s `ObservationContext`.

##### Prerequisites

Lettuce requires the Micrometer Tracing dependency to provide Tracing
functionality. Make sure to include that dependency on your classpath.

If using Maven, add the following dependency to your pom.xml:

``` xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>
```

The following example shows how to configure tracing through
`ClientResources`:

``` java
ObservationRegistry observationRegistry = …;

MicrometerTracing tracing = new MicrometerTracing(observationRegistry, "Redis");

ClientResources resources = ClientResources.builder().tracing(tracing).build();
```

#### Brave

With Brave tracing enabled, Lettuce creates a span for each Redis
command. The following options can be configured:

- `serviceName` (defaults to `redis`).

- `Endpoint` customizer. This option can be used together with a custom
  `SocketAddressResolver` to attach custom endpoint details.

- `Span` customizer. Allows for customization of spans based on the
  actual Redis `Command` object.

- Inclusion/Exclusion of all command arguments in a span. By default,
  all arguments are included.

##### Prerequisites

Lettuce requires the Brave dependency (at least 5.1) to provide Tracing
functionality. Make sure to include that dependency on your classpath.

If using Maven, add the following dependency to your pom.xml:

``` xml
<dependency>
    <groupId>io.zipkin.brave</groupId>
    <artifactId>brave</artifactId>
</dependency>
```

The following example shows how to configure tracing through
`ClientResources`:

``` java
brave.Tracing clientTracing = …;

BraveTracing tracing = BraveTracing.builder().tracing(clientTracing)
    .excludeCommandArgsFromSpanTags()
    .serviceName("custom-service-name-goes-here")
    .spanCustomizer((command, span) -> span.tag("cmd", command.getType().name()))
    .build();

ClientResources resources = ClientResources.builder().tracing(tracing).build();
```

Lettuce ships with a Tracing SPI in `io.lettuce.core.tracing` that
allows custom tracer implementations.

## Pipelining and command flushing

Redis is a TCP server using the client-server model and what is called a
Request/Response protocol. This means that usually a request is
accomplished with the following steps:

- The client sends a query to the server and reads from the socket,
  usually in a blocking way, for the server response.

- The server processes the command and sends the response back to the
  client.

A request/response server can be implemented so that it is able to
process new requests even if the client did not already read the old
responses. This way it is possible to send multiple commands to the
server without waiting for the replies at all, and finally read the
replies in a single step.

Using the synchronous API, in general, the program flow is blocked until
the response is accomplished. The underlying connection is busy with
sending the request and receiving its response. Blocking, in this case,
applies only from a current Thread perspective, not from a global
perspective.

To understand why using a synchronous API does not block on a global
level we need to understand what this means. Lettuce is a non-blocking
and asynchronous client. It provides a synchronous API to achieve a
blocking behavior on a per-Thread basis to create await (synchronize) a
command response. Blocking does not affect other Threads per se. Lettuce
is designed to operate in a pipelining way. Multiple threads can share
one connection. While one Thread may process one command, the other
Thread can send a new command. As soon as the first request returns, the
first Thread’s program flow continues, while the second request is
processed by Redis and comes back at a certain point in time.

Lettuce is built on top of netty decouple reading from writing and to
provide thread-safe connections. The result is, that reading and writing
can be handled by different threads and commands are written and read
independent of each other but in sequence. You can find more details
about [message ordering](#message-ordering) to learn
about command ordering rules in single- and multi-threaded arrangements.
The transport and command execution layer does not block the processing
until a command is written, processed and while its response is read.
Lettuce sends commands at the moment they are invoked.

A good example is the [async API](user-guide/async-api.md). Every
invocation on the [async API](user-guide/async-api.md) returns a
`Future` (response handle) after the command is written to the netty
pipeline. A write to the pipeline does not mean, the command is written
to the underlying transport. Multiple commands can be written without
awaiting the response. Invocations to the API (sync, async and starting
with `4.0` also reactive API) can be performed by multiple threads.

Sharing a connection between threads is possible but keep in mind:

**The longer commands need for processing, the longer other invoker wait
for their results**

You should not use transactional commands (`MULTI`) on shared
connection. If you use Redis-blocking commands (e. g. `BLPOP`) all
invocations of the shared connection will be blocked until the blocking
command returns which impacts the performance of other threads. Blocking
commands can be a reason to use multiple connections.

### Command flushing

!!! NOTE
    Command flushing is an advanced topic and in most cases (i.e. unless
    your use-case is a single-threaded mass import application) you won’t
    need it as Lettuce uses pipelining by default.

The normal operation mode of Lettuce is to flush every command which
means, that every command is written to the transport after it was
issued. Any regular user desires this behavior. You can control command
flushing since Version `3.3`.

Why would you want to do this? A flush is an [expensive system
call](https://github.com/netty/netty/issues/1759) and impacts
performance. Batching, disabling auto-flushing, can be used under
certain conditions and is recommended if:

- You perform multiple calls to Redis and you’re not depending
  immediately on the result of the call

- You’re bulk-importing

Controlling the flush behavior is only available on the async API. The
sync API emulates blocking calls, and as soon as you invoke a command,
you can no longer interact with the connection until the blocking call
ends.

The `AutoFlushCommands` state is set per connection and, therefore
visible to all threads using a shared connection. If you want to omit
this effect, use dedicated connections. The `AutoFlushCommands` state
cannot be set on pooled connections by the Lettuce connection pooling.

!!! WARNING
    Do not use `setAutoFlushCommands(…)` when sharing a connection across
    threads, at least not without proper synchronization. According to the
    many questions and (invalid) bug reports using
    `setAutoFlushCommands(…)` in a multi-threaded scenario causes a lot of
    complexity overhead and is very likely to cause issues on your side.
    `setAutoFlushCommands(…)` can only be reliably used on single-threaded
    connection usage in scenarios like bulk-loading.

``` java
StatefulRedisConnection<String, String> connection = client.connect();
RedisAsyncCommands<String, String> commands = connection.async();

// disable auto-flushing
commands.setAutoFlushCommands(false);

// perform a series of independent calls
List<RedisFuture<?>> futures = Lists.newArrayList();
for (int i = 0; i < iterations; i++) {
    futures.add(commands.set("key-" + i, "value-" + i));
    futures.add(commands.expire("key-" + i, 3600));
}

// write all commands to the transport layer
commands.flushCommands();

// synchronization example: Wait until all futures complete
boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS,
                   futures.toArray(new RedisFuture[futures.size()]));

// later
connection.close();
```

#### Performance impact

Commands invoked in the default flush-after-write mode perform in an
order of about 100Kops/sec (async/multithreaded execution). Grouping
multiple commands in a batch (size depends on your environment, but
batches between 50 and 1000 work nice during performance tests) can
increase the throughput up to a factor of 5x.

Pipelining within the Redis docs: <https://redis.io/docs/latest/develop/use/pipelining/>

## Connection Pooling

Lettuce connections are designed to be thread-safe so one connection can
be shared amongst multiple threads and Lettuce connections
[auto-reconnection](#client-options) by default. While connection
pooling is not necessary in most cases it can be helpful in certain use
cases. Lettuce provides generic connection pooling support.

### Is connection pooling necessary?

Lettuce is thread-safe by design which is sufficient for most cases. All
Redis user operations are executed single-threaded. Using multiple
connections does not impact the performance of an application in a
positive way. The use of blocking operations usually goes hand in hand
with worker threads that get their dedicated connection. The use of
Redis Transactions is the typical use case for dynamic connection
pooling as the number of threads requiring a dedicated connection tends
to be dynamic. That said, the requirement for dynamic connection pooling
is limited. Connection pooling always comes with a cost of complexity
and maintenance.

### Execution Models

Lettuce supports two execution models for pooling:

- Synchronous/Blocking via Apache Commons Pool 2

- Asynchronous/Non-Blocking via a Lettuce-specific pool implementation
  (since version 5.1)

### Synchronous Connection Pooling

Using imperative programming models, synchronous connection pooling is
the right choice as it carries out all operations on the thread that is
used to execute the code.

#### Prerequisites

Lettuce requires Apache’s
[common-pool2](https://commons.apache.org/proper/commons-pool/)
dependency (at least 2.2) to provide connection pooling. Make sure to
include that dependency on your classpath. Otherwise, you won’t be able
using connection pooling.

If using Maven, add the following dependency to your `pom.xml`:

``` xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
    <version>2.4.3</version>
</dependency>
```

#### Connection pool support

Lettuce provides generic connection pool support. It requires a
connection `Supplier` that is used to create connections of any
supported type (Redis Standalone, Pub/Sub, Sentinel, Master/Replica,
Redis Cluster). `ConnectionPoolSupport` will create a
`GenericObjectPool` or `SoftReferenceObjectPool`, depending on your
needs. The pool can allocate either wrapped or direct connections.

- Wrapped instances will return the connection back to the pool when
  called `StatefulConnection.close()`.

- Regular connections need to be returned to the pool with
  `GenericObjectPool.returnObject(…)`.

**Basic usage**

``` java
RedisClient client = RedisClient.create(RedisURI.create(host, port));

GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
               .createGenericObjectPool(() -> client.connect(), new GenericObjectPoolConfig());

// executing work
try (StatefulRedisConnection<String, String> connection = pool.borrowObject()) {

    RedisCommands<String, String> commands = connection.sync();
    commands.multi();
    commands.set("key", "value");
    commands.set("key2", "value2");
    commands.exec();
}

// terminating
pool.close();
client.shutdown();
```

**Cluster usage**

``` java
RedisClusterClient clusterClient = RedisClusterClient.create(RedisURI.create(host, port));

GenericObjectPool<StatefulRedisClusterConnection<String, String>> pool = ConnectionPoolSupport
               .createGenericObjectPool(() -> clusterClient.connect(), new GenericObjectPoolConfig());

// execute work
try (StatefulRedisClusterConnection<String, String> connection = pool.borrowObject()) {
    connection.sync().set("key", "value");
    connection.sync().blpop(10, "list");
}

// terminating
pool.close();
clusterClient.shutdown();
```

### Asynchronous Connection Pooling

Asynchronous/non-blocking programming models require a non-blocking API
to obtain Redis connections. A blocking connection pool can easily lead
to a state that blocks the event loop and prevents your application from
progress in processing.

Lettuce comes with an asynchronous, non-blocking pool implementation to
be used with Lettuces asynchronous connection methods. It does not
require additional dependencies.

#### Asynchronous Connection pool support

Lettuce provides asynchronous connection pool support. It requires a
connection `Supplier` that is used to asynchronously connect to any
supported type (Redis Standalone, Pub/Sub, Sentinel, Master/Replica,
Redis Cluster). `AsyncConnectionPoolSupport` will create a
`BoundedAsyncPool`. The pool can allocate either wrapped or direct
connections.

- Wrapped instances will return the connection back to the pool when
  called `StatefulConnection.closeAsync()`.

- Regular connections need to be returned to the pool with
  `AsyncPool.release(…)`.

**Basic usage**

``` java
RedisClient client = RedisClient.create();

CompletionStage<AsyncPool<StatefulRedisConnection<String, String>>> poolFuture = AsyncConnectionPoolSupport.createBoundedObjectPoolAsync(
        () -> client.connectAsync(StringCodec.UTF8, RedisURI.create(host, port)), BoundedPoolConfig.create());

// await poolFuture initialization to avoid NoSuchElementException: Pool exhausted when starting your application

// execute work
CompletableFuture<TransactionResult> transactionResult = pool.acquire().thenCompose(connection -> {

    RedisAsyncCommands<String, String> async = connection.async();

    async.multi();
    async.set("key", "value");
    async.set("key2", "value2");
    return async.exec().whenComplete((s, throwable) -> pool.release(connection));
});

// terminating
pool.closeAsync();

// after pool completion
client.shutdownAsync();
```

**Cluster usage**

``` java
RedisClusterClient clusterClient = RedisClusterClient.create(RedisURI.create(host, port));

CompletionStage<AsyncPool<StatefulRedisClusterConnection<String, String>>> poolFuture = AsyncConnectionPoolSupport.createBoundedObjectPoolAsync(
        () -> clusterClient.connectAsync(StringCodec.UTF8), BoundedPoolConfig.create());

// execute work
CompletableFuture<String> setResult = pool.acquire().thenCompose(connection -> {

    RedisAdvancedClusterAsyncCommands<String, String> async = connection.async();

    async.set("key", "value");
    return async.set("key2", "value2").whenComplete((s, throwable) -> pool.release(connection));
});

// terminating
pool.closeAsync();

// after pool completion
clusterClient.shutdownAsync();
```

## Custom commands

Lettuce covers nearly all Redis commands. Redis development is an
ongoing process and the Redis Module system is intended to introduce new
commands which are not part of the Redis Core. This requirement
introduces the need to invoke custom commands or use custom outputs.
Custom commands can be dispatched on the one hand using Lua and the
`eval()` command, on the other side Lettuce 4.x allows you to trigger
own commands. That API is used by Lettuce itself to dispatch commands
and requires some knowledge of how commands are constructed and
dispatched within Lettuce.

Lettuce provides two levels of command dispatching:

1.  Using the synchronous, asynchronous or reactive API wrappers which
    invoke commands according to their nature

2.  Using the bare connection to influence the command nature and
    synchronization (advanced)

**Example using `dispatch()` on the synchronous API**

``` java
RedisCodec<String, String> codec = StringCodec.UTF8;
RedisCommands<String, String> commands = ...

String response = redis.dispatch(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec)
                       .addKey(key)
                       .addValue(value));
```

**Example using `dispatch()` on the asynchronous API**

``` java
RedisCodec<String, String> codec = StringCodec.UTF8;
RedisAsyncCommands<String, String> commands = ...

RedisFuture<String> response = redis.dispatch(CommandType.SET, new StatusOutput<>(codec),
                                    new CommandArgs<>(codec)
                                        .addKey(key)
                                        .addValue(value));
```

**Example using `dispatch()` on the reactive API**

``` java
RedisCodec<String, String> codec = StringCodec.UTF8;
RedisReactiveCommands<String, String> commands = ...

Observable<String> response = redis.dispatch(CommandType.SET, new StatusOutput<>(codec),
                                    new CommandArgs<>(codec)
                                        .addKey(key)
                                        .addValue(value));
```

**Example using a `RedisFuture` command wrapper**

``` java
StatefulRedisConnection<String, String> connection = redis.getStatefulConnection();

RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                                        new StatusOutput<>(StringCodec.UTF8));

AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
connection.dispatch(async);

// async instanceof CompletableFuture == true
```

### Mechanics of Lettuce commands

Lettuce uses the command pattern to implement to execute commands. Every
time a command is invoked, Lettuce creates a command object (`Command`
or types implementing `RedisCommand`). Commands can carry arguments
(`CommandArgs`) and an output (subclasses of `CommandOutput`). Both are
optional. The two mandatory properties are the command type (see
`CommandType` or a type implementing `ProtocolKeyword`) and a
`RedisCodec`. If you dispatch commands by yourself, do not reuse command
instances to dispatch commands more than once. Commands that were
executed once have the completed flag set and cannot be reused.

#### Arguments

`CommandArgs` is a container for command arguments that follow the
command keyword (`CommandType`). A `PING` or `QUIT` command do not
require commands whereas the `GET` or `SET` commands require arguments
in the form of keys and values.

**The `PING` command**

``` java
RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                                        new StatusOutput<>(StringCodec.UTF8));
```

**The `SET` command**

``` java
StringCodec codec = StringCodec.UTF8;
RedisCommand<String, String, String> command = new Command<>(CommandType.SET,
                new StatusOutput<>(codec), new CommandArgs<>(codec)
                                                  .addKey("key")
                                                  .addValue("value"));
```

`CommandArgs` allow to add one or more:

- key and arrays of keys

- value and arrays of values

- `String`, `long` (the Redis integer), `double`

- byte array

- `CommandType`, `CommandKeyword` and generic `ProtocolKeyword`

The sequence of args and keywords is not validated by Lettuce beyond the
supported data types, meaning Redis will report errors if the command
syntax is not correct.

#### Outputs

Commands producing an output are required to consume the output. Lettuce
supports type-safe conversion of the response into the appropriate
result types. The output handlers derive from the `CommandOutput` base
class. Lettuce provides a wide range of output types (see the
`com.lambdaworks.redis.output` package for details). Command outputs are
mostly used to return the result as the whole object. The response is
available as soon as the whole command output is processed. There are
cases, where you might want to stream the response instead of allocating
a significant amount of memory and return the whole response as one.
These types are called streaming outputs. Following implementations ship
with Lettuce:

- `KeyStreamingOutput`

- `KeyValueScanStreamingOutput`

- `KeyValueStreamingOutput`

- `ScoredValueStreamingOutput`

- `ValueScanStreamingOutput`

- `ValueStreamingOutput`

Those outputs take a streaming channel (see `ValueStreamingChannel`) and
invoke the callback method (e.g. `onValue(V value)`) for every data
element.

Implementing an own output is, in general, a good idea when you want to
support a different data type, or you want to work with different types
than the basic collection, map, String, and primitive types. You might
get an impression of the custom types idea by taking a look on
`GeoWithinListOutput`, which takes a bunch of strings and nested lists
to construct a list of `GeoWithin` instances.

Please note that using an output that does not fit the command output
can jam the response processing and lead to not usable connections. Use
either `ArrayOutput` or `NestedMultiOutput` when in doubt, so you
receive a list of objects (nested lists).

**Output for the `PING` command**

``` java
Command<String, String, String> command = new Command<>(CommandType.PING,
                                        new StatusOutput<>(StringCodec.UTF8));
```

**Output for the `HGETALL` command**

``` java
StringCodec codec = StringCodec.UTF8;
Command<String, String, Map<String, String>> command = new Command<>(CommandType.HGETALL,
                                    new MapOutput<>(codec),
                                    new CommandArgs<>(codec).addKey(key));
```

**Output for the `HKEYS` command**

``` java
StringCodec codec = StringCodec.UTF8;
Command<String, String, List<String>> command = new Command<>(CommandType.HKEYS,
                                    new KeyListOutput<>(codec),
                                    new CommandArgs<>(codec).addKey(key));
```

### Synchronous, asynchronous and reactive

Great, that you made it up to here. You might want to know now, how to
synchronize the command completion, work with `Future`s or how about the
reactive API. The simple way is using the `dispatch(…)` method of the
according wrapper. If this is not sufficient, then continue on reading.

The `dispatch()` method on a stateful Redis connection is not
opinionated at all how you are using Lettuce, whether it is synchronous
or reactive. The only thing this method does is dispatching the command.
The response handler handles decoding the command and completing the
command once it’s done. The asynchronous command processing is the only
operating mode of Lettuce.

The `RedisCommand` interface provides methods to `complete()`,
`cancel()` and `completeExceptionally()` the command. The `complete()`
methods are called by the response handler as soon as the command is
completed. Redis commands can be wrapped and augmented by that way.
Wrapping is used when using transactions (`MULTI`) or Redis Cluster.

You are free to implement your command type or use one of the provided
commands:

- Command (default implementation)

- AsyncCommand (the `CompleteableFuture` wrapper for `RedisCommand`)

- CommandWrapper (generic wrapper)

- TransactionalCommand (wraps `RedisCommand`s when `MULTI` is active)

#### Fire & Forget

Fire&Forget is the simple-most way to dispatch commands. You just
trigger it and then you do not care what happens, whether the command
completes or not, and you don’t have access to the command output:

``` java
StatefulRedisConnection<String, String> connection = redis.getStatefulConnection();

connection.dispatch(CommandType.PING, VoidOutput.create());
```

!!! NOTE
    `VoidOutput.create()` swallows also Redis error responses. If you want
    to just avoid response decoding, create a `VoidCodec` instance using
    its constructor to retain error response decoding.

#### Asynchronous

The asynchronous API works in general with the `AsyncCommand` wrapper
that extends `CompleteableFuture`. `AsyncCommand` can be synchronized by
`await()` or `get()` which corresponds with the asynchronous pull style.
By using the methods from the `CompletionStage` interface (such as
`handle()` or `thenAccept()`) the response handler will trigger the
functions ("listeners") on command completion. Lear more about
asynchronous usage in the [Asynchronous API](user-guide/async-api.md) topic.

``` java
StatefulRedisConnection<String, String> connection = redis.getStatefulConnection();

RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                                        new StatusOutput<>(StringCodec.UTF8));

AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
connection.dispatch(async);

// async instanceof CompletableFuture == true
```

#### Synchronous

The synchronous API of Lettuce uses future synchronization to provide a
synchronous view.

#### Reactive

Reactive commands are dispatched at the moment of subscription (see
[Reactive API](user-guide/reactive-api.md) for more details on reactive APIs). In the
context of Lettuce this means, you need to start before calling the
`dispatch()` method. The reactive API uses internally an
`ObservableCommand`, but that is internal stuff. If you want to dispatch
commands the reactive way, you’ll need to wrap commands (or better:
command supplier to be able to retry commands) with the
`ReactiveCommandDispatcher`. The dispatcher implements the `OnSubscribe`
API to create an `Observable<T>`, handles command dispatching at the
time of subscription and can dissolve collection types to particular
elements. An instance of `ReactiveCommandDispatcher` allows creating
multiple `Observable`s as long as you use a `Supplier<RedisCommand>`.
Commands that were executed once have the completed flag set and cannot
be reused.

``` java
StatefulRedisConnection<String, String> connection = redis.getStatefulConnection();

RedisCommand<String, String, String> command = new Command<>(CommandType.PING,
                new StatusOutput<>(StringCodec.UTF8));
ReactiveCommandDispatcher<String, String, String> dispatcher = new ReactiveCommandDispatcher<>(command,
                connection, false);

Observable<String> observable = Observable.create(dispatcher);
String result = observable.toBlocking().first();

result == "PONG"
```

## Graal Native Image

This section explains how to use Lettuce with Graal Native Image
compilation.

### Why Create a Native Image?

The GraalVM
[`native-image`](http://www.graalvm.org/docs/reference-manual/aot-compilation/)
tool enables ahead-of-time (AOT) compilation of Java applications into
native executables or shared libraries. While traditional Java code is
just-in-time (JIT) compiled at run time, AOT compilation has two main
advantages:

1.  First, it improves the start-up time since the code is already
    pre-compiled into efficient machine code.

2.  Second, it reduces the memory footprint of Java applications since
    it eliminates the need to include infrastructure to load and
    optimize code at run time.

There are additional advantages such as more predictable performance and
less total CPU usage.

### Building Native Images

Native images assume a closed world principle in which all code needs to
be known at the time the native image is built. Graal's SubstrateVM
analyzes class files during native image build-time to determine what
bytecode needs to be translated into a native image. While this task can
be achieved to a good extent by analyzing static bytecode, it’s harder
for dynamic parts of the code such as reflection. When using reflective
access or Java proxies, the native image build process requires a little
bit of help so it can include parts that are required during runtime.

Lettuce ships with configuration files that specifically describe which
classes are used by Lettuce during runtime and which Java proxies get
created.

Starting as of Lettuce 5.3.2, the following configuration files are
available:

- `META-INF/native-image/io.lettuce/lettuce-core/native-image.properties`

- `META-INF/native-image/io.lettuce/lettuce-core/proxy-config.json`

- `META-INF/native-image/io.lettuce/lettuce-core/reflect-config.json`

Those cover Lettuce operations for `RedisClient` and
`RedisClusterClient`.

Depending on your configuration you might need additional configuration
for Netty, HdrHistogram (metrics collection), Reactive Libraries, and
dynamic Redis Command interfaces.

### HdrHistogram/Command Latency Metrics

Lettuce uses HdrHistogram and LatencyUtils to accumulate metrics. You
can use your application without these. If you want to use Command
Latency Metrics, please add the following lines to your own
`reflect-config.json` file:

``` json
  {
    "name": "org.HdrHistogram.Histogram"
  },
  {
    "name": "org.LatencyUtils.PauseDetector"
  }
```

### Dynamic Command Interfaces

You can use Dynamic Command Interfaces when compiling your code to a
GraalVM Native Image. GraalVM requires two information as Lettuce
inspects command interfaces using reflection and it creates a Java
proxy:

1.  Add the command interface class name to your `reflect-config.json`
    using ideally `allDeclaredMethods:true`.

2.  Add the command interface class name to your `proxy-config.json`

<div class="formalpara-title">

**`reflect-config.json`**

</div>

``` json
[
  {
    "name": "com.example.MyCommands",
    "allDeclaredMethods": true
  },
]
```

<div class="formalpara-title">

**`proxy-config.json`**

</div>

``` json
[
  ["com.example.MyCommands"]
]
```

#### Reactive Libraries

If you decide to use a specific reactive library with dynamic command
interfaces, please add the following lines to your `reflect-config.json`
file, depending on the presence of Rx Java 1-3:

``` json
  {
    "name": "rx.Completable"
  },
  {
    "name": "io.reactivex.Flowable"
  },
  {
    "name": "io.reactivex.rxjava3.core.Flowable"
  }
```

### Limitations

For now, native images must be compiled with
`--report-unsupported-elements-at-runtime` to ignore missing Method
Handles and annotation synthetization failures.

#### Netty Config

To properly start up the netty stack, the following reflection
configuration is required for netty and the JDK in
`reflect-config.json`:

``` json
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueColdProducerFields",
    "fields":[{"name":"producerLimit","allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueConsumerFields",
    "fields":[{"name":"consumerIndex","allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueueProducerFields",
    "fields":[{"name":"producerIndex", "allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueConsumerIndexField",
    "fields":[{"name":"consumerIndex", "allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerIndexField",
    "fields":[{"name":"producerIndex", "allowUnsafeAccess" :  true}]
  },
  {
    "name":"io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueueProducerLimitField",
    "fields":[{"name":"producerLimit","allowUnsafeAccess" :  true}]
  },
  {
    "name":"java.nio.Buffer",
    "fields":[{"name":"address", "allowUnsafeAccess":true}]
  },
  {
    "name":"java.nio.DirectByteBuffer",
    "fields":[{"name":"cleaner", "allowUnsafeAccess":true}],
    "methods":[{"name":"<init>","parameterTypes":["long","int"] }]
  },
  {
    "name":"io.netty.buffer.AbstractReferenceCountedByteBuf",
    "fields":[{"name":"refCnt", "allowUnsafeAccess":true}]
  },
  {
    "name":"io.netty.buffer.AbstractByteBufAllocator",
    "allPublicMethods": true,
    "allDeclaredFields":true,
    "allDeclaredMethods":true,
    "allDeclaredConstructors":true
  },
  {
    "name":"io.netty.buffer.PooledByteBufAllocator",
    "allPublicMethods": true,
    "allDeclaredFields":true,
    "allDeclaredMethods":true,
    "allDeclaredConstructors":true
  },
  {
    "name":"io.netty.channel.ChannelDuplexHandler",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name":"io.netty.channel.ChannelHandlerAdapter",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.ChannelInboundHandlerAdapter",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.ChannelInitializer",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.ChannelOutboundHandlerAdapter",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.DefaultChannelPipeline$HeadContext",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.DefaultChannelPipeline$TailContext",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.channel.socket.nio.NioSocketChannel",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name": "io.netty.handler.codec.MessageToByteEncoder",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  },
  {
    "name":"io.netty.util.ReferenceCountUtil",
    "allPublicMethods": true,
    "allDeclaredConstructors":true
  }
```

#### Functionality

We don’t have found a way yet to invoke default interface methods on
proxies without `MethodHandle`. Hence the `NodeSelection` API
(`masters()`, `all()` and others on `RedisAdvancedClusterCommands` and
`RedisAdvancedClusterAsyncCommands`) do not work.

## Command execution reliability

Lettuce is a thread-safe and scalable Redis client that allows multiple
independent connections to Redis.

### General

Lettuce provides two levels of consistency; these are the rules for
Redis command sends:

#### Depending on the chosen consistency level

- **at-most-once execution**, i.e. no guaranteed execution

- **at-least-once execution**, i.e. guaranteed execution (with [some
  exceptions](#exceptions-to-at-least-once))

#### Always

- command ordering in the order of invocations

### What does *at-most-once* mean?

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

### Why No Guaranteed Delivery?

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

### Message Ordering

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

### Failures and *at-least-once* execution

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

#### Exceptions to *at-least-once*

Lettuce does not loose commands while sending them. A command execution
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
experienced such severe failures get into a unrecoverable state, and no
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

### Switching between *at-least-once* and *at-most-once* operations

Lettuce’s consistency levels are bound to retries on reconnects and the
connection state. By default, Lettuce operates in the *at-least-once*
mode. Auto-reconnect is enabled and as soon as the connection is
re-established, queued commands are re-sent for execution. While a
connection failure persists, issued commands are buffered.

To change into *at-most-once* consistency level, disable auto-reconnect
mode. Connections can no longer be reconnected and thus no retries are
issued. Unsuccessful commands are canceled. New commands are rejected.

#### Controlling replay of commands in *at-lease-once* mode

!!! NOTE
    This feature is only available since Lettuce 6.6

One can achieve a more fine-grained control over the commands that are
replayed after a reconnection by using the option to specify a filter
predicate. This option is part of the ClientOptions configuration. See 
[Client Options](advanced-usage.md#client-options) for further reference.

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

### Clustered operations

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

