package io.spe;

import io.spe.api.SpeTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpeTest
public class FactorialTest {

    @Test
    public void test() {
        Factorial function = Spe.compileAndCreate(Factorial.class, FactorialImpl.class);
        assertEquals(1, function.factorial(1));
        assertEquals(2, function.factorial(2));
        assertEquals(6, function.factorial(3));
        assertEquals(24, function.factorial(4));
        assertEquals(120, function.factorial(5));
        assertEquals(720, function.factorial(6));
        assertEquals(5040, function.factorial(7));
        assertEquals(40320, function.factorial(8));
        assertEquals(362880, function.factorial(9));
        assertEquals(3628800, function.factorial(10));
        assertEquals(39916800, function.factorial(11));
    }

    @FunctionalInterface
    public interface Factorial {
        int factorial(int n);
    }

    private static final class FactorialImpl implements Factorial {
        @Override
        public int factorial(int n) {
            if (n == 0) return 1;
            else return (n * factorial(n - 1));
        }
    }
}
