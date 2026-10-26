package com.scientificcalculator.engine;

/** Error matemático o de sintaxis con mensaje legible para el usuario. */
public final class CalcException extends RuntimeException {
    public CalcException(String message) {
        super(message);
    }
}
