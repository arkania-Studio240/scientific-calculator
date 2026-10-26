package com.scientificcalculator.gui;

/** Definición de una tecla: etiqueta, acción, y la versión SHIFT (puede ser null). */
public record KeyDef(String label, String action, String shiftLabel, String shiftAction, Kind kind) {
    public enum Kind { DIGIT, OPERATOR, FUNCTION, SECONDARY, DANGER, SHIFT, EQUALS }

    public String actionFor(boolean shift) {
        return shift && shiftAction != null ? shiftAction : action;
    }

    public String labelFor(boolean shift) {
        return shift && shiftLabel != null ? shiftLabel : label;
    }
}
