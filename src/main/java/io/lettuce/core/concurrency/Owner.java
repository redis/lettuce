package io.lettuce.core.concurrency;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;

/**
 * @author chenxiaofan
 */
class Owner {

    final SingleThreadEventExecutor eventExecutor;

    // if positive, no other thread can preempt the ownership.
    private final int runningTaskNum;

    public Owner(EventExecutor eventExecutor, int runningTaskNum) {
        if (runningTaskNum < 0) {
            throw new IllegalArgumentException(String.format("negative runningTaskNum: %d", runningTaskNum));
        }
        LettuceAssert.assertState(eventExecutor instanceof SingleThreadEventExecutor,
                () -> String.format("unexpected event executor, expect %s got %s", SingleThreadEventExecutor.class.getName(),
                        eventExecutor.getClass().getName()));
        this.eventExecutor = (SingleThreadEventExecutor) eventExecutor;
        this.runningTaskNum = runningTaskNum;
    }

    public boolean inEventLoop() {
        return eventExecutor.inEventLoop();
    }

    public Owner toAdd(int n) {
        return new Owner(eventExecutor, runningTaskNum + n);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isDone() {
        return runningTaskNum == 0;
    }

    public String getThreadName() {
        return eventExecutor.threadProperties().name();
    }

}
