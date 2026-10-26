package com.scientificcalculator.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

/** Convierte valores a texto: 10 cifras significativas, notación científica, fracciones y grados-minutos-segundos. */
public final class Formatter {
    private Formatter() {}

    public static String format(double v, boolean sci) {
        if (Math.abs(v) < 1e-15) return "0";
        double abs = Math.abs(v);
        if (sci || abs >= 1e10 || abs < 1e-4) {
            String s = String.format(Locale.ROOT, "%.9e", v); // 1.234567890e+10
            int e = s.indexOf('e');
            String mant = s.substring(0, e);
            if (mant.contains(".")) mant = mant.replaceAll("0+$", "").replaceAll("\\.$", "");
            return mant + "E" + Integer.parseInt(s.substring(e + 1));
        }
        BigDecimal bd = new BigDecimal(v).round(new MathContext(10)).stripTrailingZeros();
        return bd.toPlainString();
    }

    /** Número con todos los decimales útiles, para reinsertarlo en una expresión (por ejemplo MR). */
    public static String plain(double v) {
        if (Math.abs(v) < 1e-15) return "0";
        BigDecimal bd = new BigDecimal(v).round(new MathContext(15)).stripTrailingZeros();
        return bd.toPlainString();
    }

    public static String format(Complex c, boolean sci) {
        if (c.im == 0) return format(c.re, sci);
        double eps = 1e-12 * Math.max(Math.abs(c.re), Math.abs(c.im));
        double re = Math.abs(c.re) < eps ? 0 : c.re;
        double im = Math.abs(c.im) < eps ? 0 : c.im;
        String imag;
        if (Math.abs(im) == 1) imag = "i";
        else imag = format(Math.abs(im), sci) + "i";
        if (im == 0) return format(re, sci);
        if (re == 0) return (im < 0 ? "-" : "") + imag;
        return format(re, sci) + (im < 0 ? "-" : "+") + imag;
    }

    public static String format(Matrix m, boolean sci) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < m.rows; i++) {
            if (i > 0) sb.append(';');
            for (int j = 0; j < m.cols; j++) {
                if (j > 0) sb.append(',');
                sb.append(format(m.get(i, j), sci));
            }
        }
        return sb.append(']').toString();
    }

    /** Formatea cualquier valor. Si fraction es true y el escalar real es racional, devuelve "a/b". */
    public static String formatValue(Object v, boolean fraction, boolean sci) {
        if (v instanceof Matrix m) return format(m, sci);
        Complex c = (Complex) v;
        if (fraction && c.im == 0) {
            String f = Fractions.toFraction(c.re);
            if (f != null) return f;
        }
        return format(c, sci);
    }

    /** Grados decimales -> D°M'S". */
    public static String toDms(double deg) {
        boolean neg = deg < 0;
        double a = Math.abs(deg);
        long totalCs = Math.round(a * 3600 * 100); // centésimas de segundo
        long d = totalCs / (3600 * 100);
        long m = (totalCs / (60 * 100)) % 60;
        double s = (totalCs % (60 * 100)) / 100.0;
        String secs = (s == Math.rint(s)) ? String.valueOf((long) s) : String.format(Locale.ROOT, "%.2f", s);
        return (neg ? "-" : "") + d + "°" + m + "'" + secs + "\"";
    }
}
