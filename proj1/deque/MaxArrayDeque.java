package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {

    private Comparator<T> comparator;

    public MaxArrayDeque(Comparator<T> c) {
        super();
        comparator = c;
    }

    public T max() {
        if (isEmpty()) {
            return null;
        }
        T max = get(0);
        for (T x : this) {
            if (comparator.compare(x, max) > 0) {
                max = x;
            }
        }
        return max;
    }

    public T max(Comparator<T> c) {
        if (isEmpty()) {
            return null;
        }
        T max = get(0);
        for (T x : this) {
            if (c.compare(x, max) > 0) {
                max = x;
            }
        }
        return max;
    }
}
