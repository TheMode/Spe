package io.spe.basics;

import io.spe.Spe;
import io.spe.api.SpeTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpeTest
public class RawParamReturnTest {

    @Test
    public void intGet() {
        Integer function = Spe.compileAndCreate(Integer.class, Integer.Impl.class);
        assertEquals(-1, function.get(-1));
        assertEquals(0, function.get(0));
        assertEquals(1, function.get(1));
        assertEquals(2, function.get(2));
    }

    @Test
    public void longGet() {
        Long function = Spe.compileAndCreate(Long.class, Long.Impl.class);
        assertEquals(-1, function.get(-1));
        assertEquals(0, function.get(0));
        assertEquals(1, function.get(1));
        assertEquals(2, function.get(2));
    }

    @FunctionalInterface
    public interface Integer {
        int get(int n);

        class Impl implements Integer {
            @Override
            public int get(int n) {
                return n;
            }
        }
    }

    @FunctionalInterface
    public interface Long {
        long get(long n);

        class Impl implements Long {
            @Override
            public long get(long n) {
                return n;
            }
        }
    }
}
