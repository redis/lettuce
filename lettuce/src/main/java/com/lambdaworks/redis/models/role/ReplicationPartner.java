package com.lambdaworks.redis.models.role;

import com.google.common.net.HostAndPort;

/**
 * Replication partner providing the host and the replication offset.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.08.14 10:45
 */
public class ReplicationPartner {
    private HostAndPort host;
    private long replicationOffset;

    public ReplicationPartner(HostAndPort host, long replicationOffset) {
        this.host = host;
        this.replicationOffset = replicationOffset;
    }

    /**
     *
     * @return host with port of the replication partner.
     */
    public HostAndPort getHost() {
        return host;
    }

    /**
     *
     * @return the replication offset.
     */
    public long getReplicationOffset() {
        return replicationOffset;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [host=").append(host);
        sb.append(", replicationOffset=").append(replicationOffset);
        sb.append(']');
        return sb.toString();
    }
}
