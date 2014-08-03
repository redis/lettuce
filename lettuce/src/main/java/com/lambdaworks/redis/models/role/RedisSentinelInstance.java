package com.lambdaworks.redis.models.role;

import java.io.Serializable;
import java.util.List;

/**
 * Redis sentinel instance.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.08.14 11:44
 */
public class RedisSentinelInstance implements RedisInstance, Serializable {
    private List<String> monitoredMasters;

    public RedisSentinelInstance(List<String> monitoredMasters) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisSentinelInstance)) {
            return false;
        }

        RedisSentinelInstance that = (RedisSentinelInstance) o;

        if (monitoredMasters != null ? !monitoredMasters.equals(that.monitoredMasters) : that.monitoredMasters != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return monitoredMasters != null ? monitoredMasters.hashCode() : 0;
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
