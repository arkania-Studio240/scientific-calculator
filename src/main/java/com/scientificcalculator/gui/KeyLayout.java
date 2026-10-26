package com.scientificcalculator.gui;

import com.scientificcalculator.gui.KeyDef.Kind;

import java.util.List;

/** Distribución de teclas: 5 filas de 6 columnas y 4 filas de 5 columnas (como la app de referencia). */
public final class KeyLayout {
    private KeyLayout() {}

    private static KeyDef k(String l, String a, String sl, String sa, Kind kind) { return new KeyDef(l, a, sl, sa, kind); }
    private static KeyDef k(String l, String a, Kind kind) { return new KeyDef(l, a, null, null, kind); }

    public static final List<List<KeyDef>> ROWS = List.of(
        List.of(
            k("SHIFT", "cmd:shift", Kind.SHIFT),
            k("MENU", "cmd:menu", Kind.FUNCTION),
            k("<", "cmd:left", Kind.FUNCTION),
            k(">", "cmd:right", Kind.FUNCTION),
            k("DEL", "cmd:back", Kind.DANGER),
            k("AC", "cmd:clear", Kind.DANGER)),
        List.of(
            k("DRG", "cmd:drg", "►DEG", "ins:deg(", Kind.SECONDARY),
            k("x⇔E", "cmd:sci", "►RAD", "ins:rad(", Kind.SECONDARY),
            k("[ ]", "ins:[¦]", "det", "ins:det(", Kind.SECONDARY),
            k("Σ", "ins:sum(", "mean", "ins:mean(", Kind.SECONDARY),
            k("∫dx", "ins:integ(", "d/dx", "ins:deriv(", Kind.SECONDARY),
            k("lim", "ins:lim(", "median", "ins:median(", Kind.SECONDARY)),
        List.of(
            k("π", "ins:π", "hyp", "cmd:hyp", Kind.FUNCTION),
            k("sin", "trig:sin", "sin⁻¹", "trig:sin", Kind.FUNCTION),
            k("cos", "trig:cos", "cos⁻¹", "trig:cos", Kind.FUNCTION),
            k("tan", "trig:tan", "tan⁻¹", "trig:tan", Kind.FUNCTION),
            k("i", "ins:i", "arg", "ins:arg(", Kind.SECONDARY),
            k("e", "ins:e", "conj", "ins:conj(", Kind.FUNCTION)),
        List.of(
            k("x⁻¹", "ins:^(-1)", "inv", "ins:inv(", Kind.FUNCTION),
            k("x²", "ins:^2", "x³", "ins:^3", Kind.FUNCTION),
            k("√", "ins:√(", "∛", "ins:cbrt(", Kind.FUNCTION),
            k("xʸ", "ins:^", "ⁿ√", "ins:root(", Kind.FUNCTION),
            k("log", "ins:log(", "10ˣ", "ins:10^", Kind.FUNCTION),
            k("ln", "ins:ln(", "eˣ", "ins:exp(", Kind.FUNCTION)),
        List.of(
            k("D°M'S", "cmd:dms", "CPLX", "cmd:cplx", Kind.SECONDARY),
            k("a/b", "cmd:frac", "σp", "ins:stdevp(", Kind.SECONDARY),
            k("x", "ins:x", Kind.SECONDARY),
            k("ANS", "ins:ans", "HIST", "cmd:history", Kind.SECONDARY),
            k("MR", "cmd:mr", "M+", "cmd:mplus", Kind.SECONDARY),
            k("x→M", "cmd:store", "M−", "cmd:mminus", Kind.SECONDARY)),
        List.of(
            k("7", "ins:7", "CPLX", "cmd:cplx", Kind.DIGIT),
            k("8", "ins:8", "|x|", "ins:abs(", Kind.DIGIT),
            k("9", "ins:9", "ran#", "ins:rand()", Kind.DIGIT),
            k("(", "ins:(", "%", "ins:%", Kind.OPERATOR),
            k(")", "ins:)", "mod", "ins: mod ", Kind.OPERATOR)),
        List.of(
            k("4", "ins:4", "n!", "ins:!", Kind.DIGIT),
            k("5", "ins:5", "nCr", "ins:ncr(", Kind.DIGIT),
            k("6", "ins:6", "nPr", "ins:npr(", Kind.DIGIT),
            k("×", "ins:×", Kind.OPERATOR),
            k("÷", "ins:÷", Kind.OPERATOR)),
        List.of(
            k("1", "ins:1", "σ", "ins:stdev(", Kind.DIGIT),
            k("2", "ins:2", "max", "ins:max(", Kind.DIGIT),
            k("3", "ins:3", "min", "ins:min(", Kind.DIGIT),
            k("+", "ins:+", Kind.OPERATOR),
            k("−", "ins:-", Kind.OPERATOR)),
        List.of(
            k("0", "ins:0", "Ans", "ins:ans", Kind.DIGIT),
            k(".", "ins:.", ",", "ins:,", Kind.DIGIT),
            k("+/−", "cmd:neg", ";", "ins:;", Kind.DIGIT),
            k("EXP", "ins:E", Kind.OPERATOR),
            k("=", "cmd:eval", Kind.EQUALS))
    );
}
