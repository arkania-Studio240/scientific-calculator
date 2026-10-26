package com.scientificcalculator.model;

import com.scientificcalculator.engine.Complex;

/** Memoria de la calculadora (M+, M-, MR, almacenar). */
public final class CalcMemory {
    private Complex value = Complex.ZERO;
    private boolean set = false;

    public boolean isSet() { return set; }
    public Complex recall() { return value; }
    public void store(Complex c) { value = c; set = true; }
    public void add(Complex c) { value = value.add(c); set = true; }
    public void subtract(Complex c) { value = value.sub(c); set = true; }
    public void clear() { value = Complex.ZERO; set = false; }
}
