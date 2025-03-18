package io.lettuce.core.metrics;

public interface ConnectionMonitor {

    static ConnectionMonitor disabled() {

        return new ConnectionMonitor() {

            @Override
            public void recordDisconnectedTime(String epid, long time) {

            }

            @Override
            public void incrementReconnectionAttempts(String epid) {

            }

            @Override
            public boolean isEnabled() {
                return false;
            }

        };
    }

    void recordDisconnectedTime(String epid, long time);

    void incrementReconnectionAttempts(String epid);

    boolean isEnabled();

}
