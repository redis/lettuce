/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubListener;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.PubSubOutput;

/**
 * @author Mark Paluch
 */
public class PubSubClusterEndpoint<K, V> extends PubSubEndpoint<K, V> {

    private final List<RedisClusterPubSubListener<K, V>> clusterListeners = new CopyOnWriteArrayList<>();
    private final NotifyingMessageListener multicast = new NotifyingMessageListener();
    private final UpstreamMessageListener upstream = new UpstreamMessageListener();

    private volatile boolean nodeMessagePropagation = false;
    private volatile RedisClusterNode clusterNode;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection, must not be {@literal null}
     */
    public PubSubClusterEndpoint(ClientOptions clientOptions) {
        super(clientOptions);
    }

    /**
     * Add a new {@link RedisClusterPubSubListener listener}.
     *
     * @param listener the listener, must not be {@literal null}.
     */
    public void addListener(RedisClusterPubSubListener<K, V> listener) {
        clusterListeners.add(listener);
    }

    public RedisClusterPubSubListener<K, V> getUpstreamListener() {
        return upstream;
    }

    /**
     * Remove an existing {@link RedisClusterPubSubListener listener}.
     *
     * @param listener the listener, must not be {@literal null}.
     */
    public void removeListener(RedisClusterPubSubListener<K, V> listener) {
        clusterListeners.remove(listener);
    }

    public void setNodeMessagePropagation(boolean nodeMessagePropagation) {
        this.nodeMessagePropagation = nodeMessagePropagation;
    }

    void setClusterNode(RedisClusterNode clusterNode) {
        this.clusterNode = clusterNode;
    }

    protected void notifyListeners(PubSubOutput<K, V, V> output) {
        // update listeners
        switch (output.type()) {
            case message:
                multicast.message(clusterNode, output.channel(), output.get());
                break;
            case pmessage:
                multicast.message(clusterNode, output.pattern(), output.channel(), output.get());
                break;
            case psubscribe:
                multicast.psubscribed(clusterNode, output.pattern(), output.count());
                break;
            case punsubscribe:
                multicast.punsubscribed(clusterNode, output.pattern(), output.count());
                break;
            case subscribe:
                multicast.subscribed(clusterNode, output.channel(), output.count());
                break;
            case unsubscribe:
                multicast.unsubscribed(clusterNode, output.channel(), output.count());
                break;
            default:
                throw new UnsupportedOperationException("Operation " + output.type() + " not supported");
        }
    }

    private class UpstreamMessageListener extends NotifyingMessageListener {

        @Override
        public void message(RedisClusterNode node, K channel, V message) {

            if (nodeMessagePropagation) {
                super.message(node, channel, message);
            }
        }

        @Override
        public void message(RedisClusterNode node, K pattern, K channel, V message) {

            if (nodeMessagePropagation) {
                super.message(node, pattern, channel, message);
            }
        }

        @Override
        public void subscribed(RedisClusterNode node, K channel, long count) {

            if (nodeMessagePropagation) {
                super.subscribed(node, channel, count);
            }
        }

        @Override
        public void psubscribed(RedisClusterNode node, K pattern, long count) {

            if (nodeMessagePropagation) {
                super.psubscribed(node, pattern, count);
            }
        }

        @Override
        public void unsubscribed(RedisClusterNode node, K channel, long count) {

            if (nodeMessagePropagation) {
                super.unsubscribed(node, channel, count);
            }
        }

        @Override
        public void punsubscribed(RedisClusterNode node, K pattern, long count) {

            if (nodeMessagePropagation) {
                super.punsubscribed(node, pattern, count);
            }
        }
    }

    private class NotifyingMessageListener extends RedisClusterPubSubAdapter<K, V> {

        @Override
        public void message(RedisClusterNode node, K channel, V message) {

            getListeners().forEach(listener -> listener.message(channel, message));
            clusterListeners.forEach(listener -> listener.message(node, channel, message));
        }

        @Override
        public void message(RedisClusterNode node, K pattern, K channel, V message) {

            getListeners().forEach(listener -> listener.message(pattern, channel, message));
            clusterListeners.forEach(listener -> listener.message(node, pattern, channel, message));
        }

        @Override
        public void subscribed(RedisClusterNode node, K channel, long count) {

            getListeners().forEach(listener -> listener.subscribed(channel, count));
            clusterListeners.forEach(listener -> listener.subscribed(node, channel, count));
        }

        @Override
        public void psubscribed(RedisClusterNode node, K pattern, long count) {

            getListeners().forEach(listener -> listener.psubscribed(pattern, count));
            clusterListeners.forEach(listener -> listener.psubscribed(node, pattern, count));
        }

        @Override
        public void unsubscribed(RedisClusterNode node, K channel, long count) {

            getListeners().forEach(listener -> listener.unsubscribed(channel, count));
            clusterListeners.forEach(listener -> listener.unsubscribed(node, channel, count));
        }

        @Override
        public void punsubscribed(RedisClusterNode node, K pattern, long count) {

            getListeners().forEach(listener -> listener.punsubscribed(pattern, count));
            clusterListeners.forEach(listener -> listener.punsubscribed(node, pattern, count));
        }
    }
}
