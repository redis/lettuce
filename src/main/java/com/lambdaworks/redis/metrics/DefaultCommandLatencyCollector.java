package com.lambdaworks.redis.metrics;

import java.net.SocketAddress;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.protocol.CommandType;
import org.HdrHistogram.Histogram;
import org.LatencyUtils.LatencyStats;
import org.LatencyUtils.PauseDetector;
import org.LatencyUtils.SimplePauseDetector;

import com.google.common.collect.Maps;
import com.lambdaworks.redis.metrics.CommandMetrics.CommandLatency;
import com.lambdaworks.redis.protocol.ProtocolKeyword;
import io.netty.channel.local.LocalAddress;

/**
 * Default implementation of a {@link CommandLatencyCollector} for command latencies.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class DefaultCommandLatencyCollector implements CommandLatencyCollector {

    private static final PauseDetector PAUSE_DETECTOR = new SimplePauseDetector(TimeUnit.MILLISECONDS.toNanos(10),
            TimeUnit.MILLISECONDS.toNanos(10), 3);

    private static final long MIN_LATENCY = 1000;
    private static final long MAX_LATENCY = TimeUnit.MINUTES.toNanos(5);

    private final CommandLatencyCollectorOptions options;
    private Map<CommandLatencyId, Latencies> latencyMetrics = Maps.newConcurrentMap();

    public DefaultCommandLatencyCollector(CommandLatencyCollectorOptions options) {
        this.options = options;
    }

    /**
     * Record the command latency per {@code connectionPoint} and {@code commandType}.
     * 
     * @param local the local address
     * @param remote the remote address
     * @param commandType the command type
     * @param firstResponseLatency latency value in {@link TimeUnit#NANOSECONDS} from send to the first response
     * @param completionLatency latency value in {@link TimeUnit#NANOSECONDS} from send to the command completion
     */
    public void recordCommandLatency(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType,
            long firstResponseLatency, long completionLatency) {
        if (!isEnabled()) {
            return;
        }

        CommandLatencyId id = createId(local, remote, commandType);
        Latencies latencies = latencyMetrics.get(id);
        if (latencies == null) {
            latencies = new Latencies();
            latencyMetrics.put(id, latencies);
        }

        latencies.firstResponse.recordLatency(rangify(firstResponseLatency));
        latencies.completion.recordLatency(rangify(completionLatency));

    }

    private CommandLatencyId createId(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType) {
        return CommandLatencyId.create(options.localDistinction() ? local : LocalAddress.ANY, remote, commandType);
    }

    private long rangify(long latency) {
        return Math.max(MIN_LATENCY, Math.min(MAX_LATENCY, latency));
    }

    @Override
    public boolean isEnabled() {
        return latencyMetrics != null && options.isEnabled();
    }

    @Override
    public void shutdown() {
        if (latencyMetrics != null) {
            latencyMetrics.clear();
            latencyMetrics = null;
        }
    }

    @Override
    public Map<CommandLatencyId, CommandMetrics> retrieveMetrics() {
        Map<CommandLatencyId, Latencies> copy = Maps.newHashMap();
        copy.putAll(latencyMetrics);
        if (options.resetLatenciesAfterEvent()) {
            latencyMetrics.clear();
        }

        Map<CommandLatencyId, CommandMetrics> latencies = getMetrics(copy);
        return latencies;
    }

    private Map<CommandLatencyId, CommandMetrics> getMetrics(Map<CommandLatencyId, Latencies> latencyMetrics) {
        Map<CommandLatencyId, CommandMetrics> latencies = Maps.newTreeMap();

        for (Map.Entry<CommandLatencyId, Latencies> entry : latencyMetrics.entrySet()) {
            Histogram firstResponse = entry.getValue().firstResponse.getIntervalHistogram();
            Histogram completion = entry.getValue().completion.getIntervalHistogram();

            if (firstResponse.getTotalCount() == 0 && completion.getTotalCount() == 0) {
                continue;
            }

            CommandLatency firstResponseLatency = getMetric(firstResponse);
            CommandLatency completionLatency = getMetric(completion);

            CommandMetrics metrics = new CommandMetrics(firstResponse.getTotalCount(), options.targetUnit(),
                    firstResponseLatency, completionLatency);

            latencies.put(entry.getKey(), metrics);
        }
        return latencies;
    }

    private CommandLatency getMetric(Histogram histogram) {
        Map<Double, Long> percentiles = getPercentiles(histogram);

        TimeUnit timeUnit = options.targetUnit();
        CommandLatency metric = new CommandLatency(timeUnit.convert(histogram.getMinValue(), TimeUnit.NANOSECONDS),
                timeUnit.convert(histogram.getMaxValue(), TimeUnit.NANOSECONDS), percentiles);

        return metric;
    }

    private Map<Double, Long> getPercentiles(Histogram histogram) {
        Map<Double, Long> percentiles = new TreeMap<Double, Long>();
        for (double targetPercentile : options.targetPercentiles()) {
            percentiles.put(targetPercentile,
                    options.targetUnit().convert(histogram.getValueAtPercentile(targetPercentile), TimeUnit.NANOSECONDS));
        }
        return percentiles;
    }

    private static class Latencies {

        public LatencyStats firstResponse = LatencyStats.Builder.create().pauseDetector(PAUSE_DETECTOR).build();
        public LatencyStats completion = LatencyStats.Builder.create().pauseDetector(PAUSE_DETECTOR).build();
    }
}
