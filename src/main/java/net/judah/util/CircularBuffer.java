package net.judah.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple resizable circular buffer with overwrite-on-full semantics.
 * - add(e): appends newest; if full, oldest element is overwritten.
 * - get(i): 0..size()-1 where 0 is the oldest, size()-1 is the newest.
 * - setCapacity(n): resize (keeps the most recent elements if shrinking).
 *
 * not thread safe
 */
public final class CircularBuffer<T> {
    private Object[] buf;
    private int head;   // index of oldest element
    private int size;   // number of stored elements

    public CircularBuffer(int initialCapacity) {
        if (initialCapacity <= 0) throw new IllegalArgumentException("capacity>0");
        buf = new Object[initialCapacity];
        head = 0;
        size = 0;
    }

    public int capacity() {
        return buf.length;
    }

    public  int size() {
        return size;
    }

    public  boolean isEmpty() {
        return size == 0;
    }

    // get oldest, increment read head
    public T get() {
    	T result = get(0);
    	head = head + 1 % buf.length;
    	return result;
    }

    /**
     * Add element as newest. If buffer is full, overwrite the oldest element.
     */
    public void add(T element) {
        int cap = buf.length;
        int writeIndex = (head + size) % cap;
        if (size < cap) {
            buf[writeIndex] = element;
            size++;
        } else {
            // overwrite oldest
            buf[head] = element;
            head = (head + 1) % cap;
        }
    }

    /**
     * Get element by logical index (0 = oldest, size()-1 = newest).
     * Throws IndexOutOfBoundsException for invalid indices.
     */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        int idx = (head + index) % buf.length;
        return (T) buf[idx];
    }

    /** Get the newest element, or null if empty. */
    @SuppressWarnings("unchecked")
    public T peekNewest() {
        if (size == 0) return null;
        int idx = (head + size - 1) % buf.length;
        return (T) buf[idx];
    }

    /** Get the oldest element, or null if empty. */
    @SuppressWarnings("unchecked")
    public T peekOldest() {
        if (size == 0) return null;
        return (T) buf[head];
    }

    /**
     * Resize buffer capacity. Keeps most recent elements if new capacity < current size.
     * O(n) copy on resize.
     */
    public void setCapacity(int newCapacity) {
        if (newCapacity <= 0) throw new IllegalArgumentException("capacity>0");
        if (newCapacity == buf.length) return;

        int keep = Math.min(size, newCapacity);
        Object[] newBuf = new Object[newCapacity];
        // copy last 'keep' items (so we preserve the newest data)
        int startIndex = size - keep; // logical index to begin copying from (0 = oldest)
        for (int i = 0; i < keep; i++) {
            newBuf[i] = get(startIndex + i);
        }
        buf = newBuf;
        head = 0;
        size = keep;
    }

    /** Clear the buffer. */
    public void clear() {
        head = 0;
        size = 0;
        // optional: null out array elements for GC
        Arrays.fill(buf, null);
    }

    /** Simple snapshot of contents in correct order (oldest..newest). */
    @SuppressWarnings("unchecked")
    public List<T> toList() {
        List<T> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) out.add((T) buf[(head + i) % buf.length]);
        return out;
    }
}