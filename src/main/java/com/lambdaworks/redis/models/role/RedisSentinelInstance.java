package com.lambdaworks.redis.models.role;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Redis sentinel instance.
 * 
 * @author Mark Paluch
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
        LettuceAssert.notNull(monitoredMasters, "list of monitoredMasters must not be null");
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
        LettuceAssert.notNull(monitoredMasters, "list of monitoredMasters must not be null");
        this.monitoredMasters = monitoredMasters;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [monitoredMasters=").append(monitoredMasters);
        sb.append(']');
        return sb.toString();
    }
}
