/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.metrics;

import static io.lettuce.core.internal.LettuceClassUtils.*;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.HdrHistogram.Histogram;
import org.LatencyUtils.LatencyStats;
import org.LatencyUtils.PauseDetector;
import org.LatencyUtils.PauseDetectorListener;
import org.LatencyUtils.SimplePauseDetector;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.metrics.CommandMetrics.CommandLatency;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.netty.channel.local.LocalAddress;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Default implementation of a {@link CommandLatencyCollector} for command latencies.
 *
 * @author Mark Paluch
 * @author Bryce J. Fisher
 */
public class DefaultCommandLatencyCollector implements CommandLatencyCollector {

    private static final AtomicReferenceFieldUpdater<DefaultCommandLatencyCollector, PauseDetectorWrapper> PAUSE_DETECTOR_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(DefaultCommandLatencyCollector.class, PauseDetectorWrapper.class, "pauseDetectorWrapper");

    private static final boolean LATENCY_UTILS_AVAILABLE = isPresent("org.LatencyUtils.PauseDetector");

    private static final boolean HDR_UTILS_AVAILABLE = isPresent("org.HdrHistogram.Histogram");

    private static final PauseDetectorWrapper GLOBAL_PAUSE_DETECTOR = PauseDetectorWrapper.create();

    private static final PauseDetectorWrapper GLOBAL_NO_PAUSE_DETECTOR = PauseDetectorWrapper.noop();

    private static final long MIN_LATENCY = 1000;

    private static final long MAX_LATENCY = TimeUnit.MINUTES.toNanos(5);

    private final CommandLatencyCollectorOptions options;

    private final AtomicReference<Map<CommandLatencyId, Latencies>> latencyMetricsRef = new AtomicReference<>(
            createNewLatencyMap());

    // Updated via PAUSE_DETECTOR_UPDATER
    private volatile PauseDetectorWrapper pauseDetectorWrapper;

    private volatile boolean stopped;

