package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Deque<T>, Iterable<T> {

    private T[] items;
    private int size;
    private int front;
    private int rear;
    private static final double USAGE_FACTOR = 0.5;

    public ArrayDeque() {
        items = (T[]) new Object[8];
        size = 0;
        front = 0;
        rear = 0;
    }

    @Override
    public void addFirst(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        if (!isEmpty()) {
            front = (front + items.length - 1) % items.length;
        }
        items[front] = item;
        size += 1;
    }

    @Override
    public void addLast(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        if (!isEmpty()) {
            rear = (rear + 1) % items.length;
        }
        items[rear] = item;
        size += 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        int i = 0;
        while (i < size - 1) {
            System.out.print(get(i) + " ");
            i += 1;
        }
        System.out.println(get(i));
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        T res = items[front];
        items[front] = null;
        if (size != 1) {
            front = (front + 1) % items.length;
        }
        size -= 1;
        if (items.length > 8 && (double) size / items.length < USAGE_FACTOR) {
            resize(items.length / 2);
        }
        return res;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        T rest = items[rear];
        items[rear] = null;
        if (size != 1) {
            rear = (rear + items.length - 1) % items.length;
        }
        size -= 1;
        if (items.length > 8 && (double) size / items.length < USAGE_FACTOR) {
            resize(items.length / 2);
        }
        return rest;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return items[(front + index) % items.length];
    }

    private void resize(int capacity) {
        T[] newItems = (T[]) new Object[capacity];
        int current = 0;
        for (int i = 0; i < size; i++) {
            newItems[current] = items[(front + i) % items.length];
            current += 1;
        }
        items = newItems;
        front = 0;
        rear = size == 0 ? 0 : current - 1;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Deque) {
            Deque<?> other = (Deque<?>) obj;
            if (other.size() != size) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (!other.get(i).equals(this.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int wizPos;

        ArrayDequeIterator() {
            wizPos = 0;
        }

        @Override
        public boolean hasNext() {
            return wizPos < size;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                return null;
            }
            T returnItem = get(wizPos);
            wizPos += 1;
            return returnItem;
        }
    }
}
