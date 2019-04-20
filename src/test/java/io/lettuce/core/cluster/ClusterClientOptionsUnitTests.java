/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;

/**
 * @author Mark Paluch
 */
class ClusterClientOptionsUnitTests {

    @Test
    void testCopy() {

        ClusterClientOptions options = ClusterClientOptions.builder().autoReconnect(false).requestQueueSize(100)
                .suspendReconnectOnProtocolFailure(true).maxRedirects(1234).validateClusterNodeMembership(false)
                .protocolVersion(ProtocolVersion.RESP2).build();

        ClusterClientOptions copy = ClusterClientOptions.copyOf(options);

        assertThat(copy.getProtocolVersion()).isEqualTo(options.getProtocolVersion());
        assertThat(copy.getRefreshPeriod()).isEqualTo(options.getRefreshPeriod());
        assertThat(copy.isCloseStaleConnections()).isEqualTo(options.isCloseStaleConnections());
        assertThat(copy.isRefreshClusterView()).isEqualTo(options.isRefreshClusterView());
        assertThat(copy.isValidateClusterNodeMembership()).isEqualTo(options.isValidateClusterNodeMembership());
        assertThat(copy.getRequestQueueSize()).isEqualTo(options.getRequestQueueSize());
        assertThat(copy.isAutoReconnect()).isEqualTo(options.isAutoReconnect());
        assertThat(copy.isCancelCommandsOnReconnectFailure()).isEqualTo(options.isCancelCommandsOnReconnectFailure());
        assertThat(copy.isSuspendReconnectOnProtocolFailure()).isEqualTo(options.isSuspendReconnectOnProtocolFailure());
        assertThat(copy.getMaxRedirects()).isEqualTo(options.getMaxRedirects());
    }

    @Test
    void builderFromDefaultClientOptions() {

        ClientOptions clientOptions = ClientOptions.builder().build();
        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder(clientOptions).build();

        assertThat(clusterClientOptions.getProtocolVersion()).isEqualTo(clusterClientOptions.getProtocolVersion());
        assertThat(clusterClientOptions.getDisconnectedBehavior()).isEqualTo(clusterClientOptions.getDisconnectedBehavior());
        assertThat(clusterClientOptions.getSslOptions()).isEqualTo(clusterClientOptions.getSslOptions());
        assertThat(clusterClientOptions.getTimeoutOptions()).isEqualTo(clusterClientOptions.getTimeoutOptions());
        assertThat(clusterClientOptions.getRequestQueueSize()).isEqualTo(clusterClientOptions.getRequestQueueSize());
        assertThat(clusterClientOptions.isAutoReconnect()).isEqualTo(clusterClientOptions.isAutoReconnect());
        assertThat(clusterClientOptions.isCloseStaleConnections()).isEqualTo(clusterClientOptions.isCloseStaleConnections());
        assertThat(clusterClientOptions.isCancelCommandsOnReconnectFailure()).isEqualTo(
                clusterClientOptions.isCancelCommandsOnReconnectFailure());
        assertThat(clusterClientOptions.isPublishOnScheduler()).isEqualTo(clusterClientOptions.isPublishOnScheduler());
        assertThat(clusterClientOptions.isSuspendReconnectOnProtocolFailure()).isEqualTo(
                clusterClientOptions.isSuspendReconnectOnProtocolFailure());
        assertThat(clusterClientOptions.mutate()).isNotNull();
    }

    @Test
    void builderFromClusterClientOptions() {

        ClusterClientOptions options = ClusterClientOptions.builder().maxRedirects(1234).validateClusterNodeMembership(false)
                .build();

        ClusterClientOptions copy = ClusterClientOptions.builder(options).build();

        assertThat(copy.getRefreshPeriod()).isEqualTo(options.getRefreshPeriod());
        assertThat(copy.isCloseStaleConnections()).isEqualTo(options.isCloseStaleConnections());
        assertThat(copy.isRefreshClusterView()).isEqualTo(options.isRefreshClusterView());
        assertThat(copy.isValidateClusterNodeMembership()).isEqualTo(options.isValidateClusterNodeMembership());
        assertThat(copy.getRequestQueueSize()).isEqualTo(options.getRequestQueueSize());
        assertThat(copy.isAutoReconnect()).isEqualTo(options.isAutoReconnect());
        assertThat(copy.isCancelCommandsOnReconnectFailure()).isEqualTo(options.isCancelCommandsOnReconnectFailure());
        assertThat(copy.isSuspendReconnectOnProtocolFailure()).isEqualTo(options.isSuspendReconnectOnProtocolFailure());
        assertThat(copy.getMaxRedirects()).isEqualTo(options.getMaxRedirects());
        assertThat(options.mutate()).isNotSameAs(copy.mutate());
    }
}
