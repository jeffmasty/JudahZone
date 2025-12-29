package net.judah.util;

import java.awt.Color;

/**
 * Preloaded rainbow color lookup for percent values 0..100.
 *
 * Usage:
 *   Color c = Rainbow.get(42);
 *
 * The cache is populated at class load time using the supplied algorithm.
 */
public final class Rainbow {

    private static final float SLOPE = 1.0f / 25.0f; // 0.04f

    private static final Color[] CACHE = new Color[101];

    static {
        for (int i = 0; i <= 100; i++) {
            CACHE[i] = chaseTheRainbow(i);
        }
    }

    private Rainbow() { /* utility */ }

    /**
     * Return the precomputed color for percent in [0..100].
     * If percent is outside the range returns Color.BLACK.
     */
    public static Color get(int percent) {
        if (percent < 0 || percent > 100) return Color.BLACK;
        return CACHE[percent];
    }

    public static Color get(float amt) {
		return chaseTheRainbow((int)(amt * 100));
	}

    /**
     * Algorithm provided by Fumi and Tsuki.
     * Produces a Color with components in range [0..1].
     */
    private static Color chaseTheRainbow(int percent) {
        if (percent < 0 || percent > 100) return Color.BLACK;
        float red = 0f;
        float green = 0f;
        float blue = 0f;

        if (percent < 25) { // green up
            blue = 1f;
            green = percent * SLOPE;
        } else if (percent < 50) { // blue down
            green = 1f;
            blue = 2f - SLOPE * percent;
        } else if (percent < 75) { // red up
            green = 1f;
            red = (percent - 50) * SLOPE;
        } else { // green down
            red = 1f;
            green = 4f - SLOPE * percent;
        }
        // clamp to [0,1] just in case of tiny FP drift
        red = clamp01(red);
        green = clamp01(green);
        blue = clamp01(blue);

        return new Color(red, green, blue);
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}