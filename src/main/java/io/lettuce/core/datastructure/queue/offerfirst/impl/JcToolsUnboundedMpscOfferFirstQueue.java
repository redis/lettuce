package io.lettuce.core.datastructure.queue.offerfirst.impl;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import javax.annotation.Nullable;

import io.lettuce.core.datastructure.queue.offerfirst.UnboundedOfferFirstQueue;
import io.netty.util.internal.PlatformDependent;

/**
 * @author chenxiaofan
 */
public class JcToolsUnboundedMpscOfferFirstQueue<E> implements UnboundedOfferFirstQueue<E> {

    /**
     * The queues can only be manipulated in a single thread env.
     */
    private final LinkedList<Queue<? extends E>> unsafeQueues = new LinkedList<>();

    private final Queue<E> mpscQueue = PlatformDependent.newMpscQueue();

    @Override
    public void offer(E e) {
        mpscQueue.offer(e);
    }

    /**
     * must call from consumer thread.
     *
     * @param q an queue to add
     */
    @Override
    public void offerFirstAll(@Nullable Deque<? extends E> q) {
        if (q != null && !q.isEmpty()) {
            unsafeQueues.addFirst(q);
        }
    }

    /**
     * Must call from the consumer thread.
     *
     * @return last element of the queue or null if the queue is empty
     */
    @Override
    public E poll() {
        if (!unsafeQueues.isEmpty()) {
            return pollFromUnsafeQueues();
        }
        return mpscQueue.poll();
    }

    @Override
    public boolean isEmpty() {
        return mpscQueue.isEmpty() && unsafeQueues.isEmpty();
    }

    private E pollFromUnsafeQueues() {
        Queue<? extends E> first = unsafeQueues.getFirst();
        E e = first.poll();
        if (first.isEmpty()) {
            unsafeQueues.removeFirst();
        }
        return Objects.requireNonNull(e);
    }

}
