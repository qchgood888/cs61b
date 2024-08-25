package flik;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestFlik {

    @Test
    public void testUnder100() {
        int i = 0;
        for (int j = 0; j < 100; i++, j++) {
            assertTrue(Flik.isSameNumber(i, j));
        }
    }

    @Test
    public void testUnder200() {
        int i = 101;
        for (int j = 101; j < 200; i++, j++) {
            boolean result = Flik.isSameNumber(i, j);
            assertTrue("i is " + i + " j is " + j, result);
        }
    }

    @Test
    public void TestInteger(){
        Integer a = Integer.getInteger("128");
        Integer b = Integer.getInteger("128");
        assertTrue("a is " + a + " b is " + b, Flik.isSameNumber(a, b));

    }

    @Test
    public void TestBeyond128(){
        int a = 200;
        int b = 200;
        assertTrue(Flik.isSameNumber(a, b));
    }
}
