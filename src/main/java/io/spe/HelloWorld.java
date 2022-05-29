package io.spe;

public class HelloWorld {
    public static void main(String[] args) {
        Spe.init();

        Factorial function = Spe.compile("factorial", Factorial.class);

        long blackhole = 0;

        long time = System.nanoTime();
        for (int i = 0; i < 25_000; i++) {
            int result = function.factorial(10);
            //int result = factorial(10);
            blackhole += result;
        }
        System.out.println("Time: " + (System.nanoTime() - time) / 1000000.0 + "ms " + blackhole);
    }

    static int factorial(int n) {
        if (n == 0) return 1;
        else return (n * factorial(n - 1));
    }

    @FunctionalInterface
    interface Factorial {
        int factorial(int n);
    }
}
