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
}
