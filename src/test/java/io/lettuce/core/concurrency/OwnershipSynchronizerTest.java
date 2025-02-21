package io.lettuce.core.concurrency;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OwnershipSynchronizerTest {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OwnershipSynchronizerTest.class);

    private static final int THREADS = 30;

    private static final int ITERATIONS = 10000;

    private static final int RUNS = 40;

    private static final int EXPECT_RESULT = THREADS * ITERATIONS * RUNS;

    private static final int NUM_PREEMPTS = 1000;

    @Test
    void testOwnershipSynchronizer() {
        final ClientResources clientResources = DefaultClientResources.builder().build();
        for (int i = 0; i < 10; i++) {
            test(clientResources);
        }
        clientResources.shutdown();
    }

    private void test(ClientResources clientResources) {
        final OwnershipSynchronizer<IntWrapper> ownershipSynchronizer = new OwnershipSynchronizer<>(new IntWrapper(0),
                clientResources.eventExecutorGroup().next(), 0, logger);

        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ITERATIONS; j++) {
                    ownershipSynchronizer.execute(counter -> {
                        for (int k = 0; k < RUNS; k++) {
                            counter.increment();
                        }
                    });
                }
            });
            threads[i].start();
        }

        for (int i = 0; i < NUM_PREEMPTS; i++) {
            final EventExecutor eventExecutor = clientResources.eventExecutorGroup().next();
            eventExecutor.execute(() -> {
                try {
                    ownershipSynchronizer.preempt(eventExecutor, 1);
                } catch (OwnershipSynchronizer.FailedToPreemptOwnershipException e) {
                    throw new RuntimeException(e);
                }
                eventExecutor.schedule(() -> ownershipSynchronizer.done(1), 3, java.util.concurrent.TimeUnit.MILLISECONDS);
            });
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
            final EventExecutor eventExecutor = clientResources.eventExecutorGroup().next();
            try {
                eventExecutor.submit(() -> {
                }).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        assertThat(ownershipSynchronizer.protectedResource.getValue()).isEqualTo(EXPECT_RESULT);
    }

    public static class IntWrapper {

        private int value;

        public IntWrapper(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void increment() {
            value++;
        }

    }

}
