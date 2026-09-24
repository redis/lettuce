package io.lettuce.scenario;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@code rladmin status} parsing in {@link RedisEnterpriseConfig}.
 * <p>
 * These pin the two facts the Sentinel scenario tests depend on and which no live cluster is needed to check: an endpoint can
 * be proxied by several nodes at once, and an address the discovery service reports has to be resolvable back to the node that
 * owns it.
 *
 * @author Redis
 */
@Tag(UNIT_TEST)
class RedisEnterpriseConfigUnitTests {

    private static final String NODES = "CLUSTER NODES:\n"
            + "NODE:ID    ROLE       ADDRESS       EXTERNAL_ADDRESS  HOSTNAME  SHARDS   CORES  VERSION     STATUS\n"
            + "*node:1    master     10.0.101.25   100.53.13.62      node1     0/100    2      7.4.2-54    OK\n"
            + " node:2    slave      10.0.101.188  13.220.255.215    node2     2/100    2      7.4.2-54    OK\n"
            + " node:3    slave      10.0.101.34   54.90.199.133     node3     2/100    2      7.4.2-54    OK\n";

    private static final String SINGLE_PROXY_ENDPOINT = "ENDPOINTS:\n"
            + "DB:ID   NAME        ID            NODE      ROLE    SSL\n"
            + "db:3    m-standard  endpoint:3:1  node:2    single  No\n";

    /**
     * What {@code rladmin bind endpoint 3:1 include 2} leaves behind: the node is unioned onto the proxy set rather than the
     * endpoint being moved, so rladmin prints one row per proxy.
     */
    private static final String TWO_PROXY_ENDPOINT = "ENDPOINTS:\n"
            + "DB:ID   NAME        ID            NODE      ROLE    SSL\n"
            + "db:3    m-standard  endpoint:3:1  node:3    single  No\n"
            + "db:3    m-standard  endpoint:3:1  node:2    single  No\n";

    @Test
    void shouldParseASingleProxyEndpoint() {

        RedisEnterpriseConfig config = new RedisEnterpriseConfig("3");
        config.parseFullStatus(NODES + SINGLE_PROXY_ENDPOINT);

        assertThat(config.getEndpointIds()).containsExactly("endpoint:3:1");
        assertThat(config.getEndpointNodes("endpoint:3:1")).containsExactly("node:2");
        assertThat(config.getEndpointNode("endpoint:3:1")).isEqualTo("node:2");
    }

    @Test
    void shouldReportEveryProxyOfAMultiProxyEndpoint() {

        RedisEnterpriseConfig config = new RedisEnterpriseConfig("3");
        config.parseFullStatus(NODES + TWO_PROXY_ENDPOINT);

        // Both proxies are visible, in the order rladmin printed them. Collapsing this to one node is what made the
        // scenario tests exclude an arbitrary proxy, which the discovery service does not announce.
        assertThat(config.getEndpointNodes("endpoint:3:1")).containsExactly("node:3", "node:2");

        // The endpoint itself is still one endpoint, even though it appears on two rows.
        assertThat(config.getEndpointIds()).containsExactly("endpoint:3:1");
        assertThat(config.getFirstEndpointId()).isEqualTo("3:1");
    }

    @Test
    void shouldNotAccumulateProxiesAcrossParses() {

        RedisEnterpriseConfig config = new RedisEnterpriseConfig("3");
        config.parseFullStatus(NODES + TWO_PROXY_ENDPOINT);
        config.parseFullStatus(NODES + SINGLE_PROXY_ENDPOINT);

        assertThat(config.getEndpointNodes("endpoint:3:1")).containsExactly("node:2");
    }

    @Test
    void shouldReportNoProxiesForAnUnknownEndpoint() {

        RedisEnterpriseConfig config = new RedisEnterpriseConfig("3");
        config.parseFullStatus(NODES + SINGLE_PROXY_ENDPOINT);

        assertThat(config.getEndpointNodes("endpoint:9:1")).isEmpty();
        assertThat(config.getEndpointNode("endpoint:9:1")).isNull();
    }

    @Test
    void shouldResolveANodeFromEitherOfItsAddresses() {

        RedisEnterpriseConfig config = new RedisEnterpriseConfig("3");
        config.parseFullStatus(NODES + SINGLE_PROXY_ENDPOINT);

        // The discovery service reports external addresses; the @internal master name carries the internal ones.
        assertThat(config.findNodeByAddress("54.90.199.133")).isEqualTo("node:3");
        assertThat(config.findNodeByAddress("10.0.101.34")).isEqualTo("node:3");
        assertThat(config.findNodeByAddress("13.220.255.215")).isEqualTo("node:2");

        // The master row is prefixed with '*', which must not stop the node being recognised.
        assertThat(config.findNodeByAddress("100.53.13.62")).isEqualTo("node:1");
    }

    @Test
    void shouldNotGuessANodeForAnUnknownAddress() {

        RedisEnterpriseConfig config = new RedisEnterpriseConfig("3");
        config.parseFullStatus(NODES + SINGLE_PROXY_ENDPOINT);

        assertThat(config.findNodeByAddress("203.0.113.7")).isNull();
        assertThat(config.findNodeByAddress("")).isNull();
        assertThat(config.findNodeByAddress(null)).isNull();
    }

}
