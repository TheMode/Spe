package io.spe;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class OverheadBench {
    Empty empty;

    @Setup
    public void setup() {
        this.empty = Spe.compileAndCreate(Empty.class, Impl.class);
    }

    @Benchmark
    public void call() {
        this.empty.run();
    }

    @FunctionalInterface
    public interface Empty {
        void run();
    }

    public static final class Impl implements Empty {
        @Override
        public void run() {
        }
    }
}
