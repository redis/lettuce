/*
 * Copyright 2011-2024 the original author or authors.
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

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.DefaultAutoBatchFlushEndpoint;
import io.lettuce.core.resource.ClientResources;

/**
 * Command handler for node connections within the Redis Cluster context. This handler can requeue commands if it is
 * disconnected and closed but has commands in the queue. If the handler was connected it would retry commands using the
 * {@literal MOVED} or {@literal ASK} redirection.
 *
 * @author Mark Paluch
 */
public class ClusterNodeAutoBatchFlushEndpoint extends DefaultAutoBatchFlushEndpoint {

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection.
     * @param clientResources client resources for this connection.
     * @param clusterChannelWriter top-most channel writer.
     */
    public ClusterNodeAutoBatchFlushEndpoint(ClientOptions clientOptions, ClientResources clientResources,
            RedisChannelWriter clusterChannelWriter) {
        super(clientOptions, clientResources, clusterChannelWriter != null ? cmd -> {
            if (cmd.isDone()) {
                return;
            }

            try {
                clusterChannelWriter.write(cmd);
            } catch (RedisException e) {
                cmd.completeExceptionally(e);
            }
        } : DefaultAutoBatchFlushEndpoint::cancelCommandOnEndpointClose);
    }

}
