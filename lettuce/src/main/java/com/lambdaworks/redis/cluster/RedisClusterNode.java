package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.RedisURI;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:09
 */
public class RedisClusterNode {
    private RedisURI uri;
    private String nodeId;

    private boolean connected;
    private String slaveOf;
    private long pingSentTimestamp;
    private long pongReceivedTimestamp;
    private long configEpoch;

    private List<Integer> slots = Lists.newArrayList();
    private Set<NodeFlag> flags = Sets.newHashSet();

    public RedisURI getUri() {
        return uri;
    }

    public void setUri(RedisURI uri) {
        this.uri = uri;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public void addSlot(int slot) {
        slots.add(slot);

    }

    public Set<NodeFlag> getFlags() {
        return flags;
    }

    public void addFlag(NodeFlag nodeFlag) {
        flags.add(nodeFlag);
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
        sb.append(", slot count=").append(slots.size());
        sb.append(']');
        return sb.toString();
    }

    public enum NodeFlag {
        NOFLAGS, MYSELF, SLAVE, MASTER, EVENTUAL_FAIL, FAIL, HANDSHAKE, NOADDR;
    }
}
