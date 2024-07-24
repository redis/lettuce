package io.lettuce.core.datastructure.queue.offerfirst;

import javax.annotation.Nullable;
import java.util.Deque;

/**
 * @author chenxiaofan
 */
public interface UnboundedMpscOfferFirstQueue<E> {

    /**
     * add element to the tail of the queue. The method is concurrent safe.
     */
    void offer(E e);

    /**
     * add all elements to the head of the queue.
     * <p>
     * Should only be called from the single consumer thread.
     *
     * @param q a queue to add
     */
    void offerFirstAll(@Nullable Deque<? extends E> q);

    /**
     * poll the first element from the head of the queue.
     * <p>
     * Should only be called from the single consumer thread.
     *
     * @return null if the queue is empty else the first element of the queue
     */
    @Nullable
    E poll();

}
