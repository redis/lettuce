package io.lettuce.core.metrics;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.Map;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * {@link MetricCollector} for command latencies. Command latencies are collected per connection (identified by local/remote
 * tuples of {@link SocketAddress}es) and {@link ProtocolKeyword command type}. Two command latencies are available:
 * <ul>
 * <li>Latency between command send and first response (first response received)</li>
 * <li>Latency between command send and command completion (complete response received)</li>
 * </ul>
 *
 * @author Mark Paluch
 * @since 3.4
 */
public interface CommandLatencyCollector
        extends MetricCollector<Map<CommandLatencyId, CommandMetrics>>, CommandLatencyRecorder {

    /**
     * Creates a new {@link CommandLatencyCollector} using {@link CommandLatencyCollectorOptions}.
     *
     * @param options must not be {@code null}.
     * @return the {@link CommandLatencyCollector} using {@link CommandLatencyCollectorOptions}.
     */
    static CommandLatencyCollector create(CommandLatencyCollectorOptions options) {
        return new DefaultCommandLatencyCollector(options);
    }

    /**
     * Returns a disabled no-op {@link CommandLatencyCollector}.
     *
     * @return a disabled no-op {@link CommandLatencyCollector}.
     * @since 5.1
     * @deprecated since 6.0, use {@link CommandLatencyRecorder#disabled()} instead.
     */
    @Deprecated
    static CommandLatencyCollector disabled() {

        return new CommandLatencyCollector() {

            @Override
            public void recordCommandLatency(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType,
                    long firstResponseLatency, long completionLatency) {
            }

            @Override
            public void shutdown() {
            }

            @Override
            public Map<CommandLatencyId, CommandMetrics> retrieveMetrics() {
                return Collections.emptyMap();
            }

            @Override
            public boolean isEnabled() {
                return false;
            }

        };
    }

}
