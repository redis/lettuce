package io.lettuce.scenario;

import java.time.Duration;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

/**
 * Utility class that provides pre-configured client options with recommended settings for connection interruption testing. This
 * class helps standardize client configurations across tests and provides optimized settings for fast reconnection.
 */
public class RecommendedSettingsProvider {

    private RecommendedSettingsProvider() {
        // Utility class
    }

    /**
     * Provides optimized client options for connection interruption testing scenarios.
     * 
     * @return ClientOptions configured for connection interruption testing
     */
    public static ClientOptions forConnectionInterruption() {
        return ClientOptions.builder().autoReconnect(true).socketOptions(connectionInterruptionSocketOptions()).build();
    }

    public static SocketOptions connectionInterruptionSocketOptions() {
        SocketOptions.TcpUserTimeoutOptions tcpUserTimeout = SocketOptions.TcpUserTimeoutOptions.builder()
                .tcpUserTimeout(Duration.ofSeconds(4)).enable().build();

        SocketOptions.KeepAliveOptions keepAliveOptions = SocketOptions.KeepAliveOptions.builder()
                .interval(Duration.ofSeconds(1)).idle(Duration.ofSeconds(1)).count(3).enable().build();

        return SocketOptions.builder().tcpUserTimeout(tcpUserTimeout).keepAlive(keepAliveOptions).build();
    }

}
