package io.lettuce.core.datastructure.queue.offerfirst.impl;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.annotation.Nullable;

import io.lettuce.core.datastructure.queue.offerfirst.UnboundedOfferFirstQueue;

/**
 * @author chenxiaofan
 */
public class ConcurrentLinkedOfferFirstQueue<E> implements UnboundedOfferFirstQueue<E> {

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

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

}
