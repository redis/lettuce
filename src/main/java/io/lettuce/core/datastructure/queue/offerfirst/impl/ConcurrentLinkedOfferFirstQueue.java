package io.lettuce.core.datastructure.queue.offerfirst.impl;

import io.lettuce.core.datastructure.queue.offerfirst.UnboundedMpscOfferFirstQueue;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author chenxiaofan
 */
public class ConcurrentLinkedOfferFirstQueue<E> implements UnboundedMpscOfferFirstQueue<E> {

    private final ConcurrentLinkedDeque<E> delegate;

    public ConcurrentLinkedOfferFirstQueue() {
        this.delegate = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void offer(E e) {
        delegate.offer(e);
    }

    @Override
    public void offerFirstAll(@Nullable Deque<? extends E> q) {
        if (q == null) {
            return;
        }
        while (true) {
            E e = q.pollLast();
            if (e == null) {
                break;
            }
            delegate.offerFirst(e);
        }
    }

    @Override
    public E poll() {
        return delegate.poll();
    }

}
