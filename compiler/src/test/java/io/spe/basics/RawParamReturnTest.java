package io.spe.basics;

import io.spe.Spe;
import io.spe.api.SpeTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpeTest
public class RawParamReturnTest {

    @Test
    public void voidRun() {
        Void function = Spe.compileAndCreate(Void.class, Void.Impl.class);
        assertDoesNotThrow(function::empty);
    }

    @Test
    public void booleanGet() {
        Boolean function = Spe.compileAndCreate(Boolean.class, Boolean.Impl.class);
        assertEquals(true, function.get(true));
        assertEquals(false, function.get(false));
    }

    @Test
    public void byteGet() {
        Byte function = Spe.compileAndCreate(Byte.class, Byte.Impl.class);
        assertEquals(java.lang.Byte.MIN_VALUE, function.get(java.lang.Byte.MIN_VALUE));
        assertEquals(-1, function.get((byte) -1));
        assertEquals(0, function.get((byte) 0));
        assertEquals(1, function.get((byte) 1));
        assertEquals(java.lang.Byte.MAX_VALUE, function.get(java.lang.Byte.MAX_VALUE));
    }

    @Test
    public void charGet() {
        Char function = Spe.compileAndCreate(Char.class, Char.Impl.class);
        assertEquals(Character.MIN_VALUE, function.get(Character.MIN_VALUE));
        assertEquals(0, function.get((char) 0));
        assertEquals(1, function.get((char) 1));
        assertEquals(Character.MAX_VALUE, function.get(Character.MAX_VALUE));
    }

    @Test
    public void shortGet() {
        Short function = Spe.compileAndCreate(Short.class, Short.Impl.class);
        assertEquals(java.lang.Short.MIN_VALUE, function.get(java.lang.Short.MIN_VALUE));
        assertEquals(-1, function.get((short) -1));
        assertEquals(0, function.get((short) 0));
        assertEquals(1, function.get((short) 1));
        assertEquals(java.lang.Short.MAX_VALUE, function.get(java.lang.Short.MAX_VALUE));
    }

    @Test
    public void intGet() {
        Integer function = Spe.compileAndCreate(Integer.class, Integer.Impl.class);
        assertEquals(java.lang.Integer.MIN_VALUE, function.get(java.lang.Integer.MIN_VALUE));
        assertEquals(-1, function.get(-1));
        assertEquals(0, function.get(0));
        assertEquals(1, function.get(1));
        assertEquals(java.lang.Integer.MAX_VALUE, function.get(java.lang.Integer.MAX_VALUE));
    }

    @Test
    public void longGet() {
        Long function = Spe.compileAndCreate(Long.class, Long.Impl.class);
        assertEquals(java.lang.Long.MIN_VALUE, function.get(java.lang.Long.MIN_VALUE));
        assertEquals(-1, function.get(-1));
        assertEquals(0, function.get(0));
        assertEquals(1, function.get(1));
        assertEquals(2, function.get(2));
        assertEquals(java.lang.Long.MAX_VALUE, function.get(java.lang.Long.MAX_VALUE));
    }

    @Test
    public void floatGet() {
        Float function = Spe.compileAndCreate(Float.class, Float.Impl.class);
        assertEquals(java.lang.Float.MIN_VALUE, function.get(java.lang.Float.MIN_VALUE));
        assertEquals(-1, function.get(-1));
        assertEquals(0, function.get(0));
        assertEquals(1, function.get(1));
        assertEquals(2, function.get(2));
        assertEquals(java.lang.Float.MAX_VALUE, function.get(java.lang.Float.MAX_VALUE));
    }

    @Test
    public void doubleGet() {
        Double function = Spe.compileAndCreate(Double.class, Double.Impl.class);
        assertEquals(java.lang.Double.MIN_VALUE, function.get(java.lang.Double.MIN_VALUE));
        assertEquals(-1, function.get(-1));
        assertEquals(0, function.get(0));
        assertEquals(1, function.get(1));
        assertEquals(2, function.get(2));
        assertEquals(java.lang.Double.MAX_VALUE, function.get(java.lang.Double.MAX_VALUE));
    }

    @FunctionalInterface
    public interface Void {
        void empty();

        class Impl implements Void {
            @Override
            public void empty() {
            }
        }
    }

    @FunctionalInterface
    public interface Boolean {
        boolean get(boolean n);

        class Impl implements Boolean {
            @Override
            public boolean get(boolean n) {
                return n;
            }
        }
    }

    @FunctionalInterface
    public interface Byte {
        byte get(byte n);

        class Impl implements Byte {
            @Override
            public byte get(byte n) {
                return n;
            }
        }
    }

    @FunctionalInterface
    public interface Char {
        char get(char n);

        class Impl implements Char {
            @Override
            public char get(char n) {
                return n;
            }
        }
    }

    @FunctionalInterface
    public interface Short {
        short get(short n);

        class Impl implements Short {
            @Override
            public short get(short n) {
                return n;
            }
        }
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

    @FunctionalInterface
    public interface Float {
        float get(float n);

        class Impl implements Float {
            @Override
            public float get(float n) {
                return n;
            }
        }
    }

    @FunctionalInterface
    public interface Double {
        double get(double n);

        class Impl implements Double {
            @Override
            public double get(double n) {
                return n;
            }
        }
    }
}
