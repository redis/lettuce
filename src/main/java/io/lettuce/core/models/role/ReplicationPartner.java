/*
 * Copyright 2011-2020 the original author or authors.
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