    public DefaultCommandLatencyCollector(CommandLatencyCollectorOptions options) {

        LettuceAssert.notNull(options, "CommandLatencyCollectorOptions must not be null");

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
    @Override
    public void recordCommandLatency(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType,
            long firstResponseLatency, long completionLatency) {

        if (!isEnabled()) {
            return;
        }

        PauseDetector pauseDetector;

        do {
            if (PAUSE_DETECTOR_UPDATER.get(this) == null) {
                if (PAUSE_DETECTOR_UPDATER.compareAndSet(this, null,
                        options.usePauseDetector() ? GLOBAL_PAUSE_DETECTOR : GLOBAL_NO_PAUSE_DETECTOR)) {
                    PAUSE_DETECTOR_UPDATER.get(this).retain();
                }
            }
            PauseDetectorWrapper pauseDetectorWrapper = PAUSE_DETECTOR_UPDATER.get(this);
            pauseDetector = (PauseDetector) pauseDetectorWrapper.getPauseDetector();
        } while (pauseDetector == null);

        PauseDetector pauseDetectorToUse = pauseDetector;
        Latencies latencies = latencyMetricsRef.get().computeIfAbsent(createId(local, remote, commandType), id -> {

            if (options.resetLatenciesAfterEvent()) {
                return new Latencies(pauseDetectorToUse);
            }

            return new CummulativeLatencies(pauseDetectorToUse);
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
        return options.isEnabled() && !stopped;
    }

    @Override
    public void shutdown() {

        stopped = true;

        PauseDetectorWrapper pauseDetectorWrapper = PAUSE_DETECTOR_UPDATER.get(this);
        if (pauseDetectorWrapper != null && PAUSE_DETECTOR_UPDATER.compareAndSet(this, pauseDetectorWrapper, null)) {
            pauseDetectorWrapper.release();
        }

        Map<CommandLatencyId, Latencies> latenciesMap = latencyMetricsRef.get();
        if (latencyMetricsRef.compareAndSet(latenciesMap, Collections.emptyMap())) {
            latenciesMap.values().forEach(Latencies::stop);
        }
    }

    @Override
    public Map<CommandLatencyId, CommandMetrics> retrieveMetrics() {

        Map<CommandLatencyId, Latencies> latenciesMap = latencyMetricsRef.get();
        Map<CommandLatencyId, Latencies> metricsToUse;

        if (options.resetLatenciesAfterEvent()) {

            metricsToUse = latenciesMap;
            latencyMetricsRef.set(createNewLatencyMap());

            metricsToUse.values().forEach(Latencies::stop);
        } else {
            metricsToUse = new HashMap<>(latenciesMap);
        }

        return getMetrics(metricsToUse);
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
        return new CommandLatency(timeUnit.convert(histogram.getMinValue(), TimeUnit.NANOSECONDS),
                timeUnit.convert(histogram.getMaxValue(), TimeUnit.NANOSECONDS), percentiles);
    }

    private Map<Double, Long> getPercentiles(Histogram histogram) {

        Map<Double, Long> percentiles = new TreeMap<>();
        for (double targetPercentile : options.targetPercentiles()) {
            percentiles.put(targetPercentile,
                    options.targetUnit().convert(histogram.getValueAtPercentile(targetPercentile), TimeUnit.NANOSECONDS));
        }

        return percentiles;
    }

    /**
     * Returns {@code true} if HdrUtils and LatencyUtils are available on the class path.
     *
     * @return
     */
    public static boolean isAvailable() {
        return LATENCY_UTILS_AVAILABLE && HDR_UTILS_AVAILABLE;
    }

    private static ConcurrentHashMap<CommandLatencyId, Latencies> createNewLatencyMap() {
        return new ConcurrentHashMap<>(CommandType.values().length);
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

        public void stop() {
            firstResponse.stop();
            completion.stop();
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

    /**
     * No-operation {@link PauseDetector} implementation.
     */
    static class NoPauseDetector extends PauseDetector {

        protected static final NoPauseDetector INSTANCE = new NoPauseDetector();

        private NoPauseDetector() {
            super();
            super.shutdown();
        }

        @Override
        protected synchronized void notifyListeners(long pauseLengthNsec, long pauseEndTimeNsec) {
        }

        @Override
        public synchronized void addListener(PauseDetectorListener listener) {
        }

        @Override
        public synchronized void addListener(PauseDetectorListener listener, boolean isHighPriority) {
        }

        @Override
        public synchronized void removeListener(PauseDetectorListener listener) {
        }

        @Override
        public void shutdown() {
        }

    }

    /**
     * Wrapper for initialization of {@link PauseDetector}. Encapsulates absence of LatencyUtils.
     */
    interface PauseDetectorWrapper {

        /**
         * No-operation {@link PauseDetectorWrapper} implementation.
         */
        PauseDetectorWrapper NO_OP = new PauseDetectorWrapper() {

            @Override
            public void release() {
            }

            @Override
            public void retain() {
            }

            @Override
            public Object getPauseDetector() {
                return NoPauseDetector.INSTANCE;
            }

        };

        static PauseDetectorWrapper create() {

            if (HDR_UTILS_AVAILABLE && LATENCY_UTILS_AVAILABLE) {
                return new DefaultPauseDetectorWrapper();
            }

            return NO_OP;
        }

        static PauseDetectorWrapper noop() {
            return NO_OP;
        }

        /**
         * Retain reference to {@link PauseDetectorWrapper} and increment reference counter.
         */
        void retain();

        /**
         * Release reference to {@link PauseDetectorWrapper} and decrement reference counter.
         */
        void release();

        /**
         * Obtain the current {@link PauseDetector}. Requires a call to {@link #retain()} first. Using {@code Object} to avoid
         * static code paths to optional libraries.
         */
        Object getPauseDetector();

    }

    /**
     * Reference-counted wrapper for {@link PauseDetector} instances.
     */
    static class DefaultPauseDetectorWrapper implements PauseDetectorWrapper {

        private static final AtomicLong instanceCounter = new AtomicLong();

        private final AtomicLong counter = new AtomicLong();

        private final Lock lock = new ReentrantLock();

        private volatile PauseDetector pauseDetector;

        private volatile Thread shutdownHook;

        /**
         * Obtain the current {@link PauseDetector}. Requires a call to {@link #retain()} first.
         *
         * @return
         */
        @Override
        public PauseDetector getPauseDetector() {
            return pauseDetector;
        }

        /**
         * Creates or initializes a {@link PauseDetector} instance after incrementing the usage counter to one. Should be
         * {@link #release() released} once it is no longer in use.
         */
        @Override
        public void retain() {

            if (counter.incrementAndGet() == 1) {

                // Avoid concurrent calls to retain/release
                lock.lock();
                try {

                    if (instanceCounter.getAndIncrement() > 0) {
                        InternalLogger instance = InternalLoggerFactory.getInstance(getClass());
                        instance.info("Initialized PauseDetectorWrapper more than once.");
                    }

                    PauseDetector pauseDetector = new SimplePauseDetector(TimeUnit.MILLISECONDS.toNanos(10),
                            TimeUnit.MILLISECONDS.toNanos(10), 3);

                    shutdownHook = new Thread("ShutdownHook for SimplePauseDetector") {

                        @Override
                        public void run() {
                            pauseDetector.shutdown();
                        }

                    };

                    this.pauseDetector = pauseDetector;
                    try {
                        Runtime.getRuntime().addShutdownHook(shutdownHook);
                    } catch (IllegalStateException ignored) {
                        // Do not interfere with ongoing shutdown
                        // java.lang.IllegalStateException: Shutdown in progress
                        InternalLogger instance = InternalLoggerFactory.getInstance(getClass());
                        instance.warn(
                                "Initialized PauseDetectorWrapper during shutdown; pause detector was shutdown immediately.");
                        // The JVM invokes shutdown hooks in non-deterministic order, so an existing pauseDetector might already
                        // be shutdown anyway
                        pauseDetector.shutdown();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        /**
         * Decrements the usage counter. When reaching {@code 0}, the {@link PauseDetector} instance is released.
         */
        @Override
        public void release() {

            if (counter.decrementAndGet() == 0) {

                // Avoid concurrent calls to retain/release
                lock.lock();
                try {

                    instanceCounter.decrementAndGet();

                    pauseDetector.shutdown();
                    pauseDetector = null;

                    try {
                        Runtime.getRuntime().removeShutdownHook(shutdownHook);
                    } catch (IllegalStateException ignored) {
                        // Do not prevent shutdown
                        // java.lang.IllegalStateException: Shutdown in progress
                    }

                    shutdownHook = null;
                } finally {
                    lock.unlock();
                }
            }
        }

    }

}
