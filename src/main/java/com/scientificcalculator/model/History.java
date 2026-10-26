package com.scientificcalculator.model;

import java.util.ArrayList;
import java.util.List;

/** Historial de cálculos (más reciente primero). */
public final class History {
    public record Entry(String expression, String result) {}

    private final List<Entry> entries = new ArrayList<>();
    private final int max;

    public History(int max) { this.max = max; }

    public void add(String expression, String result) {
        entries.add(0, new Entry(expression, result));
        while (entries.size() > max) entries.remove(entries.size() - 1);
    }

    public List<Entry> entries() { return List.copyOf(entries); }
    public void clear() { entries.clear(); }
}
