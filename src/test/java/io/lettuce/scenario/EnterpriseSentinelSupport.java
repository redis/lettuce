package io.lettuce.scenario;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.test.env.Endpoints;
import reactor.core.Disposable;

/**
 * Shared plumbing for the Redis Enterprise discovery-service (Sentinel-compatible API) scenario tests.
 * <p>
 * Redis Enterprise runs a Sentinel-protocol emulator on port 8001 of every cluster node. It announces each database under two
 * master names - {@code <db>} resolving to the endpoint's external address and {@code <db>@internal} resolving to the internal
 * one - and publishes {@code +switch-master} whenever the database's endpoint (DMC proxy) address changes.
 *
 * @author Redis
 */
final class EnterpriseSentinelSupport {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseSentinelSupport.class);

    /**
     * Port of the Redis Enterprise discovery service. Hard-coded and non-configurable in Redis Enterprise.
     */
    static final int SENTINEL_PORT = 8001;

    /**
     * Endpoint configuration key, which is also the Redis Enterprise database name and therefore the Sentinel master name.
     */
    static final String DB_NAME = "m-standard";

    static final String SWITCH_MASTER_CHANNEL = "+switch-master";

    static final String PLUS_MASTER_CHANNEL = "+master";

    static final String MINUS_MASTER_CHANNEL = "-master";

    /**
     * Master name resolving to the internal address of the endpoint. Redis Enterprise publishes a second {@code +switch-master}
     * under this name for every endpoint move.
     */
    static final String INTERNAL_SUFFIX = "@internal";

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);

    private EnterpriseSentinelSupport() {
    }

    /**
     * Discovery-service URIs, one per cluster node, taken verbatim from the {@code discovery_endpoints} field of the endpoint
     * configuration.
     * <p>
     * The URIs deliberately carry no credentials and no client name: the Redis Enterprise discovery service implements no
     * {@code AUTH} command, and its {@code HELLO} rejects every argument past the protocol version. A credentialed URI makes
     * Lettuce send {@code HELLO 3 AUTH ...}, and the resulting syntax error is neither {@code unknown command} nor
     * {@code NOPROTO}, so the handshake does not fall back to RESP2 and the connection fails outright. Credentials belong on
     * the top-level {@code redis-sentinel://} URI only, from which the data-node URIs inherit them.
     *
     * @param endpoint endpoint configuration of the database under test.
     * @return discovery-service URIs, empty when the configuration does not advertise them.
     */
    static List<RedisURI> sentinelUris(Endpoints.Endpoint endpoint) {

        List<RedisURI> uris = new ArrayList<>();
        List<String> discoveryEndpoints = endpoint.getDiscoveryEndpoints();

        if (discoveryEndpoints == null) {
            return uris;
        }

        for (String discoveryEndpoint : discoveryEndpoints) {
            HostAndPort hostAndPort = HostAndPort.parse(discoveryEndpoint);
            uris.add(RedisURI.builder().withHost(hostAndPort.getHostText())
                    .withPort(hostAndPort.hasPort() ? hostAndPort.getPort() : SENTINEL_PORT).withTimeout(PROBE_TIMEOUT)
                    .build());
        }

        return uris;
    }

    /**
     * Skip message for the {@code assumeTrue} guard, naming the field and the generator that should emit it.
     */
    static String missingDiscoveryEndpoints() {
        return "Skipping: endpoint '" + DB_NAME + "' does not advertise 'discovery_endpoints'. The endpoints.json "
                + "generator (re_env0 redis_ent.py create_bdbs) should emit one '<node-addr>:" + SENTINEL_PORT
                + "' entry per cluster node.";
    }

    /**
     * Skip message for the reachability guard. Covers all three ways the discovery service can be absent.
     */
    static String unreachableDiscoveryService(List<RedisURI> sentinels) {
        return "Skipping: no Redis Enterprise discovery service answered PING on " + sentinels + ". Either port "
                + SENTINEL_PORT + " is not reachable (security-group ingress), or the service is disabled "
                + "('rladmin cluster config services sentinel_service enabled'; check 'rladmin info services_config').";
    }

    /**
     * The {@code redis-sentinel://} URI from {@link io.lettuce.examples.ConnectToRedisUsingRedisSentinel}: Sentinel is used
     * only to discover the current master, and {@link RedisClient#connect()} then opens an ordinary standalone connection to
     * it.
     * <p>
     * Credentials are carried in the URI userinfo, which {@code RedisURI} applies to the top-level URI (the data connection)
     * while building the per-sentinel entries without them - see {@code RedisURI#configureSentinel}. That split is what makes
     * this URI usable against Redis Enterprise at all: its discovery service implements no {@code AUTH}, and its {@code HELLO}
     * rejects every argument past the protocol version, so a credentialed sentinel URI would fail the handshake outright.
     *
     * @return e.g. {@code redis-sentinel://default:pass@host1:8001,host2:8001,host3:8001#m-standard}
     */
    static RedisURI sentinelDiscoveryUri(Endpoints.Endpoint endpoint, String masterName) {

        StringBuilder hosts = new StringBuilder();
        for (RedisURI sentinel : sentinelUris(endpoint)) {
            if (hosts.length() > 0) {
                hosts.append(',');
            }
            hosts.append(sentinel.getHost()).append(':').append(sentinel.getPort());
        }

        String uri = String.format("redis-sentinel://%s:%s@%s#%s", endpoint.getUsername(), endpoint.getPassword(), hosts,
                masterName);

        RedisURI redisURI = RedisURI.create(uri);
        redisURI.setTimeout(PROBE_TIMEOUT);
        return redisURI;
    }

    /**
     * Client options for connections to the discovery service itself. RESP2 keeps the handshake to {@code PING}, which the
     * discovery service implements, and avoids relying on how it renders RESP3 push frames.
     */
    static ClientOptions sentinelClientOptions() {
        return ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
    }

    /**
     * First discovery-service URI that answers {@code PING}, or {@code null} when none does.
     * <p>
     * Used instead of blindly taking the first configured URI: the discovery service runs on every node and answers for every
     * database, so any reachable node will do, and a single node being down must not abort the test.
     */
    static RedisURI firstReachableSentinel(List<RedisURI> sentinels) {

        for (RedisURI sentinel : sentinels) {
            RedisClient client = RedisClient.create();
            client.setOptions(sentinelClientOptions());
            try (StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(StringCodec.UTF8,
                    sentinel)) {
                if ("PONG".equals(connection.sync().ping())) {
                    log.info("Discovery service reachable at {}", sentinel);
                    return sentinel;
                }
            } catch (RuntimeException e) {
                log.warn("Discovery service not reachable at {}: {}", sentinel, e.toString());
            } finally {
                client.shutdown();
            }
        }

        return null;
    }

    /**
     * @return {@code true} if at least one discovery-service URI answers {@code PING}.
     */
    static boolean anySentinelReachable(List<RedisURI> sentinels) {
        return firstReachableSentinel(sentinels) != null;
    }

    /**
     * Address the discovery service currently reports for a master name, via {@code SENTINEL GET-MASTER-ADDR-BY-NAME}. This is
     * the authoritative, client-observable answer to "where is the endpoint now", independent of any Lettuce topology state.
     */
    static SocketAddress reportedMaster(RedisURI sentinel, String masterName) {

        RedisClient client = RedisClient.create();
        client.setOptions(sentinelClientOptions());
        try (StatefulRedisSentinelConnection<String, String> connection = client.connectSentinel(StringCodec.UTF8, sentinel)) {
            return connection.sync().getMasterAddrByName(masterName);
        } finally {
            client.shutdown();
        }
    }

    static String format(SocketAddress address) {

        if (address instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) address;
            return inet.getHostString() + ":" + inet.getPort();
        }

        return String.valueOf(address);
    }

    /**
     * Captures {@code +switch-master}, {@code +master} and {@code -master} straight off the discovery service using
     * {@code SUBSCRIBE}, which is what Redis Enterprise supports. This is intentionally independent of Lettuce's own
     * consumption path, so a test can tell "Redis Enterprise did not publish" apart from "Lettuce did not react".
     */
    static final class SwitchMasterCapture implements Closeable {

        private final RedisClient client;

        private final StatefulRedisPubSubConnection<String, String> connection;

        private final List<String[]> messages = new CopyOnWriteArrayList<>();

        private volatile boolean recording = true;

        SwitchMasterCapture(RedisURI sentinel) {

            this.client = RedisClient.create();
            this.client.setOptions(sentinelClientOptions());
            this.connection = client.connectPubSub(StringCodec.UTF8, sentinel);

            this.connection.addListener(new RedisPubSubAdapter<String, String>() {

                @Override
                public void message(String channel, String message) {
                    if (recording) {
                        log.info("Discovery service published: {} {}", channel, message);
                        messages.add(new String[] { channel, message });
                    } else {
                        log.info("Discovery service published (outside capture window, ignored): {} {}", channel, message);
                    }
                }

            });

            this.connection.sync().subscribe(SWITCH_MASTER_CHANNEL, PLUS_MASTER_CHANNEL, MINUS_MASTER_CHANNEL);
        }

        /**
         * Stop recording without closing the connection, so restore operations do not pollute the assertions.
         */
        void stopRecording() {
            this.recording = false;
        }

        void clear() {
            messages.clear();
        }

        List<String[]> messages() {
            return new ArrayList<>(messages);
        }

        /**
         * Payloads seen on {@code +switch-master} whose master name is exactly {@code masterName}. Matching uses the
         * {@code masterName + " "} prefix, which is also what Lettuce's own predicate uses - and is why
         * {@code <db>@internal ...} does not match {@code <db>}.
         */
        List<String> switchMasterPayloads(String masterName) {

            List<String> payloads = new ArrayList<>();
            for (String[] message : messages) {
                if (SWITCH_MASTER_CHANNEL.equals(message[0]) && message[1].startsWith(masterName + " ")) {
                    payloads.add(message[1]);
                }
            }
            return payloads;
        }

        Optional<String> firstSwitchMasterFor(String masterName) {
            List<String> payloads = switchMasterPayloads(masterName);
            return payloads.isEmpty() ? Optional.<String> empty() : Optional.of(payloads.get(0));
        }

        /**
         * @return {@code true} while the capturing connection is still usable, so "no event" can be told apart from "subscriber
         *         was disconnected".
         */
        boolean isAlive() {
            return connection.isOpen();
        }

        @Override
        public void close() {
            connection.close();
            client.shutdown();
        }

    }

    /**
     * Records connection activations and disconnects off the client event bus. Activations show which endpoint traffic landed
     * on; disconnects distinguish a proactive handoff from one the server forced by dropping the connection.
     */
    static final class ConnectionEventCapture implements Closeable {

        private final List<String> remotes = new CopyOnWriteArrayList<>();

        private final List<String> disconnects = new CopyOnWriteArrayList<>();

        private final Disposable subscription;

        ConnectionEventCapture(RedisClient client) {
            this.subscription = client.getResources().eventBus().get().subscribe(event -> {
                if (event instanceof ConnectionActivatedEvent) {
                    String remote = format(((ConnectionActivatedEvent) event).remoteAddress());
                    log.info("Connection activated: {}", remote);
                    remotes.add(remote);
                } else if (event instanceof DisconnectedEvent) {
                    String remote = format(((DisconnectedEvent) event).remoteAddress());
                    log.info("Connection disconnected: {}", remote);
                    disconnects.add(remote);
                }
            });
        }

        /**
         * Disconnects seen for connections to this port. A handoff driven by the client should need none.
         */
        List<String> disconnectsOnPort(int port) {
            return matching(disconnects, port);
        }

        /**
         * @param port only connections to this port are considered, so the discovery-service connections on
         *        {@link #SENTINEL_PORT} do not pollute assertions about data connections.
         */
        List<String> remotesOnPort(int port) {
            return matching(remotes, port);
        }

        private static List<String> matching(List<String> addresses, int port) {

            List<String> matched = new ArrayList<>();
            String suffix = ":" + port;
            for (String address : addresses) {
                if (address.endsWith(suffix)) {
                    matched.add(address);
                }
            }
            return matched;
        }

        boolean sawRemote(String hostAndPort) {
            return remotes.contains(hostAndPort);
        }

        void clear() {
            remotes.clear();
            disconnects.clear();
        }

        @Override
        public void close() {
            subscription.dispose();
        }

    }

}
