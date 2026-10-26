package com.scientificcalculator.engine;

/** Número complejo inmutable. Los reales son complejos con parte imaginaria 0. */
public final class Complex {
    public static final Complex ZERO = new Complex(0, 0);
    public static final Complex ONE = new Complex(1, 0);
    public static final Complex I = new Complex(0, 1);

    public final double re;
    public final double im;

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public static Complex real(double r) {
        return new Complex(r, 0);
    }

    public boolean isReal() {
        return im == 0;
    }

    public boolean isFinite() {
        return Double.isFinite(re) && Double.isFinite(im);
    }

    public Complex add(Complex o) {
        return new Complex(re + o.re, im + o.im);
    }

    public Complex sub(Complex o) {
        return new Complex(re - o.re, im - o.im);
    }

    public Complex mul(Complex o) {
        return new Complex(re * o.re - im * o.im, re * o.im + im * o.re);
    }

    public Complex div(Complex o) {
        double d = o.re * o.re + o.im * o.im;
        if (d == 0) throw new CalcException("División por cero");
        return new Complex((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d);
    }

    public Complex neg() {
        return new Complex(-re, -im);
    }

    public Complex conj() {
        return new Complex(re, -im);
    }

    public double abs() {
        return Math.hypot(re, im);
    }

    public double arg() {
        return Math.atan2(im, re);
    }

    public Complex exp() {
        double e = Math.exp(re);
        return new Complex(e * Math.cos(im), e * Math.sin(im));
    }

    public Complex log() {
        if (re == 0 && im == 0) throw new CalcException("Logaritmo de cero no definido");
        return new Complex(Math.log(abs()), arg());
    }

    public Complex sqrt() {
        double m = abs();
        double r = Math.sqrt((m + re) / 2);
        double i = Math.sqrt(Math.max(0, (m - re) / 2));
        if (im < 0) i = -i;
        return new Complex(r, i);
    }

    public Complex pow(Complex o) {
        if (re == 0 && im == 0) {
            if (o.im == 0 && o.re > 0) return ZERO;
            throw new CalcException("0 elevado a esa potencia no está definido");
        }
        return log().mul(o).exp();
    }
}
