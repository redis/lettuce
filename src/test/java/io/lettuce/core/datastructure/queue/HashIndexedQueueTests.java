package io.lettuce.core.datastructure.queue;

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;

import io.lettuce.test.LettuceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ConstantValue")
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HashIndexedQueueTests {

    private HashIndexedQueue<Integer> queue;

    @BeforeEach
    void setUp() {
        queue = new HashIndexedQueue<>();
    }

    @Test
    void testInitialization() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void testAddAndOffer() {
        assertTrue(queue.add(1));
        assertTrue(queue.offer(2));
        assertEquals(2, queue.size());
        assertTrue(queue.contains(1));
        assertTrue(queue.contains(2));
    }

    @Test
    void testRemoveAndPoll() {
        queue.add(1);
        queue.add(2);
        assertEquals(1, queue.remove());
        assertEquals(1, queue.size());
        assertEquals(2, queue.poll());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testPeekAndElement() {
        queue.add(1);
        queue.add(2);
        assertEquals(1, queue.peek());
        assertEquals(1, queue.element());
        queue.remove();
        assertEquals(2, queue.peek());
        assertEquals(2, queue.element());
    }

    @Test
    void testContains() {
        queue.add(1);
        assertTrue(queue.contains(1));
        assertFalse(queue.contains(2));
    }

    @Test
    void testSizeAndIsEmpty() {
        assertTrue(queue.isEmpty());
        queue.add(1);
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
    }

    @Test
    void testClear() {
        queue.add(1);
        queue.add(2);
        queue.clear();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void testRemove() {
        queue.add(1);
        queue.add(1);
        queue.add(2);
        queue.add(1);
        assertTrue(queue.remove(1));
        assertEquals(3, queue.size());
        assertTrue(queue.contains(1));
        assertTrue(queue.contains(2));
        assertTrue(queue.remove(1));
        assertEquals(2, queue.size());
        assertTrue(queue.remove(1));
        assertEquals(1, queue.size());
        assertTrue(queue.contains(2));
    }

    @Test
    void testRemoveAll() {
        queue.add(1);
        queue.add(1);
        queue.add(2);
        queue.add(1);
        assertTrue(queue.removeAll(Collections.singletonList(1)));
        assertEquals(1, queue.size());
        assertTrue(queue.contains(2));
    }

    @Test
    void testRemoveAll2() {
        queue.add(1);
        queue.add(1);
        queue.add(1);
        queue.add(1);
        assertTrue(queue.removeAll(Collections.singletonList(1)));
        assertEquals(0, queue.size());
    }

    @Test
    void testRemoveAllAndRetainAll() {
        queue.addAll(Arrays.asList(1, 2, 3, 4));
        queue.removeAll(Arrays.asList(1, 2));
        assertEquals(2, queue.size());
        assertFalse(queue.contains(1));
        assertFalse(queue.contains(2));
        queue.retainAll(Collections.singletonList(3));
        assertEquals(1, queue.size());
        assertTrue(queue.contains(3));
        assertFalse(queue.contains(4));
    }

    @Test
    void testRetainAll() {
        queue.addAll(Arrays.asList(1, 2, 3, 4, 1, 2, 3, 4));
        queue.retainAll(Arrays.asList(1, 2));
        assertEquals(4, queue.size());
        HashIndexedQueue<Integer>.Iterator iterator = queue.iterator();
        iterator.remove();
        assertEquals(1, iterator.next());
        assertEquals(2, iterator.next());
        assertEquals(1, iterator.next());
        assertEquals(2, iterator.next());
        iterator.remove();
        // no effect
        iterator.remove();
        iterator.remove();

        iterator = queue.iterator();
        assertEquals(1, iterator.next());
        assertEquals(2, iterator.next());
        assertEquals(1, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void testRemoveNonExistentElement() {
        assertThrows(NoSuchElementException.class, () -> queue.remove());
    }

    @Test
    void testPollEmptyQueue() {
        assertNull(queue.poll());
    }

    @Test
    void testElementEmptyQueue() {
        assertThrows(NoSuchElementException.class, () -> queue.element());
    }

    @Test
    void testPeekEmptyQueue() {
        assertNull(queue.peek());
    }

}
