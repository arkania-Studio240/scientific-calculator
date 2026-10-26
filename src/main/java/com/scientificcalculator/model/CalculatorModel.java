package com.scientificcalculator.model;

import com.scientificcalculator.engine.*;

import java.util.regex.Pattern;

/**
 * Estado y lógica de la calculadora, independiente de Minecraft (se puede probar sin el juego).
 * Las teclas se envían como "acciones": "ins:TEXTO", "trig:sin" o "cmd:nombre".
 */
public final class CalculatorModel {
    private static final Pattern FUNC_END = Pattern.compile("(√|[a-z]+)\\($");
    public static final char CURSOR_MARK = '¦';

    public final EvalContext ctx = new EvalContext();
    private final CalcMemory memory = new CalcMemory();
    private final History history = new History(50);

    private String expr = "";
    private int cursor = 0;
    private String result = "0";
    private String error = null;
    private Object lastValue = null;
    private boolean afterEval = false;
    private boolean shift, hyp, fraction, sci;

    // ------------------------------------------------------------- lectura de estado
    public String expression() { return expr; }
    public int cursor() { return cursor; }
    public String result() { return result; }
    public String error() { return error; }
    public boolean shift() { return shift; }
    public boolean hyp() { return hyp; }
    public boolean fraction() { return fraction; }
    public boolean sci() { return sci; }
    public boolean complexMode() { return ctx.complexMode; }
    public AngleMode angle() { return ctx.angle; }
    public boolean hasMemory() { return memory.isSet(); }
    public History history() { return history; }

    // ------------------------------------------------------------- entrada de teclas
    public void press(String action) {
        if (action == null) return;
        boolean keepShift = false;
        if (action.startsWith("ins:")) {
            insert(action.substring(4));
        } else if (action.startsWith("trig:")) {
            String base = action.substring(5);
            String name = (shift ? "a" : "") + base + (hyp ? "h" : "");
            hyp = false;
            insert(name + "(");
        } else if (action.startsWith("cmd:")) {
            keepShift = command(action.substring(4));
        }
        if (!keepShift) shift = false;
    }

    /** Devuelve true si el comando debe conservar el estado de SHIFT (solo el propio SHIFT). */
    private boolean command(String c) {
        switch (c) {
            case "shift": shift = !shift; return true;
            case "hyp": hyp = !hyp; break;
            case "left": if (cursor > 0) cursor--; break;
            case "right": if (cursor < expr.length()) cursor++; break;
            case "back": backspace(); break;
            case "clear": clear(); break;
            case "eval": evaluate(); break;
            case "drg": ctx.angle = ctx.angle.toggled(); break;
            case "sci": sci = !sci; refreshResult(); break;
            case "frac": fraction = !fraction; refreshResult(); break;
            case "cplx": ctx.complexMode = !ctx.complexMode; break;
            case "dms": showDms(); break;
            case "neg": negate(); break;
            case "mr": insertMemory(); break;
            case "mplus": memoryOp(1); break;
            case "mminus": memoryOp(-1); break;
            case "store": memoryOp(0); break;
            case "clearmem": memory.clear(); break;
            case "clearhist": history.clear(); break;
            default: break; // menu / history los gestiona la interfaz
        }
        return false;
    }

    public void insert(String text) {
        int mark = text.indexOf(CURSOR_MARK);
        String clean = text.replace(String.valueOf(CURSOR_MARK), "");
        if (afterEval) {
            if (startsWithBinary(clean)) { expr = "ans"; cursor = 3; }
            else { expr = ""; cursor = 0; }
            afterEval = false;
        }
        error = null;
        if (expr.length() + clean.length() > 300) return;
        expr = expr.substring(0, cursor) + clean + expr.substring(cursor);
        cursor += mark >= 0 ? mark : clean.length();
    }

    private static boolean startsWithBinary(String s) {
        if (s.isEmpty()) return false;
        char c = s.charAt(0);
        return "+-×÷*/^!%".indexOf(c) >= 0 || s.startsWith(" mod");
    }

    private void backspace() {
        afterEval = false;
        error = null;
        if (cursor == 0) return;
        String before = expr.substring(0, cursor);
        int len = 1;
        var m = FUNC_END.matcher(before);
        if (m.find()) len = m.group().length();
        else if (before.endsWith(" mod ")) len = 5;
        expr = expr.substring(0, cursor - len) + expr.substring(cursor);
        cursor -= len;
    }

    public void clear() {
        expr = "";
        cursor = 0;
        error = null;
        result = "0";
        lastValue = null;
        afterEval = false;
    }

    private void negate() {
        if (afterEval) { afterEval = false; }
        error = null;
        if (expr.isEmpty()) { expr = "-"; cursor = 1; return; }
        if (expr.startsWith("-(") && expr.endsWith(")")) {
            expr = expr.substring(2, expr.length() - 1);
        } else {
            expr = "-(" + expr + ")";
        }
        cursor = expr.length();
    }

    // ------------------------------------------------------------- evaluación
    public void evaluate() {
        if (expr.isBlank()) return;
        try {
            Object v = Parser.evaluate(expr, ctx);
            lastValue = v;
            ctx.ans = v;
            result = Formatter.formatValue(v, fraction, sci);
            error = null;
            history.add(expr, result);
            afterEval = true;
            cursor = expr.length();
        } catch (CalcException ex) {
            error = ex.getMessage();
        }
    }

    private void refreshResult() {
        if (lastValue != null) result = Formatter.formatValue(lastValue, fraction, sci);
    }

    private void showDms() {
        try {
            Object v = lastValue != null ? lastValue : (expr.isBlank() ? null : Parser.evaluate(expr, ctx));
            if (v == null) return;
            if (!(v instanceof Complex c) || c.im != 0) throw new CalcException("D°M'S solo funciona con números reales");
            lastValue = v;
            result = Formatter.toDms(c.re);
            error = null;
        } catch (CalcException ex) {
            error = ex.getMessage();
        }
    }

    // ------------------------------------------------------------- memoria
    private Complex currentScalar() {
        Object v = !expr.isBlank() ? Parser.evaluate(expr, ctx) : (lastValue != null ? lastValue : Complex.ZERO);
        if (v instanceof Complex c) return c;
        throw new CalcException("La memoria solo guarda números, no matrices");
    }

    private void memoryOp(int kind) {
        try {
            Complex c = currentScalar();
            if (kind > 0) memory.add(c);
            else if (kind < 0) memory.subtract(c);
            else memory.store(c);
            error = null;
        } catch (CalcException ex) {
            error = ex.getMessage();
        }
    }

    private void insertMemory() {
        Complex c = memory.recall();
        String s = c.im == 0 ? Formatter.plain(c.re)
                : "(" + Formatter.plain(c.re) + (c.im < 0 ? "-" : "+") + Formatter.plain(Math.abs(c.im)) + "*i)";
        if (c.im == 0 && c.re < 0) s = "(" + s + ")";
        insert(s);
    }

    /** Inserta el resultado de una entrada del historial. */
    public void reuse(String text) { insert(text); }
}
