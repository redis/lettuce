package com.lambdaworks.redis.cluster.models.slots;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.net.HostAndPort;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Represents a range of slots together with its master and slaves.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ClusterSlotRange implements Serializable {
    private int from;
    private int to;

    @Deprecated
    private HostAndPort master;

    private RedisClusterNode masterNode;

    @Deprecated
    private List<HostAndPort> slaves = Collections.emptyList();

    private List<RedisClusterNode> slaveNodes = Collections.emptyList();

    public ClusterSlotRange() {

    }

    /**
     * Constructs a {@link ClusterSlotRange}
     * 
     * @param from from slot
     * @param to to slot
     * @param master master for the slots, may be {@literal null}
     * @param slaves list of slaves must not be {@literal null} but may be empty
     * @deprecated Use {@link #ClusterSlotRange(int, int, RedisClusterNode, List)}
     */
    @Deprecated
    public ClusterSlotRange(int from, int to, HostAndPort master, List<HostAndPort> slaves) {

        LettuceAssert.notNull(master, "master must not be null");
        LettuceAssert.notNull(slaves, "slaves must not be null");

        this.from = from;
        this.to = to;
        this.masterNode = toRedisClusterNode(master, null, Collections.singleton(RedisClusterNode.NodeFlag.MASTER));
        this.slaveNodes = toRedisClusterNodes(slaves, null, Collections.singleton(RedisClusterNode.NodeFlag.SLAVE));
        this.master = master;
        this.slaves = slaves;
    }

    /**
     * Constructs a {@link ClusterSlotRange}
     *
     * @param from from slot
     * @param to to slot
     * @param masterNode master for the slots, may be {@literal null}
     * @param slaveNodes list of slaves must not be {@literal null} but may be empty
     */
    public ClusterSlotRange(int from, int to, RedisClusterNode masterNode, List<RedisClusterNode> slaveNodes) {

        LettuceAssert.notNull(masterNode, "masterNode must not be null");
        LettuceAssert.notNull(slaveNodes, "slaveNodes must not be null");

        this.from = from;
        this.to = to;
        this.master = toHostAndPort(masterNode);
        this.slaves = toHostAndPorts(slaveNodes);
        this.masterNode = masterNode;
        this.slaveNodes = slaveNodes;
    }

    private HostAndPort toHostAndPort(RedisClusterNode redisClusterNode) {
        RedisURI uri = redisClusterNode.getUri();
        return HostAndPort.fromParts(uri.getHost(), uri.getPort());
    }

    private List<HostAndPort> toHostAndPorts(List<RedisClusterNode> nodes) {
        List<HostAndPort> result = new ArrayList<>();
        for (RedisClusterNode node : nodes) {
            result.add(toHostAndPort(node));
        }
        return result;
    }

    private RedisClusterNode toRedisClusterNode(HostAndPort hostAndPort, String slaveOf, Set<RedisClusterNode.NodeFlag> flags) {
        RedisClusterNode redisClusterNode = new RedisClusterNode();
        redisClusterNode.setUri(RedisURI
                .create(hostAndPort.getHostText(), hostAndPort.getPortOrDefault(RedisURI.DEFAULT_REDIS_PORT)));
        redisClusterNode.setSlaveOf(slaveOf);
        redisClusterNode.setFlags(flags);
        return redisClusterNode;
    }

    private List<RedisClusterNode> toRedisClusterNodes(List<HostAndPort> hostAndPorts, String slaveOf, Set<RedisClusterNode.NodeFlag> flags) {
        List<RedisClusterNode> result = new ArrayList<>();
        for (HostAndPort hostAndPort : hostAndPorts) {
            result.add(toRedisClusterNode(hostAndPort, slaveOf, flags));
        }
        return result;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    /**
     * @deprecated Use {@link #getMasterNode()} to retrieve the {@code nodeId} and the {@code slaveOf} details.
     * @return the master host and port
     */
    @Deprecated
    public HostAndPort getMaster() {
        return master;
    }

    /**
     * @deprecated Use {@link #getSlaveNodes()} to retrieve the {@code nodeId} and the {@code slaveOf} details.
     * @return the master host and port
     */
    @Deprecated
    public List<HostAndPort> getSlaves() {
        return slaves;
    }

    public RedisClusterNode getMasterNode() {
        return masterNode;
    }

    public void setMasterNode(RedisClusterNode masterNode) {
        this.masterNode = masterNode;
    }

    public List<RedisClusterNode> getSlaveNodes() {
        return slaveNodes;
    }

    public void setSlaveNodes(List<RedisClusterNode> slaveNodes) {
        this.slaveNodes = slaveNodes;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public void setMaster(HostAndPort master) {
        LettuceAssert.notNull(master, "master must not be null");
        this.master = master;
    }

    public void setSlaves(List<HostAndPort> slaves) {

        LettuceAssert.notNull(slaves, "slaves must not be null");
        this.slaves = slaves;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [from=").append(from);
        sb.append(", to=").append(to);
        sb.append(", masterNode=").append(masterNode);
        sb.append(", slaveNodes=").append(slaveNodes);
        sb.append(']');
        return sb.toString();
    }
}
