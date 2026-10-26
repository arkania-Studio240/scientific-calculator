package com.scientificcalculator.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parser descendente recursivo + evaluador. Trabaja sobre tokens (nunca ejecuta texto como código).
 * Los valores son {@link Complex} o {@link Matrix}.
 */
public final class Parser {
    private static final int MAX_STEPS = 1_000_000;
    private static final int MAX_DEPTH = 80;
    private static final Set<String> LAZY = Set.of("sum", "integ", "deriv", "lim");

    private final List<Token> t;
    private final EvalContext ctx;
    private int pos = 0;
    private int steps = 0;
    private int depth = 0;
    private Complex xVar = null;

    private Parser(List<Token> tokens, EvalContext ctx) {
        this.t = tokens;
        this.ctx = ctx;
    }

    /** Evalúa una expresión. Lanza CalcException con un mensaje claro si hay error. */
    public static Object evaluate(String src, EvalContext ctx) {
        if (src.length() > 300) throw new CalcException("Expresión demasiado larga");
        Parser p = new Parser(Lexer.tokenize(src), ctx);
        if (p.peek().type == Token.Type.END) throw new CalcException("Expresión vacía");
        Object v = p.expr();
        if (p.peek().type != Token.Type.END) throw new CalcException("Expresión inválida cerca de \"" + p.peek() + "\"");
        if (v instanceof Complex c && !c.isFinite()) throw new CalcException("Desbordamiento numérico");
        if (v instanceof Matrix m && !m.isFinite()) throw new CalcException("Desbordamiento numérico");
        return v;
    }

    // ------------------------------------------------------------- utilidades de tokens
    private Token peek() { return t.get(pos); }
    private Token next() { Token k = t.get(pos); if (k.type != Token.Type.END) pos++; return k; }
    private boolean isOp(String s) { return peek().isOp(s); }
    private boolean consumeOp(String s) { if (isOp(s)) { pos++; return true; } return false; }

    private void expectClose(String c) {
        if (isOp(c)) { pos++; return; }
        if (peek().type == Token.Type.END) return; // cierre automático de paréntesis
        throw new CalcException("Se esperaba \"" + c + "\" cerca de \"" + peek() + "\"");
    }

    private void enter() { if (++depth > MAX_DEPTH) throw new CalcException("Expresión demasiado anidada"); }
    private void leave() { depth--; }

    // ------------------------------------------------------------- gramática
    private Object expr() {
        Object l = term();
        while (true) {
            if (isOp("+")) { next(); l = add(l, term()); }
            else if (isOp("-")) { next(); l = sub(l, term()); }
            else break;
        }
        return l;
    }

    private Object term() {
        Object l = unary();
        while (true) {
            if (isOp("*")) { next(); l = mul(l, unary()); }
            else if (isOp("/")) { next(); l = div(l, unary()); }
            else if (peek().isId("mod")) { next(); l = mod(l, unary()); }
            else if (startsImplicit()) { l = mul(l, unary()); }
            else break;
        }
        return l;
    }

    private boolean startsImplicit() {
        Token k = peek();
        if (k.type == Token.Type.NUM) return true;
        if (k.type == Token.Type.ID) return !k.text.equals("mod");
        return k.isOp("(") || k.isOp("[");
    }

    private Object unary() {
        if (isOp("-")) { next(); enter(); Object v = unary(); leave(); return neg(v); }
        if (isOp("+")) { next(); return unary(); }
        return power();
    }

    private Object power() {
        Object base = postfix();
        if (isOp("^")) {
            next();
            enter();
            Object ex = unary(); // asociatividad derecha y permite 2^-1
            leave();
            return pow(base, ex);
        }
        return base;
    }

    private Object postfix() {
        Object p = primary();
        while (true) {
            if (isOp("!")) { next(); p = Complex.real(MathFunctions.factorial(real(p))); }
            else if (isOp("%")) { next(); p = div(p, Complex.real(100)); }
            else break;
        }
        return p;
    }

    private Object primary() {
        if (++steps > MAX_STEPS) throw new CalcException("Expresión demasiado costosa");
        Token k = next();
        switch (k.type) {
            case NUM:
                return Complex.real(k.num);
            case OP:
                if (k.text.equals("(")) {
                    enter();
                    Object v = expr();
                    expectClose(")");
                    leave();
                    return v;
                }
                if (k.text.equals("[")) return matrixLiteral();
                throw new CalcException("Expresión inválida cerca de \"" + k + "\"");
            case ID:
                return identifier(k.text);
            default:
                throw new CalcException("Expresión incompleta");
        }
    }

