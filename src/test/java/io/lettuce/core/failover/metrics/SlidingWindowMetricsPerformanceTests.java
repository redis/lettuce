package io.lettuce.core.failover.metrics;

import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Performance tests for lock-free sliding window metrics.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@Tag("performance")
@DisplayName("Sliding Window Metrics Performance")
class SlidingWindowMetricsPerformanceTests {

    @Test
    @DisplayName("should record 1M events with minimal overhead")
    void shouldRecord1MEventsWithMinimalOverhead() {
        SlidingWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics();
        int eventCount = 1_000_000;

        long startTime = System.nanoTime();

        for (int i = 0; i < eventCount; i++) {
            if (i % 2 == 0) {
                metrics.recordSuccess();
            } else {
                metrics.recordFailure();
            }
        }

        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        long durationMs = durationNs / 1_000_000;
        double opsPerSec = (eventCount * 1_000_000_000.0) / durationNs;

        System.out.println("Recorded " + eventCount + " events in " + durationMs + "ms");
        System.out.println("Throughput: " + String.format("%.2f", opsPerSec) + " ops/sec");
        System.out.println("Average latency: " + String.format("%.2f", durationNs / (double) eventCount) + " ns/op");
    }

    @Test
    @DisplayName("should query metrics with minimal overhead")
    void shouldQueryMetricsWithMinimalOverhead() {
        SlidingWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics();

        // Record some events
        for (int i = 0; i < 10_000; i++) {
            metrics.recordSuccess();
        }

        int queryCount = 1_000_000;
        long startTime = System.nanoTime();

        for (int i = 0; i < queryCount; i++) {
            metrics.getSnapshot();
        }

        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        long durationMs = durationNs / 1_000_000;
        double opsPerSec = (queryCount * 1_000_000_000.0) / durationNs;

        System.out.println("Queried metrics " + queryCount + " times in " + durationMs + "ms");
        System.out.println("Throughput: " + String.format("%.2f", opsPerSec) + " ops/sec");
        System.out.println("Average latency: " + String.format("%.2f", durationNs / (double) queryCount) + " ns/op");
    }

    @Test
    @DisplayName("should handle concurrent recording and querying")
    void shouldHandleConcurrentRecordingAndQuerying() throws InterruptedException {
        SlidingWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics();
        int threadCount = 8;
        int operationsPerThread = 100_000;

        Thread[] threads = new Thread[threadCount];
        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    if (j % 3 == 0) {
                        metrics.recordSuccess();
                    } else if (j % 3 == 1) {
                        metrics.recordFailure();
                    } else {
                        metrics.getSnapshot();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        long durationMs = durationNs / 1_000_000;
        int totalOps = threadCount * operationsPerThread;
        double opsPerSec = (totalOps * 1_000_000_000.0) / durationNs;

        System.out.println("Concurrent operations: " + totalOps + " in " + durationMs + "ms");
        System.out.println("Throughput: " + String.format("%.2f", opsPerSec) + " ops/sec");
        System.out.println("Average latency: " + String.format("%.2f", durationNs / (double) totalOps) + " ns/op");
    }

    @Test
    @DisplayName("should measure memory overhead")
    void shouldMeasureMemoryOverhead() {
        Runtime runtime = Runtime.getRuntime();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        SlidingWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics();

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;

        System.out.println("Memory overhead per metrics instance: " + memoryUsed + " bytes");
        System.out.println("Expected: ~2.4KB (60 buckets × 40 bytes)");
    }

    @Test
    @DisplayName("should measure GC impact")
    void shouldMeasureGCImpact() {
        System.gc();
        long gcCountBefore = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> b.getCollectionCount())
                .sum();

        SlidingWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics();

        // Record many events
        for (int i = 0; i < 10_000_000; i++) {
            if (i % 2 == 0) {
                metrics.recordSuccess();
            } else {
                metrics.recordFailure();
            }
        }

        System.gc();
        long gcCountAfter = ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> b.getCollectionCount())
                .sum();

        long gcCollections = gcCountAfter - gcCountBefore;
        System.out.println("GC collections during 10M operations: " + gcCollections);
        System.out.println("Expected: Minimal (lock-free, no allocations)");
    }

}
