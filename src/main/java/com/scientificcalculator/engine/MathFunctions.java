package com.scientificcalculator.engine;

/** Funciones matemáticas reales auxiliares (sin estado). */
public final class MathFunctions {
    private MathFunctions() {}

    public static double sinDeg(double x) {
        double r = x % 360;
        if (r < 0) r += 360;
        if (r % 90 == 0) {
            switch ((int) (r / 90)) {
                case 1: return 1;
                case 3: return -1;
                default: return 0;
            }
        }
        return Math.sin(Math.toRadians(x));
    }

    public static double cosDeg(double x) {
        double r = x % 360;
        if (r < 0) r += 360;
        if (r % 90 == 0) {
            switch ((int) (r / 90)) {
                case 0: return 1;
                case 2: return -1;
                default: return 0;
            }
        }
        return Math.cos(Math.toRadians(x));
    }

    public static double tanDeg(double x) {
        double r = x % 180;
        if (r < 0) r += 180;
        if (r == 0) return 0;
        if (r == 90) throw new CalcException("Tangente no definida en ese ángulo");
        return Math.tan(Math.toRadians(x));
    }

    public static double factorial(double x) {
        if (x < 0 || x != Math.rint(x)) throw new CalcException("El factorial solo existe para enteros no negativos");
        if (x > 170) throw new CalcException("Desbordamiento numérico (factorial)");
        double r = 1;
        for (int i = 2; i <= (int) x; i++) r *= i;
        return r;
    }

    public static double nCr(double n, double r) {
        checkNr(n, r);
        r = Math.min(r, n - r);
        double res = 1;
        for (int i = 1; i <= (int) r; i++) {
            res = res * (n - r + i) / i;
        }
        if (!Double.isFinite(res)) throw new CalcException("Desbordamiento numérico");
        return Math.rint(res);
    }

    public static double nPr(double n, double r) {
        checkNr(n, r);
        double res = 1;
        for (int i = 0; i < (int) r; i++) res *= (n - i);
        if (!Double.isFinite(res)) throw new CalcException("Desbordamiento numérico");
        return res;
    }

    private static void checkNr(double n, double r) {
        if (n != Math.rint(n) || r != Math.rint(r) || n < 0 || r < 0 || r > n)
            throw new CalcException("nCr/nPr requieren enteros con 0 ≤ r ≤ n");
        if (n > 1000) throw new CalcException("n demasiado grande");
    }

    public static double root(double n, double x) {
        if (n == 0) throw new CalcException("Raíz de índice cero no válida");
        if (x >= 0) return Math.pow(x, 1.0 / n);
        if (n == Math.rint(n) && Math.abs(n) % 2 == 1) return -Math.pow(-x, 1.0 / n);
        throw new CalcException("Raíz no válida en el dominio real");
    }

    public static double mean(double[] v) {
        double s = 0;
        for (double d : v) s += d;
        return s / v.length;
    }

    public static double median(double[] v) {
        double[] c = v.clone();
        java.util.Arrays.sort(c);
        int n = c.length;
        return n % 2 == 1 ? c[n / 2] : (c[n / 2 - 1] + c[n / 2]) / 2;
    }

    /** Desviación estándar. sample = true usa n-1; false usa n. */
    public static double stdev(double[] v, boolean sample) {
        int n = v.length;
        if (sample && n < 2) throw new CalcException("La desviación muestral necesita al menos 2 datos");
        double m = mean(v);
        double s = 0;
        for (double d : v) s += (d - m) * (d - m);
        return Math.sqrt(s / (sample ? n - 1 : n));
    }
}
