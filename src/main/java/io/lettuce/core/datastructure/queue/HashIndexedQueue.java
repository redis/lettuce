/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.datastructure.queue;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.lettuce.core.internal.LettuceAssert;
import org.jetbrains.annotations.NotNull;

/**
 * A queue implementation that supports O(1) removal of elements. The queue is backed by a hash map and a doubly linked list.
 * 
 * @author chenxiaofan
 */
@SuppressWarnings("unchecked")
public class HashIndexedQueue<E> extends AbstractQueue<E> {

    private final Map<E, Object> map; // Object can be Node<E> or List<Node<E>>

    private Node<E> head;

    private Node<E> tail;

    private int size;

    private static class Node<E> {

        E value;

        Node<E> next;

        Node<E> prev;

        Node(E value) {
            this.value = value;
        }

    }

    /**
     * Create a new instance of the {@link HashIndexedQueue}.
     */
    public HashIndexedQueue() {
        map = new HashMap<>();
        size = 0;
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        final Node<E> newNode = new Node<>(e);
        if (tail == null) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }

        if (!map.containsKey(e)) {
            map.put(e, newNode);
        } else {
            Object current = map.get(e);
            if (current instanceof Node) {
                List<Node<E>> nodes = new ArrayList<>();
                nodes.add((Node<E>) current);
                nodes.add(newNode);
                map.put(e, nodes);
            } else {
                ((List<Node<E>>) current).add(newNode);
            }
        }
        size++;
        return true;
    }

    @Override
    public E poll() {
        if (head == null) {
            return null;
        }
        E value = head.value;
        removeNodeFromMap(head);
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.prev = null;
        }
        size--;
        return value;
    }

    @Override
    public E peek() {
        if (head == null) {
            return null;
        }
        return head.value;
    }

    @Override
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public class Iterator implements java.util.Iterator<E> {

        private Node<E> current;

        private Node<E> prev;

        private Iterator() {
            current = HashIndexedQueue.this.head;
            prev = null;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            E value = current.value;
            prev = current;
            current = current.next;
            return value;
        }

        @Override
        public void remove() {
            if (prev != null) {
                removeNodeFromMap(prev);
                removeNode(prev);
                size--;
                // remove once
                prev = null;
            }
        }

    }

    @NotNull
    @Override
    public Iterator iterator() {
        return new Iterator();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object e : c) {
            if (removeAllOccurrences(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        head = null;
        tail = null;
        map.clear();
        size = 0;
    }

    private boolean removeFirstOccurrence(Object element) {
        Object current = map.get(element);
        if (current == null) {
            return false;
        }
        if (current instanceof Node) {
            Node<E> node = (Node<E>) current;
            removeNode(node);
            map.remove(element);
        } else {
            List<Node<E>> nodes = (List<Node<E>>) current;
            Node<E> node = nodes.remove(0);
            if (nodes.isEmpty()) {
                map.remove(element);
            }
            removeNode(node);
        }
        size--;
        return true;
    }

    private boolean removeAllOccurrences(Object element) {
        Object current = map.get(element);
        if (current == null) {
            return false;
        }
        if (current instanceof Node) {
            final Node<E> node = (Node<E>) current;
            removeNode(node);
            size--;
        } else {
            final List<Node<E>> nodes = (List<Node<E>>) current;
            for (Node<E> node : nodes) {
                removeNode(node);
                size--;
            }
        }
        map.remove(element);
        return true;
    }

    private void removeNode(Node<E> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
    }

    private void removeNodeFromMap(Node<E> node) {
        E value = node.value;
        Object current = map.get(value);
        if (current instanceof Node) {
            LettuceAssert.assertState(current == node, "current != node");
            map.remove(value);
        } else {
            List<Node<E>> nodes = (List<Node<E>>) current;
            final boolean removed = nodes.remove(node);
            LettuceAssert.assertState(removed, "!nodes.remove(node)");
            if (nodes.isEmpty()) {
                map.remove(value);
            }
        }
    }

}
