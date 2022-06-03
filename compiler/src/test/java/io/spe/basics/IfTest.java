package io.spe.basics;

import io.spe.Spe;
import io.spe.api.SpeTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpeTest
public class IfTest {
    @Test
    public void evenTest() {
        IsEven function = Spe.compileAndCreate(IsEven.class, IsEven.Impl.class);
        assertEquals(true, function.test(0));
        assertEquals(false, function.test(1));
        assertEquals(true, function.test(2));
        assertEquals(false, function.test(3));
    }

    @Test
    public void chainTest() {
        Chain function = Spe.compileAndCreate(Chain.class, Chain.Impl.class);
        assertEquals(20, function.get(0));
        assertEquals(5, function.get(1));
        assertEquals(10, function.get(2));
        assertEquals(15, function.get(3));
    }

    @FunctionalInterface
    public interface IsEven {
        boolean test(int n);

        class Impl implements IsEven {
            @Override
            public boolean test(int n) {
                return n % 2 == 0;
            }
        }
    }

    @FunctionalInterface
    public interface Chain {
        int get(int n);

        class Impl implements Chain {
            @Override
            public int get(int n) {
                if (n == 1) return 5;
                else if (n == 2) return 10;
                else if (n == 3) return 15;
                else return 20;
            }
        }
    }
}
