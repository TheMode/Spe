package io.spe;

public class HelloWorld {
    public static void main(String[] args) {
        Spe.init();
        Factorial function = Spe.compile("factorial", Factorial.class, FactorialImpl::new);

        long time = System.nanoTime();
        long blackHole = 0;
        for (int i = 0; i < 25_000; i++) {
            blackHole += function.factorial(10);
        }
        System.out.println("Time: " + (System.nanoTime() - time) / 1000000.0 + "ms " + blackHole);
    }

    @FunctionalInterface
    interface Factorial {
        int factorial(int n);
    }

    static final class FactorialImpl implements Factorial {
        @Override
        public int factorial(int n) {
            if (n == 0) return 1;
            else return (n * factorial(n - 1));
        }
    }
}