    private Object matrixLiteral() {
        List<double[]> rows = new ArrayList<>();
        List<Double> cur = new ArrayList<>();
        enter();
        while (true) {
            cur.add(real(expr()));
            if (consumeOp(",")) continue;
            if (consumeOp(";")) { rows.add(toArr(cur)); cur = new ArrayList<>(); continue; }
            break;
        }
        rows.add(toArr(cur));
        expectClose("]");
        leave();
        return new Matrix(rows.toArray(new double[0][]));
    }

    private static double[] toArr(List<Double> l) {
        double[] a = new double[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    private Object identifier(String name) {
        switch (name) {
            case "pi": return Complex.real(Math.PI);
            case "e": return Complex.real(Math.E);
            case "i": return Complex.I;
            case "ans": return ctx.ans;
            case "x":
                if (xVar == null) throw new CalcException("La variable x solo existe dentro de sum, integ, deriv y lim");
                return xVar;
            default:
        }
        if (!isOp("(")) throw new CalcException("Función o símbolo desconocido: " + name);
        next();
        if (LAZY.contains(name)) return lazy(name);
        List<Object> args = new ArrayList<>();
        enter();
        if (isOp(")")) next();
        else {
            do { args.add(expr()); } while (consumeOp(","));
            expectClose(")");
        }
        leave();
        return call(name, args);
    }

    // ------------------------------------------------------------- funciones con expresión (sum, integ, deriv, lim)
    private int skipExpr() {
        int d = 0, i = pos;
        while (i < t.size()) {
            Token k = t.get(i);
            if (k.type == Token.Type.END) return i;
            if (k.isOp("(") || k.isOp("[")) d++;
            else if (k.isOp(")") || k.isOp("]")) { if (--d < 0) return i; }
            else if (k.isOp(",") && d == 0) return i;
            i++;
        }
        return i;
    }

    private double evalAt(int start, int end, double x) {
        int save = pos;
        Complex old = xVar;
        xVar = Complex.real(x);
        pos = start;
        Object v = expr();
        if (pos != end) throw new CalcException("Expresión inválida dentro de la función");
        xVar = old;
        pos = save;
        return real(v);
    }

    private Object lazy(String name) {
        int start = pos;
        int end = skipExpr();
        if (!t.get(end).isOp(",")) throw new CalcException("Faltan argumentos en " + name + "(expresión, ...)");
        pos = end + 1;
        List<Double> a = new ArrayList<>();
        enter();
        do { a.add(real(expr())); } while (consumeOp(","));
        expectClose(")");
        leave();
        int after = pos;
        Object result;
        switch (name) {
            case "sum": {
                if (a.size() != 2) throw new CalcException("sum(expresión, desde, hasta)");
                double lo = a.get(0), hi = a.get(1);
                if (lo != Math.rint(lo) || hi != Math.rint(hi)) throw new CalcException("Los límites de la sumatoria deben ser enteros");
                if (hi - lo > 20000) throw new CalcException("Sumatoria demasiado grande (máximo 20001 términos)");
                double s = 0;
                for (double k = lo; k <= hi; k++) s += evalAt(start, end, k);
                result = Complex.real(s);
                break;
            }
            case "integ": {
                if (a.size() != 2) throw new CalcException("integ(expresión, a, b)");
                double lo = a.get(0), hi = a.get(1);
                int n = 2000;
                double h = (hi - lo) / n;
                double s = evalAt(start, end, lo) + evalAt(start, end, hi);
                for (int k = 1; k < n; k++) s += evalAt(start, end, lo + k * h) * (k % 2 == 1 ? 4 : 2);
                result = Complex.real(s * h / 3);
                break;
            }
            case "deriv": {
                if (a.size() != 1) throw new CalcException("deriv(expresión, punto)");
                double x0 = a.get(0);
                double h = 1e-3 * Math.max(1, Math.abs(x0));
                double d = (-evalAt(start, end, x0 + 2 * h) + 8 * evalAt(start, end, x0 + h)
                        - 8 * evalAt(start, end, x0 - h) + evalAt(start, end, x0 - 2 * h)) / (12 * h);
                result = Complex.real(d);
                break;
            }
            default: { // lim
                if (a.size() != 1) throw new CalcException("lim(expresión, punto)");
                double x0 = a.get(0);
                double h = 1e-7 * Math.max(1, Math.abs(x0));
                double l = evalAt(start, end, x0 - h);
                double r = evalAt(start, end, x0 + h);
                if (Math.abs(l - r) > 1e-4 * Math.max(1, Math.max(Math.abs(l), Math.abs(r))))
                    throw new CalcException("El límite no existe (los límites laterales difieren)");
                result = Complex.real((l + r) / 2);
            }
        }
        pos = after;
        return result;
    }

    // ------------------------------------------------------------- funciones normales
    private double toRad(double x) { return ctx.angle == AngleMode.DEG ? Math.toRadians(x) : x; }
    private double fromRad(double r) { return ctx.angle == AngleMode.DEG ? Math.toDegrees(r) : r; }

    private void need(String name, List<Object> a, int n) {
        if (a.size() != n) throw new CalcException("Argumentos incorrectos para " + name + "()");
    }

    private double[] realArgs(String name, List<Object> a) {
        if (a.isEmpty()) throw new CalcException(name + "() necesita al menos un dato");
        double[] v = new double[a.size()];
        for (int i = 0; i < v.length; i++) v[i] = real(a.get(i));
        return v;
    }

    private static Complex r(double v) {
        if (!Double.isFinite(v)) throw new CalcException(Double.isNaN(v) ? "Resultado no definido" : "Desbordamiento numérico");
        return Complex.real(v);
    }

    private Object call(String name, List<Object> a) {
        // funciones que aceptan complejos o matrices
        switch (name) {
            case "abs": {
                need(name, a, 1);
                return r(scalar(a.get(0)).abs());
            }
            case "sqrt": {
                need(name, a, 1);
                Complex c = scalar(a.get(0));
                if (!c.isReal() || c.re >= 0) return c.isReal() ? r(Math.sqrt(c.re)) : c.sqrt();
                if (ctx.complexMode) return c.sqrt();
                throw new CalcException("Raíz cuadrada de un número negativo (dominio real)");
            }
            case "exp": {
                need(name, a, 1);
                Complex c = scalar(a.get(0));
                return c.isReal() ? r(Math.exp(c.re)) : c.exp();
            }
            case "ln": {
                need(name, a, 1);
                Complex c = scalar(a.get(0));
                if (c.isReal() && c.re > 0) return r(Math.log(c.re));
                if (c.isReal() && c.re == 0) throw new CalcException("Logaritmo de cero no definido");
                if (c.isReal() && !ctx.complexMode) throw new CalcException("Logaritmo de un número negativo (dominio real)");
                return c.log();
            }
            case "conj": need(name, a, 1); return scalar(a.get(0)).conj();
            case "re": need(name, a, 1); return Complex.real(scalar(a.get(0)).re);
            case "im": need(name, a, 1); return Complex.real(scalar(a.get(0)).im);
            case "arg": {
                need(name, a, 1);
                double ang = scalar(a.get(0)).arg();
                return Complex.real(fromRad(ang));
            }
            case "inv": {
                need(name, a, 1);
                if (a.get(0) instanceof Matrix m) return m.inverse();
                return div(Complex.ONE, a.get(0));
            }
            case "det": {
                need(name, a, 1);
                return r(matrix(a.get(0)).det());
            }
            case "tr": {
                need(name, a, 1);
                return matrix(a.get(0)).transpose();
            }
            default:
        }

        // el resto trabaja con reales
        switch (name) {
            case "rand":
                need(name, a, 0);
                return Complex.real(ctx.random.nextDouble());
            case "mean": return r(MathFunctions.mean(realArgs(name, a)));
            case "median": return r(MathFunctions.median(realArgs(name, a)));
            case "stdev": case "std": return r(MathFunctions.stdev(realArgs(name, a), true));
            case "stdevp": return r(MathFunctions.stdev(realArgs(name, a), false));
            case "min": { double m = Double.POSITIVE_INFINITY; for (double d : realArgs(name, a)) m = Math.min(m, d); return r(m); }
            case "max": { double m = Double.NEGATIVE_INFINITY; for (double d : realArgs(name, a)) m = Math.max(m, d); return r(m); }
            case "ncr": need(name, a, 2); return r(MathFunctions.nCr(real(a.get(0)), real(a.get(1))));
            case "npr": need(name, a, 2); return r(MathFunctions.nPr(real(a.get(0)), real(a.get(1))));
            case "root": need(name, a, 2); return r(MathFunctions.root(real(a.get(0)), real(a.get(1))));
            default:
        }

        need(name, a, 1);
        double x = real(a.get(0));
        boolean deg = ctx.angle == AngleMode.DEG;
        switch (name) {
            case "sin": return r(deg ? MathFunctions.sinDeg(x) : Math.sin(x));
            case "cos": return r(deg ? MathFunctions.cosDeg(x) : Math.cos(x));
            case "tan": return r(deg ? MathFunctions.tanDeg(x) : Math.tan(x));
            case "asin":
                if (Math.abs(x) > 1) throw new CalcException("Arcoseno: el argumento debe estar entre -1 y 1");
                return r(fromRad(Math.asin(x)));
            case "acos":
                if (Math.abs(x) > 1) throw new CalcException("Arcocoseno: el argumento debe estar entre -1 y 1");
                return r(fromRad(Math.acos(x)));
            case "atan": return r(fromRad(Math.atan(x)));
            case "sinh": return r(Math.sinh(x));
            case "cosh": return r(Math.cosh(x));
            case "tanh": return r(Math.tanh(x));
            case "asinh": return r(Math.log(x + Math.sqrt(x * x + 1)));
            case "acosh":
                if (x < 1) throw new CalcException("Arcocosh: el argumento debe ser ≥ 1");
                return r(Math.log(x + Math.sqrt(x * x - 1)));
            case "atanh":
                if (Math.abs(x) >= 1) throw new CalcException("Arctanh: el argumento debe estar entre -1 y 1");
                return r(0.5 * Math.log((1 + x) / (1 - x)));
            case "log":
                if (x <= 0) throw new CalcException("Logaritmo de un número no positivo no definido");
                return r(Math.log10(x));
            case "cbrt": return r(Math.cbrt(x));
            case "fact": return r(MathFunctions.factorial(x));
            case "floor": return r(Math.floor(x));
            case "ceil": return r(Math.ceil(x));
            case "round": return r(Math.rint(x));
            case "deg": return r(Math.toDegrees(x)); // radianes -> grados
            case "rad": return r(Math.toRadians(x)); // grados -> radianes
            default:
                throw new CalcException("Función desconocida: " + name);
        }
    }

    // ------------------------------------------------------------- operaciones sobre valores
    private static Complex scalar(Object o) {
        if (o instanceof Complex c) return c;
        throw new CalcException("Esta operación no admite matrices");
    }

    private static Matrix matrix(Object o) {
        if (o instanceof Matrix m) return m;
        throw new CalcException("Se esperaba una matriz, por ejemplo [1,2;3,4]");
    }

    private static double real(Object o) {
        if (o instanceof Complex c) {
            if (Math.abs(c.im) > 1e-12 * Math.max(1, Math.abs(c.re))) throw new CalcException("Operación no válida con números complejos");
            return c.re;
        }
        throw new CalcException("Esta operación no admite matrices");
    }

    private static Object chk(Complex c) {
        if (!c.isFinite()) throw new CalcException(Double.isNaN(c.re) || Double.isNaN(c.im) ? "Resultado no definido" : "Desbordamiento numérico");
        return c;
    }

    private static Object add(Object a, Object b) {
        if (a instanceof Complex x && b instanceof Complex y) return chk(x.add(y));
        if (a instanceof Matrix x && b instanceof Matrix y) return x.add(y);
        throw new CalcException("No se puede sumar un escalar con una matriz");
    }

    private static Object sub(Object a, Object b) {
        if (a instanceof Complex x && b instanceof Complex y) return chk(x.sub(y));
        if (a instanceof Matrix x && b instanceof Matrix y) return x.sub(y);
        throw new CalcException("No se puede restar un escalar y una matriz");
    }

    private static Object mul(Object a, Object b) {
        if (a instanceof Complex x && b instanceof Complex y) return chk(x.mul(y));
        if (a instanceof Matrix x && b instanceof Matrix y) return x.mul(y);
        if (a instanceof Matrix x) return x.scale(real(b));
        return ((Matrix) b).scale(real(a));
    }

    private static Object div(Object a, Object b) {
        if (a instanceof Complex x && b instanceof Complex y) return chk(x.div(y));
        if (a instanceof Matrix x && b instanceof Complex y) {
            double d = real(y);
            if (d == 0) throw new CalcException("División por cero");
            return x.scale(1 / d);
        }
        throw new CalcException("División no válida con matrices");
    }

    private static Object mod(Object a, Object b) {
        double x = real(a), y = real(b);
        if (y == 0) throw new CalcException("División por cero (mod)");
        return Complex.real(x - y * Math.floor(x / y));
    }

    private static Object neg(Object a) {
        if (a instanceof Complex c) return c.neg();
        return ((Matrix) a).scale(-1);
    }

    private Object pow(Object a, Object b) {
        if (a instanceof Matrix m) {
            double e = real(b);
            if (e != Math.rint(e) || Math.abs(e) > 1000) throw new CalcException("La potencia de una matriz debe ser un entero");
            return m.pow((int) e);
        }
        Complex x = scalar(a), y = scalar(b);
        if (x.isReal() && y.isReal()) {
            double p = x.re, q = y.re;
            if (p == 0 && q == 0) throw new CalcException("0^0 está indefinido");
            if (p == 0 && q < 0) throw new CalcException("División por cero");
            if (p >= 0 || q == Math.rint(q)) return r(Math.pow(p, q));
            if (!ctx.complexMode) throw new CalcException("Potencia no válida en el dominio real");
        }
        return chk(x.pow(y));
    }
}
