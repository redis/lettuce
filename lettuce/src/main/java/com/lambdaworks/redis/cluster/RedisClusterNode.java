package com.lambdaworks.redis.cluster;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.lambdaworks.redis.RedisURI;

/**
 * Representation of a redis cluster node.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:09
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

    public RedisURI getUri() {
        return uri;
    }

    void setUri(RedisURI uri) {
        this.uri = uri;
    }

    public String getNodeId() {
        return nodeId;
    }

    void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public Set<NodeFlag> getFlags() {
        return flags;
    }

    void setSlots(List<Integer> slots) {
        this.slots = slots;
    }

    void setFlags(Set<NodeFlag> flags) {
        this.flags = flags;
    }

    public boolean isConnected() {
        return connected;
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getSlaveOf() {
        return slaveOf;
    }

    void setSlaveOf(String slaveOf) {
        this.slaveOf = slaveOf;
    }

    public long getPingSentTimestamp() {
        return pingSentTimestamp;
    }

    void setPingSentTimestamp(long pingSentTimestamp) {
        this.pingSentTimestamp = pingSentTimestamp;
    }

    public long getPongReceivedTimestamp() {
        return pongReceivedTimestamp;
    }

    void setPongReceivedTimestamp(long pongReceivedTimestamp) {
        this.pongReceivedTimestamp = pongReceivedTimestamp;
    }

    public long getConfigEpoch() {
        return configEpoch;
    }

    void setConfigEpoch(long configEpoch) {
        this.configEpoch = configEpoch;
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

        if (uri != null ? !uri.equals(that.uri) : that.uri != null) {
            return false;
        }
        if (nodeId != null ? !nodeId.equals(that.nodeId) : that.nodeId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = uri != null ? uri.hashCode() : 0;
        result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
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
