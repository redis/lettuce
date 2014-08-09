package com.lambdaworks.redis.models.role;

import java.io.Serializable;
import java.util.List;

/**
 * Redis sentinel instance.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.08.14 11:44
 */
@SuppressWarnings("serial")
public class RedisSentinelInstance implements RedisInstance, Serializable {
    private List<String> monitoredMasters;

	protected RedisSentinelInstance() {
	}

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
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [monitoredMasters=").append(monitoredMasters);
        sb.append(']');
        return sb.toString();
    }
}
