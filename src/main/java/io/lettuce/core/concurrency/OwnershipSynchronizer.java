package io.lettuce.core.concurrency;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.logging.InternalLogger;

/**
 * @author chenxiaofan
 */
public class OwnershipSynchronizer<R> {

    public static final int LOOP_CHECK_PERIOD = 100_000;

    public static class FailedToPreemptOwnershipException extends Exception {

        public FailedToPreemptOwnershipException() {
            super("failed to preempt ownership");
        }

    }

    private static final AtomicReferenceFieldUpdater<OwnershipSynchronizer, Owner> OWNER = AtomicReferenceFieldUpdater
            .newUpdater(OwnershipSynchronizer.class, Owner.class, "owner");

    @SuppressWarnings("java:S3077")
    private volatile Owner owner;

    final InternalLogger logger;

    final R protectedResource;

    /**
     * Create OwnershipSynchronizer instance.
     *
     * @param protectedResource protected resource, which can only be accessed by the owner thread， e.g. mpsc queue.
     * @param initialOwnerEventExecutor initial owner thread.
     * @param initialRunningTaskNum initial running task number.
     * @param logger logger.
     */
    public OwnershipSynchronizer(R protectedResource, EventExecutor initialOwnerEventExecutor, int initialRunningTaskNum,
            InternalLogger logger) {
        this.protectedResource = protectedResource;
        this.owner = new Owner(initialOwnerEventExecutor, initialRunningTaskNum);
        this.logger = logger;
    }

    /**
     * Safely run a task in current owner thread and release its memory effect to next owner thread.
     *
     * @param task task to run
     */
    public void execute(Consumer<R> task) {
        Owner cur;
        do {
            cur = this.owner;
            if (isOwnerCurrentThreadAndPreemptPrevented(cur)) {
                // already prevented preemption, safe to skip expensive add/done calls
                task.accept(protectedResource);
                return;
            }
        } while (!OWNER.compareAndSet(this, cur, cur.toAdd(1)));

        if (cur.inEventLoop()) {
            try {
                task.accept(protectedResource);
            } finally {
                done(1);
            }
        } else {
            try {
                cur.eventExecutor.execute(() -> {
                    try {
                        task.accept(protectedResource);
                    } finally {
                        done(1);
                    }
                });
            } catch (Exception e) {
                logger.error("failed to execute task in owner thread", e);
                done(1);
                throw e;
            }
        }
    }

    /**
     * Preempt ownership only when there is no running tasks in current owner
     *
     * @param eventExecutor new thread
     * @param runningTaskNumber running task number to add
     */
    @SuppressWarnings("unused")
    public void preempt(EventExecutor eventExecutor, int runningTaskNumber) throws FailedToPreemptOwnershipException {
        preempt(eventExecutor, runningTaskNumber, Long.MAX_VALUE);
    }

    /**
     * Preempt ownership only when there is no running tasks in current owner
     *
     * @param eventExecutor new thread
     * @param runningTaskNumber running task number to add
     * @param timeout timeout
     */
    public void preempt(EventExecutor eventExecutor, int runningTaskNumber, Duration timeout)
            throws FailedToPreemptOwnershipException {
        preempt(eventExecutor, runningTaskNumber, System.nanoTime() + timeout.toNanos());
    }

    @SuppressWarnings("java:S3776" /* complexity */)
    private void preempt(EventExecutor eventExecutor, int runningTaskNumber, long deadline)
            throws FailedToPreemptOwnershipException {
        Owner newOwner = null;
        int i = 0;
        while (true) {
            final Owner cur = this.owner;

            if (cur.eventExecutor == eventExecutor) {
                if (runningTaskNumber == 0 || OWNER.compareAndSet(this, cur, cur.toAdd(runningTaskNumber))) { // prevent preempt
                    return;
                }
            } else if (cur.isDone()) {
                if (newOwner == null) {
                    newOwner = new Owner(eventExecutor, runningTaskNumber);
                }

                if (OWNER.compareAndSet(this, cur, newOwner)) {
                    logger.debug("ownership preempted by a new thread [{}]", newOwner.getThreadName());
                    // established happens-before with done()
                    return;
                }
            }

            // 1. unsafe to preempt, wait for the owner to finish
            // 2. CAS failed
            if (deadline < Long.MAX_VALUE && ++i > LOOP_CHECK_PERIOD) {
                if (System.nanoTime() > deadline) {
                    throw new FailedToPreemptOwnershipException();
                }
                i = 0;
            }
        }
    }

    /**
     * done n tasks in current owner.
     *
     * @param n number of tasks to be done.
     */
    public void done(int n) {
        Owner cur;
        do {
            cur = this.owner;
            assertIsOwnerThreadAndPreemptPrevented(cur);
        } while (!OWNER.compareAndSet(this, cur, cur.toAdd(-n)));
        // create happens-before with preempt()
    }

    public void assertIsOwnerThreadAndPreemptPrevented() {
        assertIsOwnerThreadAndPreemptPrevented(this.owner);
    }

    private void assertIsOwnerThreadAndPreemptPrevented(Owner cur) {
        LettuceAssert.assertState(isOwnerCurrentThreadAndPreemptPrevented(cur),
                () -> "[executeInOwnerWithPreemptPrevention] unexpected: "
                        + (cur.inEventLoop() ? "preemption not prevented" : "owner is not this thread"));
    }

    private boolean isOwnerCurrentThreadAndPreemptPrevented(Owner owner) {
        return owner.inEventLoop() && !owner.isDone();
    }

}
