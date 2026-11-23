package com.printhub.ds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A binary min-heap: the smallest element (by compareTo) is always at the top.
 * add and poll take O(log n); peek takes O(1). remove-by-element is O(n) because it
 * has to find the element first, which is fine for a print shop queue.
 */
public class MinHeap<T extends Comparable<T>> {

    private final List<T> items = new ArrayList<>();

    public void add(T item) {
        items.add(item);
        siftUp(items.size() - 1);
    }

    /** The smallest element, or null when empty. */
    public T peek() {
        return items.isEmpty() ? null : items.get(0);
    }

    /** Removes and returns the smallest element, or null when empty. */
    public T poll() {
        if (items.isEmpty()) {
            return null;
        }
        T top = items.get(0);
        removeAt(0);
        return top;
    }

    /** Removes one element equal to the given one. Returns false if it is not in the heap. */
    public boolean remove(T item) {
        int index = items.indexOf(item);
        if (index < 0) {
            return false;
        }
        removeAt(index);
        return true;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** All elements in heap order, smallest first. The heap itself is not changed. */
    public List<T> toSortedList() {
        List<T> copy = new ArrayList<>(items);
        Collections.sort(copy);
        return copy;
    }

    private void removeAt(int index) {
        int last = items.size() - 1;
        swap(index, last);
        items.remove(last);
        if (index < items.size()) {
            siftDown(index);
            siftUp(index);
        }
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (items.get(index).compareTo(items.get(parent)) >= 0) {
                return;
            }
            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown(int index) {
        int size = items.size();
        while (true) {
            int left = 2 * index + 1;
            int right = left + 1;
            int smallest = index;
            if (left < size && items.get(left).compareTo(items.get(smallest)) < 0) {
                smallest = left;
            }
            if (right < size && items.get(right).compareTo(items.get(smallest)) < 0) {
                smallest = right;
            }
            if (smallest == index) {
                return;
            }
            swap(index, smallest);
            index = smallest;
        }
    }

    private void swap(int a, int b) {
        T tmp = items.get(a);
        items.set(a, items.get(b));
        items.set(b, tmp);
    }
}
