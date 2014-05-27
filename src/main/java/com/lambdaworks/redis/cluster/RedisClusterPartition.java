package com.lambdaworks.redis.cluster;

import java.util.List;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.RedisURI;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:09
 */
public class RedisClusterPartition {
    private RedisURI uri;
    private String nodeId;

    private List<Integer> slots = Lists.newArrayList();

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

    public void setSlots(List<Integer> slots) {
        this.slots = slots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisClusterPartition)) {
            return false;
        }

        RedisClusterPartition that = (RedisClusterPartition) o;

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
}
