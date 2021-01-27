package awfulplotter;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SmoothNoise implements MathFunction {

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

    public final double VAR = 2;
    public final double OFF = -1;

    public double scale(double y) {
        return VAR * y + OFF;
    }

    public double apply(double t) {
        if (t < 0) return 0;

        int current = (int)t;

        Double r0 = fixedRandomPoints.computeIfAbsent(current, k -> rng.nextDouble());
        Double r1 = fixedRandomPoints.computeIfAbsent(current + 1, k -> rng.nextDouble());

        double x = t - current; // in [0, 1]

        return scale(r0 + smoothstep(x) * (r1 - r0));
    }
}
