package io.spe;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class FactorialBench {

    @Param({"false", "true"})
    public boolean nativeCode = true;

    Factorial factorial;

    @Setup
    public void setup() {
        this.factorial = nativeCode ? Spe.compile("factorial", Factorial.class, FactorialImpl.class) : new FactorialImpl();
    }

    @Benchmark
    public void factorial(Blackhole blackhole) {
        for (int i = 0; i < 10; i++) {
            blackhole.consume(1);
        }
    }

    public static final class FactorialImpl implements Factorial {
        @Override
        public int factorial(int n) {
            if (n == 0) return 1;
            else return (n * factorial(n - 1));
        }
    }
}
