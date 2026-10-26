package com.scientificcalculator.engine;

import java.util.ArrayList;
import java.util.List;

/** Convierte el texto en tokens. No evalúa nada. */
public final class Lexer {
    private Lexer() {}

    private static boolean digit(char c) { return c >= '0' && c <= '9'; }
    private static boolean letter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }

    public static List<Token> tokenize(String s) {
        List<Token> out = new ArrayList<>();
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }

            if (digit(c) || (c == '.' && i + 1 < n && digit(s.charAt(i + 1)))) {
                int j = i;
                boolean dot = false;
                while (j < n && (digit(s.charAt(j)) || (s.charAt(j) == '.' && !dot))) {
                    if (s.charAt(j) == '.') dot = true;
                    j++;
                }
                if (j < n && s.charAt(j) == 'E') { // notación científica: 1E3, 2.5E-4
                    int k = j + 1;
                    if (k < n && (s.charAt(k) == '+' || s.charAt(k) == '-')) k++;
                    if (k < n && digit(s.charAt(k))) {
                        while (k < n && digit(s.charAt(k))) k++;
                        j = k;
                    }
                }
                if (j < n && s.charAt(j) == '.') throw new CalcException("Número inválido");
                String txt = s.substring(i, j);
                double v;
                try {
                    v = Double.parseDouble(txt);
                } catch (NumberFormatException e) {
                    throw new CalcException("Número inválido: " + txt);
                }
                if (!Double.isFinite(v)) throw new CalcException("Desbordamiento numérico");
                out.add(Token.number(v, txt));
                i = j;
                continue;
            }

            if (letter(c)) {
                int j = i;
                while (j < n && letter(s.charAt(j))) j++;
                out.add(Token.id(s.substring(i, j).toLowerCase()));
                i = j;
                continue;
            }

            switch (c) {
                case '×': out.add(Token.op("*")); break;
                case '÷': out.add(Token.op("/")); break;
                case '−': out.add(Token.op("-")); break;
                case 'π': out.add(Token.id("pi")); break;
                case '√': out.add(Token.id("sqrt")); break;
                case '+': case '-': case '*': case '/': case '^': case '(': case ')':
                case '[': case ']': case ',': case ';': case '!': case '%':
                    out.add(Token.op(String.valueOf(c)));
                    break;
                default:
                    throw new CalcException("Carácter no válido: " + c);
            }
            i++;
        }
        out.add(Token.end());
        return out;
    }
}
