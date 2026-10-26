package com.scientificcalculator.engine;

import java.util.Random;

/** Estado que necesita el evaluador: modo de ángulos, modo complejo y último resultado (Ans). */
public final class EvalContext {
    public AngleMode angle = AngleMode.DEG;
    public boolean complexMode = false;
    public Object ans = Complex.ZERO;
    public final Random random = new Random();
}
