package com.lambdaworks.redis.cluster.topology;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.CommandType;

/**
 * @author Mark Paluch
 */
class Connections {

    private Map<RedisURI, StatefulRedisConnection<String, String>> connections = new TreeMap<>(
            TopologyComparators.RedisUriComparator.INSTANCE);

    public Connections() {
    }

    private Connections(Map<RedisURI, StatefulRedisConnection<String, String>> connections) {
        this.connections = connections;
    }

    public void addConnection(RedisURI redisURI, StatefulRedisConnection<String, String> connection) {
        connections.put(redisURI, connection);
    }

    /*
     * Initiate {@code CLUSTER NODES} on all connections and return the {@link Requests}.
     *
     * @return the {@link Requests}.
     */
    public Requests requestTopology() {

        Requests requests = new Requests();

        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : connections.entrySet()) {

            CommandArgs<String, String> args = new CommandArgs<>(ClusterTopologyRefresh.CODEC).add(CommandKeyword.NODES);
            Command<String, String, String> command = new Command<>(CommandType.CLUSTER,
                    new StatusOutput<>(ClusterTopologyRefresh.CODEC), args);
            TimedAsyncCommand<String, String, String> timedCommand = new TimedAsyncCommand<>(
                    command);

            entry.getValue().dispatch(timedCommand);
            requests.addRequest(entry.getKey(), timedCommand);
        }

        return requests;
    }

    /*
     * Initiate {@code CLIENT LIST} on all connections and return the {@link Requests}.
     *
     * @return the {@link Requests}.
     */
    public Requests requestClients() {

        Requests requests = new Requests();

        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : connections.entrySet()) {

            CommandArgs<String, String> args = new CommandArgs<>(ClusterTopologyRefresh.CODEC).add(CommandKeyword.LIST);
            Command<String, String, String> command = new Command<>(CommandType.CLIENT,
                    new StatusOutput<>(ClusterTopologyRefresh.CODEC), args);
            TimedAsyncCommand<String, String, String> timedCommand = new TimedAsyncCommand<>(
                    command);

            entry.getValue().dispatch(timedCommand);
            requests.addRequest(entry.getKey(), timedCommand);
        }

        return requests;
    }

    /**
     * Close all connections.
     */
    public void close() {
        for (StatefulRedisConnection<String, String> connection : connections.values()) {
            connection.close();
        }
    }

    public Set<RedisURI> nodes() {
        return connections.keySet();
    }

    public Connections mergeWith(Connections discoveredConnections) {

        Map<RedisURI, StatefulRedisConnection<String, String>> result = new TreeMap<>(
                TopologyComparators.RedisUriComparator.INSTANCE);
        result.putAll(this.connections);
        result.putAll(discoveredConnections.connections);

        return new Connections(result);
    }
}
