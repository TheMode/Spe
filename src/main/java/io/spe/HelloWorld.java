package io.spe;

public class HelloWorld {
    public static void main(String[] args) {
        Factorial function = true ? Spe.compile(Factorial.class, FactorialImpl.class) : new FactorialImpl();

        long blackhole = 0;
        long time = System.nanoTime();
        for (int i = 0; i < 25_000; i++) {
            blackhole += function.factorial(10);
        }
        System.out.println("Time: " + (System.nanoTime() - time) / 1000000.0 + "ms " + blackhole);
    }

    private static final class FactorialImpl implements Factorial {
        @Override
        public int factorial(int n) {
            if (n == 0) return 1;
            else return (n * factorial(n - 1));
        }
    }
}
