package io.spe.type;

import io.spe.Factorial;

public final class FactorialImpl implements Factorial {
    @Override
    public int factorial(int n) {
        if (n == 0) return 1;
        else return (n * factorial(n - 1));
    }
}
