/*
 * Copyright 2017-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.protocol.ConnectionIntent;

/**
 * Specialized {@link Function} to obtain connections for Redis Cluster nodes. Connecting to a node returns a
 * {@link CompletableFuture} for asynchronous connection and late synchronization.
 *
 * @author Mark Paluch
 * @since 5.0
 */
interface ClusterNodeConnectionFactory<K, V>
        extends Function<ClusterNodeConnectionFactory.ConnectionKey, ConnectionFuture<StatefulRedisConnection<K, V>>> {

    /**
     * Set the {@link Partitions}.
     *
     * @param partitions
     */
    void setPartitions(Partitions partitions);

    /**
     * Connection to identify a connection either by nodeId or host/port.
     */
    class ConnectionKey {

        final ConnectionIntent connectionIntent;

        final String nodeId;

        final String host;

        final int port;

        public ConnectionKey(ConnectionIntent connectionIntent, String nodeId) {
            this.connectionIntent = connectionIntent;
            this.nodeId = nodeId;
            this.host = null;
            this.port = 0;
        }

        public ConnectionKey(ConnectionIntent connectionIntent, String host, int port) {
            this.connectionIntent = connectionIntent;
            this.host = host;
            this.port = port;
            this.nodeId = null;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o)
                return true;
            if (!(o instanceof ConnectionKey))
                return false;

            ConnectionKey key = (ConnectionKey) o;

            if (port != key.port)
                return false;
            if (connectionIntent != key.connectionIntent)
                return false;
            if (nodeId != null ? !nodeId.equals(key.nodeId) : key.nodeId != null)
                return false;
            return !(host != null ? !host.equals(key.host) : key.host != null);
        }

        @Override
        public int hashCode() {

            int result = connectionIntent != null ? connectionIntent.name().hashCode() : 0;
            result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
            result = 31 * result + (host != null ? host.hashCode() : 0);
            result = 31 * result + port;
            return result;
        }

        @Override
        public String toString() {

            StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append(" [connectionIntent=").append(connectionIntent);
            sb.append(", nodeId='").append(nodeId).append('\'');
            sb.append(", host='").append(host).append('\'');
            sb.append(", port=").append(port);
            sb.append(']');
            return sb.toString();
        }

    }

}
