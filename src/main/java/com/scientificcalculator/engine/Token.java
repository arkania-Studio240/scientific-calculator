package com.scientificcalculator.engine;

public final class Token {
    public enum Type { NUM, ID, OP, END }

    public final Type type;
    public final String text;
    public final double num;

    private Token(Type type, String text, double num) {
        this.type = type;
        this.text = text;
        this.num = num;
    }

    public static Token number(double v, String text) { return new Token(Type.NUM, text, v); }
    public static Token id(String s) { return new Token(Type.ID, s, 0); }
    public static Token op(String s) { return new Token(Type.OP, s, 0); }
    public static Token end() { return new Token(Type.END, "", 0); }

    public boolean isOp(String s) { return type == Type.OP && text.equals(s); }
    public boolean isId(String s) { return type == Type.ID && text.equals(s); }

    @Override
    public String toString() { return type == Type.END ? "fin de expresión" : text; }
}
