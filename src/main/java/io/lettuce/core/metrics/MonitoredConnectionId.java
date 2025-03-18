package io.lettuce.core.metrics;

import io.lettuce.core.internal.LettuceAssert;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 * Identifier for a connection
 *
 * @author Ivo Gaydajiev
 */
@SuppressWarnings("serial")
public class MonitoredConnectionId implements Serializable, Comparable<MonitoredConnectionId> {

    private final String epId;

    protected MonitoredConnectionId(String epId) {
        LettuceAssert.notNull(epId, "EndpointId must not be null");

        this.epId = epId;
    }

    /**
     * Create a new instance of {@link MonitoredConnectionId}.
     *
     * @param epId the endpoint id
     * @return a new instance of {@link MonitoredConnectionId}
     */
    public static MonitoredConnectionId create(String epId) {
        return new MonitoredConnectionId(epId);
    }

    /**
     * Returns the command type.
     *
     * @return the command type
     */
    public String epId() {
        return epId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MonitoredConnectionId))
            return false;

        MonitoredConnectionId that = (MonitoredConnectionId) o;

        return epId.equals(that.epId);
    }

    @Override
    public int hashCode() {
        return epId.hashCode();
    }

    @Override
    public int compareTo(MonitoredConnectionId o) {

        if (o == null) {
            return -1;
        }

        return epId.compareTo(o.epId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append("epId=").append(epId).append(']');
        return sb.toString();
    }

}
