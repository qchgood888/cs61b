package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {

    private class Node {
        private T value;
        private Node prev;
        private Node next;

        Node(T value, Node prev, Node next) {
            this.value = value;
            this.prev = prev;
            this.next = next;
        }

        public T getRecursive(int index) {
            if (index == 0) {
                return value;
            } else {
                assert next != null;
                return next.getRecursive(index - 1);
            }
        }
    }

    private Node sentinel;
    private int size;

    public LinkedListDeque() {
        sentinel = new Node(null, null, null);
        sentinel.next = sentinel;
        sentinel.prev = sentinel;
        size = 0;
    }

    @Override
    public void addFirst(T item) {
        sentinel.next = new Node(item, null, sentinel.next);
        sentinel.next.prev = sentinel;
        sentinel.next.next.prev = sentinel.next;
        size += 1;
    }

    @Override
    public void addLast(T item) {
        sentinel.prev = new Node(item, sentinel.prev, null);
        sentinel.prev.next = sentinel;
        sentinel.prev.prev.next = sentinel.prev;
        size += 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        for (int i = 0; i < size - 1; i++) {
            System.out.print(get(i) + " ");
        }
        System.out.print(get(size - 1));
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        T temp = sentinel.next.value;
        sentinel.next = sentinel.next.next;
        sentinel.next.prev = sentinel;
        size -= 1;
        return temp;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        T temp = sentinel.prev.value;
        sentinel.prev = sentinel.prev.prev;
        sentinel.prev.next = sentinel;
        size -= 1;
        return temp;
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        int i = 0;
        Node current = sentinel.next;
        while (i < index) {
            current = current.next;
            i += 1;
        }
        return current.value;
    }

    public T getRecursive(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return sentinel.next.getRecursive(index);
    }

    @Override
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
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

    private class LinkedListDequeIterator implements Iterator<T> {

        private int wizPos;

        LinkedListDequeIterator() {
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
