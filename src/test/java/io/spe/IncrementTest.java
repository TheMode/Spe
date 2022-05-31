package io.spe;

import io.spe.api.SpeTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpeTest
public class IncrementTest {

    @Test
    public void intIncr() {
        IntIncrement function = Spe.compile(IntIncrement.class, IntIncrement.Impl.class);
        assertEquals(0, function.increment(-1));
        assertEquals(1, function.increment(0));
        assertEquals(2, function.increment(1));
        assertEquals(3, function.increment(2));
    }

    @FunctionalInterface
    public interface IntIncrement {
        int increment(int n);

        class Impl implements IntIncrement {
            @Override
            public int increment(int n) {
                return ++n;
            }
        }
    }
}
