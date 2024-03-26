package io.lettuce.core.models.role;

import java.io.Serializable;

import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Replication partner providing the host and the replication offset.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ReplicationPartner implements Serializable {

    private HostAndPort host;

    private long replicationOffset;

    public ReplicationPartner() {

    }

    /**
     * Constructs a replication partner.
     *
     * @param host host information, must not be {@code null}
     * @param replicationOffset the replication offset
     */
    public ReplicationPartner(HostAndPort host, long replicationOffset) {
        LettuceAssert.notNull(host, "Host must not be null");
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

    public void setHost(HostAndPort host) {
        LettuceAssert.notNull(host, "Host must not be null");
        this.host = host;
    }

    public void setReplicationOffset(long replicationOffset) {
        this.replicationOffset = replicationOffset;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [host=").append(host);
        sb.append(", replicationOffset=").append(replicationOffset);
        sb.append(']');
        return sb.toString();
    }

}
