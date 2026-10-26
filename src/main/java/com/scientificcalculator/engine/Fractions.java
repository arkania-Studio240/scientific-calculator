package com.scientificcalculator.engine;

/** Conversión de decimales a fracciones mediante fracciones continuas. */
public final class Fractions {
    private static final long MAX_DEN = 100_000;

    private Fractions() {}

    /** Devuelve "n/d" o null si el número es entero o no se aproxima bien con denominador ≤ 100000. */
    public static String toFraction(double v) {
        if (!Double.isFinite(v) || v == Math.rint(v)) return null;
        boolean neg = v < 0;
        double x = Math.abs(v);
        if (x > 1e9) return null;
        long h1 = 1, h0 = 0, k1 = 0, k0 = 1;
        double b = x;
        for (int i = 0; i < 30; i++) {
            long a = (long) Math.floor(b);
            long h2 = a * h1 + h0;
            long k2 = a * k1 + k0;
            if (k2 > MAX_DEN) break;
            h0 = h1; h1 = h2; k0 = k1; k1 = k2;
            double f = b - a;
            if (f < 1e-12 || Math.abs(x - (double) h1 / k1) < 1e-12 * Math.max(1, x)) break;
            b = 1 / f;
        }
        if (k1 == 0 || Math.abs(x - (double) h1 / k1) > 1e-9 * Math.max(1, x)) return null;
        return (neg ? "-" : "") + h1 + "/" + k1;
    }
}
