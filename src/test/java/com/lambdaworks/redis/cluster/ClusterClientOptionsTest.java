package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ClusterClientOptionsTest {

    @Test
    public void testCopy() throws Exception {

        ClusterClientOptions options = ClusterClientOptions.builder()
                .autoReconnect(false).requestQueueSize(100).suspendReconnectOnProtocolFailure(true).maxRedirects(1234)
                .validateClusterNodeMembership(false).build();

        ClusterClientOptions copy = ClusterClientOptions.copyOf(options);

        assertThat(copy.getRefreshPeriod()).isEqualTo(options.getRefreshPeriod());
        assertThat(copy.getRefreshPeriodUnit()).isEqualTo(options.getRefreshPeriodUnit());
        assertThat(copy.isCloseStaleConnections()).isEqualTo(options.isCloseStaleConnections());
        assertThat(copy.isRefreshClusterView()).isEqualTo(options.isRefreshClusterView());
        assertThat(copy.isValidateClusterNodeMembership()).isEqualTo(options.isValidateClusterNodeMembership());
        assertThat(copy.getRequestQueueSize()).isEqualTo(options.getRequestQueueSize());
        assertThat(copy.isAutoReconnect()).isEqualTo(options.isAutoReconnect());
        assertThat(copy.isCancelCommandsOnReconnectFailure()).isEqualTo(options.isCancelCommandsOnReconnectFailure());
        assertThat(copy.isSuspendReconnectOnProtocolFailure()).isEqualTo(options.isSuspendReconnectOnProtocolFailure());
        assertThat(copy.getMaxRedirects()).isEqualTo(options.getMaxRedirects());
    }
}
