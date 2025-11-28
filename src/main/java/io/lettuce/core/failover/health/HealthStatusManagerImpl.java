package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HealthStatusManagerImpl implements HealthStatusManager, AutoCloseable {

    private final HealthCheckCollection healthChecks = new HealthCheckCollection();

    private final List<HealthStatusListener> listeners = new CopyOnWriteArrayList<>();

    private final Map<RedisURI, List<HealthStatusListener>> endpointListeners = new ConcurrentHashMap<RedisURI, List<HealthStatusListener>>();

    @Override
    public void registerListener(HealthStatusListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(HealthStatusListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void registerListener(RedisURI endpoint, HealthStatusListener listener) {
        endpointListeners.computeIfAbsent(endpoint, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(RedisURI endpoint, HealthStatusListener listener) {
        endpointListeners.computeIfPresent(endpoint, (k, v) -> {
            v.remove(listener);
            return v;
        });
    }

    public void notifyListeners(HealthStatusChangeEvent eventArgs) {
        endpointListeners.computeIfPresent(eventArgs.getEndpoint(), (k, v) -> {
            for (HealthStatusListener listener : v) {
                listener.onStatusChange(eventArgs);
            }
            return v;
        });
        for (HealthStatusListener listener : listeners) {
            listener.onStatusChange(eventArgs);
        }
    }

    @Override
    public HealthCheck add(RedisURI endpoint, HealthCheckStrategy strategy) {
        HealthCheck hc = new HealthCheckImpl(endpoint, strategy);

        // Register the manager as a listener to forward events to registered listeners
        hc.addListener(this::notifyListeners);

        HealthCheck old = healthChecks.add(hc);
        hc.start();
        if (old != null) {
            old.stop();
        }
        return hc;
    }

    @Override
    public void remove(RedisURI endpoint) {
        HealthCheck old = healthChecks.remove(endpoint);
        if (old != null) {
            old.stop();
        }
    }

    @Override
    public HealthStatus getHealthStatus(RedisURI endpoint) {
        HealthCheck healthCheck = healthChecks.get(endpoint);
        return healthCheck != null ? healthCheck.getStatus() : HealthStatus.UNKNOWN;
    }

    @Override
    public long getMaxWaitFor(RedisURI endpoint) {
        HealthCheck healthCheck = healthChecks.get(endpoint);
        return healthCheck != null ? healthCheck.getMaxWaitFor() : 0;
    }

    @Override
    public void close() {
        healthChecks.close();
    }

}
