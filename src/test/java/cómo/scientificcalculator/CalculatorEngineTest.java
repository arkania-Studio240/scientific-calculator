package com.scientificcalculator;

import com.scientificcalculator.engine.*;
import com.scientificcalculator.model.CalculatorModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorEngineTest {

    private static String ev(String s, AngleMode m) {
        EvalContext c = new EvalContext();
        c.angle = m;
        return Formatter.formatValue(Parser.evaluate(s, c), false, false);
    }

    private static String ev(String s) { return ev(s, AngleMode.DEG); }

    private static String err(String s) {
        try {
            ev(s);
        } catch (CalcException e) {
            return e.getMessage();
        }
        return "SIN ERROR";
    }

    @Test void basicArithmetic() {
        assertEquals("7", ev("1+2*3"));
        assertEquals("9", ev("(1+2)*3"));
        assertEquals("-4", ev("-2^2"));
        assertEquals("512", ev("2^3^2"));
        assertEquals("0.5", ev("2^-1"));
        assertEquals("0.3", ev("0.1+0.2"));
        assertEquals("6", ev("2(3)"));
        assertEquals("0.5", ev("50%"));
        assertEquals("1", ev("7 mod 3"));
    }

    @Test void exampleFromSpec() {
        assertEquals("5.5", ev("sin(30) + sqrt(25)"));
        assertEquals("5.5", ev("sin(30)+√(25"));
    }

    @Test void degRadSwitch() {
        assertEquals("0.5", ev("sin(30)", AngleMode.DEG));
        assertEquals("-0.9880316241", ev("sin(30)", AngleMode.RAD));
        assertEquals("1", ev("sin(π/2)", AngleMode.RAD));
        assertEquals("0", ev("cos(90)", AngleMode.DEG));
        assertEquals("30", ev("asin(0.5)", AngleMode.DEG));
        assertEquals("45", ev("atan(1)", AngleMode.DEG));
    }

    @Test void scientificFunctions() {
        assertEquals("2", ev("log(100)"));
        assertEquals("1", ev("ln(e)"));
        assertEquals("120", ev("5!"));
        assertEquals("10", ev("ncr(5,2)"));
        assertEquals("20", ev("npr(5,2)"));
        assertEquals("3", ev("root(3,27)"));
        assertEquals("-2", ev("root(3,-8)"));
        assertEquals("1000", ev("1E3"));
        assertEquals("2.718281828", ev("exp(1)"));
        assertEquals("3.141592654", ev("π"));
        assertEquals("1.175201194", ev("sinh(1)"));
        assertEquals("0.25", ev("inv(4)"));
        assertEquals("5", ev("abs(-5)"));
        assertEquals("180", ev("deg(π)"));
    }

    @Test void advanced() {
        assertEquals("55", ev("sum(x, 1, 10)"));
        assertEquals("385", ev("sum(x^2, 1, 10)"));
        assertEquals("9", ev("integ(x^2, 0, 3)"));
        assertEquals("6", ev("deriv(x^2, 3)"));
        assertEquals("2", ev("lim(sin(x)/x*2, 0)", AngleMode.RAD));
        assertEquals("3", ev("mean(1,2,3,4,5)"));
        assertEquals("3", ev("median(5,1,3)"));
        assertEquals("1.58113883", ev("stdev(1,2,3,4,5)").substring(0, 10));
        assertEquals("-2", ev("det([1,2;3,4])"));
        assertEquals("[19,22;43,50]", ev("[1,2;3,4]*[5,6;7,8]"));
        assertEquals("[1,3;2,4]", ev("tr([1,2;3,4])"));
    }

    @Test void complexNumbers() {
        assertEquals("-1", ev("i*i"));
        assertEquals("4+6i", ev("(1+2i)+(3+4i)"));
        assertEquals("-5+10i", ev("(1+2i)*(3+4i)"));
        assertEquals("5", ev("abs(3+4i)"));
        EvalContext c = new EvalContext();
        c.complexMode = true;
        assertEquals("2i", Formatter.formatValue(Parser.evaluate("sqrt(-4)", c), false, false));
    }

    @Test void errors() {
        assertEquals("División por cero", err("1/0"));
        assertTrue(err("sqrt(-4)").contains("negativo"));
        assertTrue(err("ln(0)").contains("cero"));
        assertTrue(err("asin(2)").contains("entre -1 y 1"));
        assertTrue(err("tan(90)").contains("no definida"));
        assertTrue(err("2+").contains("incompleta"));
        assertTrue(err("2 $ 3").contains("no válido"));
        assertTrue(err("foo(3)").contains("desconocida"));
        assertTrue(err("10^400").contains("Desbordamiento"));
        assertTrue(err("171!").contains("Desbordamiento"));
        assertTrue(err("(-8)^(1/3)").contains("dominio real"));
        assertTrue(err("x+1").contains("variable x"));
    }

    @Test void formatting() {
        assertEquals("1E10", Formatter.format(1e10, false));
        assertEquals("1.5E-5", Formatter.format(0.000015, false));
        assertEquals("1.23E3", Formatter.format(1230, true));
        assertEquals("0", Formatter.format(Math.sin(Math.PI), false));
        assertEquals("3/4", Fractions.toFraction(0.75));
        assertEquals("-1/3", Fractions.toFraction(-1.0 / 3));
        assertEquals("30°15'0\"", Formatter.toDms(30.25));
    }

    @Test void modelBehaviour() {
        CalculatorModel m = new CalculatorModel();
        for (String k : new String[]{"ins:2", "ins:+", "ins:3", "cmd:eval"}) m.press(k);
        assertEquals("5", m.result());
        m.press("ins:×");              // continúa desde Ans
        m.press("ins:2");
        assertEquals("ans×2", m.expression());
        m.press("cmd:eval");
        assertEquals("10", m.result());
        m.press("cmd:clear");
        m.press("trig:sin");
        assertEquals("sin(", m.expression());
        m.press("cmd:back");           // borra la función completa
        assertEquals("", m.expression());
        m.press("cmd:shift");
        assertTrue(m.shift());
        m.press("ins:1");
        assertFalse(m.shift());
        m.press("ins:/");
        m.press("ins:0");
        m.press("cmd:eval");
        assertEquals("División por cero", m.error());
        m.press("cmd:drg");
        assertEquals(AngleMode.RAD, m.angle());
    }

    @Test void memory() {
        CalculatorModel m = new CalculatorModel();
        m.press("ins:5"); m.press("cmd:store");
        m.press("cmd:clear");
        m.press("ins:2"); m.press("cmd:mplus");
        m.press("cmd:clear");
        m.press("cmd:mr");
        m.press("cmd:eval");
        assertEquals("7", m.result());
        assertTrue(m.hasMemory());
    }
}
