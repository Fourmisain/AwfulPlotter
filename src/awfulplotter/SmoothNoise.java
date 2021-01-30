package awfulplotter;

import awfulplotter.AwfulPlotter.DrawInstruction;
import awfulplotter.AwfulPlotter.DrawInstructor;

import java.util.*;
import java.util.function.Consumer;

public class SmoothNoise implements MathFunction, DrawInstructor {

    public static double clamp(double x, double lower, double upper) {
        if (x < lower) return lower;
        if (x > upper) return upper;
        return x;
    }

    public static double smoothstep(double x) {
        x = clamp(x, 0.0, 1.0);
        return x * x * (3 - 2 * x);
    }

    public static double smoothstep(double x, double edge0, double edge1) {
        x = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return x * x * (3 - 2 * x);
    }

    protected final Random rng = new Random();
    protected final Map<Integer, Double> fixedRandomPoints = new HashMap<>();

    protected double nextRandom() {
        return 2 * rng.nextDouble() - 1;
    }

    @Override
    public double apply(double t) {
        if (t < 0) return 0;

        int current = (int)t;

        Double r0 = fixedRandomPoints.computeIfAbsent(current, k -> nextRandom());
        Double r1 = fixedRandomPoints.computeIfAbsent(current + 1, k -> nextRandom());

        double x = t - current; // in [0, 1]

        return r0 + smoothstep(x) * (r1 - r0);
    }

    @Override
    public void provideInstructions(Consumer<Pair<Double, DrawInstruction>> consumer, double minX, double maxX) {
        for (int x : fixedRandomPoints.keySet())
            if (minX <= x && x <= maxX)
                consumer.accept(Pair.of((double)x, DrawInstruction.FILL_CIRCLE)); // "yield" draw instruction
    }
}
