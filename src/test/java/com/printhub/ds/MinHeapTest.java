package com.printhub.ds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MinHeapTest {

    private MinHeap<Integer> heapOf(int... values) {
        MinHeap<Integer> heap = new MinHeap<>();
        for (int v : values) {
            heap.add(v);
        }
        return heap;
    }

    @Test
    void pollReturnsSmallestFirst() {
        MinHeap<Integer> heap = heapOf(5, 1, 4, 2, 3);
        for (int expected = 1; expected <= 5; expected++) {
            assertEquals(expected, heap.poll());
        }
    }

    @Test
    void removeArbitraryElementKeepsHeapValid() {
        MinHeap<Integer> heap = heapOf(1, 2, 3, 4, 5, 6, 7);
        assertTrue(heap.remove(4));
        assertTrue(!heap.remove(99));
        int[] expected = {1, 2, 3, 5, 6, 7};
        for (int e : expected) {
            assertEquals(e, heap.poll());
        }
    }

    @Test
    void emptyHeapReturnsNull() {
        MinHeap<Integer> heap = new MinHeap<>();
        assertTrue(heap.isEmpty());
        assertNull(heap.peek());
        assertNull(heap.poll());
    }

    @Test
    void toSortedListLeavesHeapUnchanged() {
        MinHeap<Integer> heap = heapOf(3, 1, 2);
        assertEquals(List.of(1, 2, 3), heap.toSortedList());
        assertEquals(3, heap.size());
        assertEquals(1, heap.peek());
    }

    @Test
    void duplicatesAllowed() {
        MinHeap<Integer> heap = heapOf(2, 2, 1, 2);
        assertEquals(1, heap.poll());
        assertEquals(2, heap.poll());
        assertEquals(2, heap.poll());
        assertEquals(2, heap.poll());
        assertNull(heap.poll());
    }
}
