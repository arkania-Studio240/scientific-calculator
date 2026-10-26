package com.scientificcalculator.engine;

public enum AngleMode {
    DEG, RAD;

    public AngleMode toggled() {
        return this == DEG ? RAD : DEG;
    }
}
