# ClientOptions

ClientOptions allow controlling behavior for some specific features.

ClientOptions are immutable. Connections inherit the current options at
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
<p>Charset to use for Lua scripts.</p></td>
</tr>
<tr>
<td>Socket Options</td>
<td><code>socketOptions</code></td>
<td><code>10 seconds Connection-Timeout, no keep-alive, no TCP noDelay</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.3</p>
<p>Options to configure low-level socket options for the connections
kept to Redis servers.</p></td>
</tr>
<tr>
<td>SSL Options</td>
<td><code>sslOptions</code></td>
<td><code>(none), use JDK defaults</code></td>
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

## Cluster-specific options

ClusterClientOptions extend the regular ClientOptions by some cluster
specifics.

ClusterClientOptions are immutable. Connections inherit the current
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
discarding the full client by setting new ClientOptions.</p></td>
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
<td><code>enableAdaptiveRefreshTrigger</code></td>
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
<code>PERSISTENT_RECONNECTS</code>, <code>UNKNOWN_NODE</code> (since
5.1), and <code>UNCOVERED_SLOT</code> (since 5.2) (see also reconnect
attempts for the reconnect trigger)</p></td>
</tr>
<tr>
<td>Adaptive refresh triggers timeout</td>
<td><code>adaptiveRefreshTriggersTimeout</code></td>
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
<td><code>refreshTriggersReconnectAttempts</code></td>
<td><code>5</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 4.2</p>
<p>Set the threshold for the <code>PERSISTENT_RECONNECTS</code> refresh
trigger. Topology updates based on persistent reconnects lead only to a
refresh if the reconnect process tries at least the number of specified
attempts. The first reconnect attempt starts with
<code>1</code>.</p></td>
</tr>
<tr>
<td>Dynamic topology refresh sources</td>
<td><code>dynamicRefreshSources</code></td>
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
<td><code>closeStaleConnections</code></td>
<td><code>true</code></td>
</tr>
<tr>
<td colspan="3"><p>Since: 3.3, 4.1</p>
<p>Stale connections are existing connections to nodes which are no
longer part of the Redis Cluster. If this flag is set to
<code>true</code>, then stale connections are closed upon topology
refreshes. It's strongly advised to close stale connections as open
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
remove unwanted nodes (e.g. failed replicas). Note that the filter is
applied only after obtaining the topology so the filter does not prevent
trying to connect the node during topology discovery.</p></td>
</tr>
<tr>
<td>Validate cluster node membership</td>
<td><code>validateClusterNodeMembership</code></td>
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

## Request queue size and cluster

Clustered operations use multiple connections. The resulting
overall-queue limit is
`requestQueueSize * ((number of cluster nodes * 2) + 1)`.
