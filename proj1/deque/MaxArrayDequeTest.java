package deque;

import org.junit.Test;
import static org.junit.Assert.*;

public class MaxArrayDequeTest {

    @Test
    public void testIntConstructor() {
        IntComparator comparator = new IntComparator();
        MaxArrayDeque<Integer> maxArrayDeque = new MaxArrayDeque<>(comparator);

        assertTrue(maxArrayDeque.isEmpty());

        maxArrayDeque.addFirst(1);
        maxArrayDeque.addFirst(2);
        maxArrayDeque.addFirst(3);
        maxArrayDeque.addFirst(4);
        maxArrayDeque.addFirst(5);
        maxArrayDeque.addFirst(6);

        int actual = maxArrayDeque.max();

        assertEquals(6, actual);
    }

    @Test
    public void testIntMaxMethod() {
        MaxArrayDeque<Integer> maxArrayDeque = new MaxArrayDeque<>(new IntComparator());

        assertTrue(maxArrayDeque.isEmpty());

        maxArrayDeque.addFirst(1);
        maxArrayDeque.addFirst(2);
        maxArrayDeque.addFirst(3);
        maxArrayDeque.addFirst(4);
        maxArrayDeque.addFirst(5);
        maxArrayDeque.addFirst(6);

        int actual = maxArrayDeque.max(new IntComparator());

        assertEquals(6, actual);
    }
}
