/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.metrics;

import io.lettuce.core.metrics.CommandMetrics.CommandLatency;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.netty.channel.local.LocalAddress;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.HdrHistogram.Histogram;
import org.LatencyUtils.LatencyStats;
import org.LatencyUtils.PauseDetector;
import org.LatencyUtils.SimplePauseDetector;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static io.lettuce.core.internal.LettuceClassUtils.isPresent;

/**
 * Default implementation of a {@link CommandLatencyCollector} for command latencies.
 *
 * @author Mark Paluch
 */
public class DefaultCommandLatencyCollector implements CommandLatencyCollector {

    private static final AtomicReference<PauseDetectorWrapper> PAUSE_DETECTOR = new AtomicReference<>();
    private static final boolean LATENCY_UTILS_AVAILABLE = isPresent("org.LatencyUtils.PauseDetector");
    private static final boolean HDR_UTILS_AVAILABLE = isPresent("org.HdrHistogram.Histogram");

    private static final long MIN_LATENCY = 1000;
    private static final long MAX_LATENCY = TimeUnit.MINUTES.toNanos(5);

    private final CommandLatencyCollectorOptions options;
    private Map<CommandLatencyId, Latencies> latencyMetrics = new ConcurrentHashMap<>(CommandType.values().length);

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

        Latencies latencies = latencyMetrics.computeIfAbsent(createId(local, remote, commandType), id -> {

            PauseDetectorWrapper wrapper = PAUSE_DETECTOR.get();
            if (wrapper == null) {
                wrapper = new PauseDetectorWrapper();

                if (PAUSE_DETECTOR.compareAndSet(null, wrapper)) {
                    wrapper.initialize();
                }
            }

            if (options.resetLatenciesAfterEvent()) {
                return new Latencies(PAUSE_DETECTOR.get().pauseDetector);
            }
            return new CummulativeLatencies(PAUSE_DETECTOR.get().pauseDetector);
        });

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

        Map<CommandLatencyId, Latencies> copy = new HashMap<>(latencyMetrics);

        if (options.resetLatenciesAfterEvent()) {
            latencyMetrics.clear();
        }

        return getMetrics(copy);
    }

    private Map<CommandLatencyId, CommandMetrics> getMetrics(Map<CommandLatencyId, Latencies> latencyMetrics) {

        Map<CommandLatencyId, CommandMetrics> result = new TreeMap<>();

        for (Map.Entry<CommandLatencyId, Latencies> entry : latencyMetrics.entrySet()) {

            Latencies latencies = entry.getValue();

            Histogram firstResponse = latencies.getFirstResponseHistogram();
            Histogram completion = latencies.getCompletionHistogram();

            if (firstResponse.getTotalCount() == 0 && completion.getTotalCount() == 0) {
                continue;
            }

            CommandLatency firstResponseLatency = getMetric(firstResponse);
            CommandLatency completionLatency = getMetric(completion);

            CommandMetrics metrics = new CommandMetrics(firstResponse.getTotalCount(), options.targetUnit(),
                    firstResponseLatency, completionLatency);

            result.put(entry.getKey(), metrics);
        }

        return result;
    }

    private CommandLatency getMetric(Histogram histogram) {

        Map<Double, Long> percentiles = getPercentiles(histogram);

        TimeUnit timeUnit = options.targetUnit();
        return new CommandLatency(timeUnit.convert(histogram.getMinValue(), TimeUnit.NANOSECONDS), timeUnit.convert(
                histogram.getMaxValue(), TimeUnit.NANOSECONDS), percentiles);
    }

    private Map<Double, Long> getPercentiles(Histogram histogram) {

        Map<Double, Long> percentiles = new TreeMap<Double, Long>();
        for (double targetPercentile : options.targetPercentiles()) {
            percentiles.put(targetPercentile,
                    options.targetUnit().convert(histogram.getValueAtPercentile(targetPercentile), TimeUnit.NANOSECONDS));
        }

        return percentiles;
    }

    /**
     * Returns {@literal true} if HdrUtils and LatencyUtils are available on the class path.
     *
     * @return
     */
    public static boolean isAvailable() {
        return LATENCY_UTILS_AVAILABLE && HDR_UTILS_AVAILABLE;
    }

    /**
     * Returns a disabled no-op {@link CommandLatencyCollector}.
     *
     * @return
     */
    public static CommandLatencyCollector disabled() {

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

    private static class Latencies {

        private final LatencyStats firstResponse;
        private final LatencyStats completion;

        Latencies(PauseDetector pauseDetector) {
            firstResponse = LatencyStats.Builder.create().pauseDetector(pauseDetector).build();
            completion = LatencyStats.Builder.create().pauseDetector(pauseDetector).build();
        }

        public Histogram getFirstResponseHistogram() {
            return firstResponse.getIntervalHistogram();
        }

        public Histogram getCompletionHistogram() {
            return completion.getIntervalHistogram();
        }
    }

    private static class CummulativeLatencies extends Latencies {

        private final Histogram firstResponse;
        private final Histogram completion;

        CummulativeLatencies(PauseDetector pauseDetector) {
            super(pauseDetector);

            firstResponse = super.firstResponse.getIntervalHistogram();
            completion = super.completion.getIntervalHistogram();
        }

        @Override
        public Histogram getFirstResponseHistogram() {

            firstResponse.add(super.getFirstResponseHistogram());
            return firstResponse;
        }

        @Override
        public Histogram getCompletionHistogram() {

            completion.add(super.getFirstResponseHistogram());
            return completion;
        }
    }

    private static class PauseDetectorWrapper {

        private static final AtomicLong counter = new AtomicLong();

        PauseDetector pauseDetector;

        public void initialize() {

            if (counter.getAndIncrement() > 0) {
                InternalLogger instance = InternalLoggerFactory.getInstance(getClass());
                instance.info("Initialized PauseDetectorWrapper more than once.");
            }

            pauseDetector = new SimplePauseDetector(TimeUnit.MILLISECONDS.toNanos(10), TimeUnit.MILLISECONDS.toNanos(10), 3);
            Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook for SimplePauseDetector") {
                @Override
                public void run() {
                    pauseDetector.shutdown();
                }
            });
        }
    }
}
