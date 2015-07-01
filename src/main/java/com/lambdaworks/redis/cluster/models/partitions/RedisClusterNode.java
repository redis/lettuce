package com.lambdaworks.redis.cluster.models.partitions;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.lambdaworks.redis.RedisURI;

/**
 * Representation of a redis cluster node.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisClusterNode implements Serializable {
    private RedisURI uri;
    private String nodeId;

    private boolean connected;
    private String slaveOf;
    private long pingSentTimestamp;
    private long pongReceivedTimestamp;
    private long configEpoch;

    private List<Integer> slots;
    private Set<NodeFlag> flags;

    public RedisClusterNode() {

    }

    public RedisClusterNode(RedisURI uri, String nodeId, boolean connected, String slaveOf, long pingSentTimestamp,
            long pongReceivedTimestamp, long configEpoch, List<Integer> slots, Set<NodeFlag> flags) {
        this.uri = uri;
        this.nodeId = nodeId;
        this.connected = connected;
        this.slaveOf = slaveOf;
        this.pingSentTimestamp = pingSentTimestamp;
        this.pongReceivedTimestamp = pongReceivedTimestamp;
        this.configEpoch = configEpoch;
        this.slots = slots;
        this.flags = flags;
    }

    public RedisURI getUri() {
        return uri;
    }

    public void setUri(RedisURI uri) {
        checkArgument(uri != null, "uri must not be null");
        this.uri = uri;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        checkArgument(nodeId != null, "nodeId must not be null");
        this.nodeId = nodeId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getSlaveOf() {
        return slaveOf;
    }

    public void setSlaveOf(String slaveOf) {
        this.slaveOf = slaveOf;
    }

    public long getPingSentTimestamp() {
        return pingSentTimestamp;
    }

    public void setPingSentTimestamp(long pingSentTimestamp) {
        this.pingSentTimestamp = pingSentTimestamp;
    }

    public long getPongReceivedTimestamp() {
        return pongReceivedTimestamp;
    }

    public void setPongReceivedTimestamp(long pongReceivedTimestamp) {
        this.pongReceivedTimestamp = pongReceivedTimestamp;
    }

    public long getConfigEpoch() {
        return configEpoch;
    }

    public void setConfigEpoch(long configEpoch) {
        this.configEpoch = configEpoch;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public void setSlots(List<Integer> slots) {
        checkArgument(slots != null, "slots must not be null");

        this.slots = slots;
    }

    public Set<NodeFlag> getFlags() {
        return flags;
    }

    public void setFlags(Set<NodeFlag> flags) {
        this.flags = flags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisClusterNode)) {
            return false;
        }

        RedisClusterNode that = (RedisClusterNode) o;

        if (nodeId != null ? !nodeId.equals(that.nodeId) : that.nodeId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 31 * (nodeId != null ? nodeId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [uri=").append(uri);
        sb.append(", nodeId='").append(nodeId).append('\'');
        sb.append(", connected=").append(connected);
        sb.append(", slaveOf='").append(slaveOf).append('\'');
        sb.append(", pingSentTimestamp=").append(pingSentTimestamp);
        sb.append(", pongReceivedTimestamp=").append(pongReceivedTimestamp);
        sb.append(", configEpoch=").append(configEpoch);
        sb.append(", flags=").append(flags);
        if (slots != null) {
            sb.append(", slot count=").append(slots.size());
        }
        sb.append(']');
        return sb.toString();
    }

    public enum NodeFlag {
        NOFLAGS, MYSELF, SLAVE, MASTER, EVENTUAL_FAIL, FAIL, HANDSHAKE, NOADDR;
    }
}
