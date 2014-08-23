package com.lambdaworks.redis.models.role;

import static com.google.common.base.Preconditions.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Redis sentinel instance.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisSentinelInstance implements RedisInstance, Serializable {
    private List<String> monitoredMasters = Collections.emptyList();

    public RedisSentinelInstance() {
    }

    /**
     * Constructs a {@link RedisSentinelInstance}
     * 
     * @param monitoredMasters list of monitored masters, must not be {@literal null} but may be empty
     */
    public RedisSentinelInstance(List<String> monitoredMasters) {
        checkArgument(monitoredMasters != null, "list of monitoredMasters must not be null");
        this.monitoredMasters = monitoredMasters;
    }

    /**
     *
     * @return always {@link com.lambdaworks.redis.models.role.RedisInstance.Role#SENTINEL}
     */
    @Override
    public Role getRole() {
        return Role.SENTINEL;
    }

    /**
     *
     * @return List of monitored master names.
     */
    public List<String> getMonitoredMasters() {
        return monitoredMasters;
    }

    public void setMonitoredMasters(List<String> monitoredMasters) {
        checkArgument(monitoredMasters != null, "list of monitoredMasters must not be null");
        this.monitoredMasters = monitoredMasters;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [monitoredMasters=").append(monitoredMasters);
        sb.append(']');
        return sb.toString();
    }
}
