/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.internal;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Predicate;

/**
 * {@link Deque} implementing a fast counting strategy by updating its count after applying modifications
 *
 * @author Mark Paluch
 * @since 4.4
 */
public class FastCountingDeque<E> extends ForwardingDeque<E> implements Deque<E> {

    private final static AtomicIntegerFieldUpdater<FastCountingDeque> UPDATER = AtomicIntegerFieldUpdater.newUpdater(
            FastCountingDeque.class, "count");

    // accessed through AtomicIntegerFieldUpdater.
    private volatile int count = 0;

    public FastCountingDeque(Deque<E> delegate) {
        super(delegate);
    }

    @Override
    public void addFirst(E e) {
        increment();
        super.addFirst(e);
    }

    @Override
    public void addLast(E e) {
        increment();
        super.addLast(e);
    }

    @Override
    public boolean offerFirst(E e) {
        if (super.offerFirst(e)) {
            increment();
            return true;
        }

        return false;
    }

    @Override
    public boolean offerLast(E e) {
        if (super.offerLast(e)) {
            increment();
            return true;
        }

        return false;
    }

    @Override
    public E removeFirst() {
        E element = super.removeFirst();
        if (element != null) {
            decrement();
        }
        return element;
    }

    @Override
    public E removeLast() {
        E element = super.removeLast();
        if (element != null) {
            decrement();
        }
        return element;
    }

    @Override
    public E pollFirst() {
        E element = super.pollFirst();
        if (element != null) {
            decrement();
        }
        return element;
    }

    @Override
    public E pollLast() {
        E element = super.pollLast();
        if (element != null) {
            decrement();
        }
        return element;
    }

    @Override
    public E getFirst() {
        return super.getFirst();
    }

    @Override
    public E getLast() {
        return super.getLast();
    }

    @Override
    public E peekFirst() {
        return super.peekFirst();
    }

    @Override
    public E peekLast() {
        return super.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (super.removeFirstOccurrence(o)) {
            decrement();

            return true;
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (super.removeLastOccurrence(o)) {
            decrement();

            return true;
        }
        return false;
    }

    @Override
    public boolean add(E e) {

        if (super.add(e)) {
            increment();
            return true;
        }
        return false;
    }

    @Override
    public boolean offer(E e) {

        if (super.offer(e)) {
            increment();
            return true;
        }
        return false;
    }

    @Override
    public E remove() {
        E element = super.remove();
        if (element != null) {
            decrement();
        }
        return element;
    }

    @Override
    public E poll() {
        E element = super.poll();
        if (element != null) {
            decrement();
        }
        return element;
    }

    @Override
    public E element() {
        return super.element();
    }

    @Override
    public E peek() {
        return super.peek();
    }

    @Override
    public void push(E e) {
        super.push(e);
        increment();
    }

    @Override
    public E pop() {
        E element = super.pop();
        if (element != null) {
            decrement();
        }
        return element;
    }

    @Override
    public boolean remove(Object o) {
        if (super.remove(o)) {
            decrement();
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(Object o) {
        return super.contains(o);
    }

    @Override
    public int size() {
        return UPDATER.get(this);
    }

    @Override
    public Iterator<E> iterator() {
        return new UnmodifiableIterator<>(super.iterator());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new UnmodifiableIterator<>(super.descendingIterator());
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public Object[] toArray() {
        return super.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return super.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean state = super.addAll(c);
        count = super.size();
        return state;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean state = super.removeAll(c);
        count = super.size();
        return state;
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        boolean state = super.removeIf(filter);
        count = super.size();
        return state;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean state = super.retainAll(c);
        count = super.size();
        return state;
    }

    @Override
    public void clear() {
        super.clear();
        UPDATER.set(this, 0);
    }

    protected void increment() {
        assert UPDATER.incrementAndGet(this) > 0;
    }

    protected void decrement() {
        assert UPDATER.decrementAndGet(this) >= 0;
    }

    private static class UnmodifiableIterator<E> implements Iterator<E> {

        private final Iterator<E> delegate;

        public UnmodifiableIterator(Iterator<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public E next() {
            return delegate.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
