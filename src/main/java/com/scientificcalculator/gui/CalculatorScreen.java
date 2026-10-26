package com.scientificcalculator.gui;

import com.scientificcalculator.engine.AngleMode;
import com.scientificcalculator.gui.KeyDef.Kind;
import com.scientificcalculator.model.CalculatorModel;
import com.scientificcalculator.model.History;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de la calculadora. Dibuja todo con rectángulos y texto (sin texturas),
 * y adapta el tamaño de las teclas a la resolución y a la escala de GUI.
 */
public class CalculatorScreen extends Screen {
    // Colores ARGB
    private static final int BG = 0xFF050505;
    private static final int KEY_BG = 0xFF121212;
    private static final int KEY_HOVER = 0xFF1F1F1F;
    private static final int KEY_PRESS = 0xFF3C3C3C;
    private static final int BORDER = 0xFF5C5C5C;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFA8A8A8;
    private static final int GREEN = 0xFF9BD6A4;
    private static final int GREEN_DIM = 0xFF4F7F58;
    private static final int RED = 0xFFE8473B;
    private static final int PANEL = 0xFF0C0C0C;

    private static final int PRESS_MS = 130;

    private final CalculatorModel model = new CalculatorModel();

    private final List<int[]> keyRects = new ArrayList<>(); // x, y, w, h por tecla (orden de KeyLayout)
    private final List<KeyDef> keys = new ArrayList<>();

    private int left, top, calcW, cellH, gap, displayH, keysTop;
    private int pressedIndex = -1;
    private long pressedAt = 0;

    private enum Overlay { NONE, MENU, HISTORY }
    private Overlay overlay = Overlay.NONE;
    private int[] overlayRect = new int[4];

    public CalculatorScreen() {
        super(Text.literal("Scientific Calculator"));
        for (List<KeyDef> row : KeyLayout.ROWS) keys.addAll(row);
    }

    @Override
    public boolean shouldPause() {
        return false; // no pausa el juego en mundos individuales
    }

    @Override
    protected void init() {
        computeLayout();
    }

    // ------------------------------------------------------------------ layout
    private void computeLayout() {
        int availW = Math.max(120, width - 8);
        int availH = Math.max(120, height - 8);
        gap = availH >= 300 ? 2 : 1;
        cellH = Math.max(11, (int) ((availH - 4 - 8 * gap) / 11.6f));
        displayH = (int) (cellH * 2.6f);
        int cellW = (availW - 5 * gap) / 6;
        cellW = Math.max(18, Math.min(cellW, (int) (cellH * 2.4f)));
        calcW = 6 * cellW + 5 * gap;
        int totalH = displayH + 4 + 9 * cellH + 8 * gap;
        left = (width - calcW) / 2;
        top = Math.max(2, (height - totalH) / 2);
        keysTop = top + displayH + 4;

        keyRects.clear();
        int w5 = (calcW - 4 * gap) / 5;
        for (int r = 0; r < KeyLayout.ROWS.size(); r++) {
            int cols = KeyLayout.ROWS.get(r).size();
            int w = cols == 6 ? cellW : w5;
            for (int c = 0; c < cols; c++) {
                int x = left + c * (w + gap);
                if (cols == 5) x = left + c * (w5 + gap);
                int y = keysTop + r * (cellH + gap);
                keyRects.add(new int[]{x, y, w, cellH});
            }
        }
        overlayRect = new int[]{left, top, calcW, displayH + 4 + 9 * cellH + 8 * gap};
    }

    private float textScale() {
        return Math.max(1f, Math.min(2f, cellH / 16f));
    }

    // ------------------------------------------------------------------ dibujo
    private void text(DrawContext c, String s, float x, float y, float scale, int color, int align) {
        MatrixStack m = c.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);
        int w = textRenderer.getWidth(s);
        int dx = align == 1 ? -w / 2 : (align == 2 ? -w : 0);
        c.drawText(textRenderer, s, dx, 0, color, false);
        m.pop();
    }

    private float fit(String s, float scale, int maxW) {
        int w = textRenderer.getWidth(s);
        if (w * scale <= maxW || w == 0) return scale;
        return Math.max(0.5f, maxW / (float) w);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        c.fill(0, 0, width, height, BG);
        drawDisplay(c);
        long now = System.currentTimeMillis();
        for (int i = 0; i < keys.size(); i++) {
            drawKey(c, i, mouseX, mouseY, now);
        }
        if (overlay == Overlay.MENU) drawMenu(c, mouseX, mouseY);
        if (overlay == Overlay.HISTORY) drawHistory(c, mouseX, mouseY);
    }

    private void drawDisplay(DrawContext c) {
        float s = textScale();
        // línea de estado
        StringBuilder st = new StringBuilder();
        st.append(model.angle() == AngleMode.DEG ? "DEG" : "RAD");
        if (model.hasMemory()) st.append("   M");
        if (model.shift()) st.append("   SHIFT");
        if (model.hyp()) st.append("   HYP");
        if (model.complexMode()) st.append("   CPLX");
        if (model.fraction()) st.append("   a/b");
        if (model.sci()) st.append("   SCI");
        text(c, st.toString(), left + 4, top + 2, Math.max(0.8f, s * 0.8f), GRAY, 0);

        // expresión (con cursor visible)
        float es = s;
        int maxW = calcW - 10;
        String ex = model.expression();
        int cur = model.cursor();
        int start = 0;
        while (start < cur && textRenderer.getWidth(ex.substring(start, cur)) * es > maxW * 0.85f) start++;
        String before = ex.substring(start, cur);
        int bw = (int) (textRenderer.getWidth(before) * es);
        String after = textRenderer.trimToWidth(ex.substring(cur), Math.max(0, (int) ((maxW - bw) / es)));
        float ey = top + displayH * 0.30f;
        text(c, before, left + 5, ey, es, WHITE, 0);
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            c.fill(left + 5 + bw, (int) ey - 1, left + 5 + bw + Math.max(1, (int) es), (int) (ey + 9 * es), WHITE);
        }
        text(c, after, left + 5 + bw + 2, ey, es, WHITE, 0);

        // resultado
        String err = model.error();
        String res = err != null ? err : model.result();
        float rs = err != null ? s : s * 1.7f;
        rs = fit(res, rs, calcW - 10);
        text(c, res, left + calcW - 5, top + displayH - 10 * rs - 4, rs, err != null ? RED : WHITE, 2);

        // línea inferior blanca
        c.fill(left, top + displayH, left + calcW, top + displayH + 2, WHITE);
    }

    private void drawKey(DrawContext c, int i, int mx, int my, long now) {
        KeyDef k = keys.get(i);
        int[] r = keyRects.get(i);
        boolean shift = model.shift();
        boolean hover = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3] && overlay == Overlay.NONE;
        boolean pressed = i == pressedIndex && now - pressedAt < PRESS_MS;

        int border = BORDER, textColor = WHITE;
        switch (k.kind()) {
            case SHIFT: border = shift ? GREEN : GREEN_DIM; textColor = GREEN; break;
            case SECONDARY: border = GREEN_DIM; textColor = GREEN; break;
            case DANGER: border = RED; textColor = RED; break;
            case EQUALS: border = GRAY; break;
            default: break;
        }
        int bg = pressed ? KEY_PRESS : (hover ? KEY_HOVER : KEY_BG);
        c.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
        c.drawBorder(r[0], r[1], r[2], r[3], border);

        float s = textScale();
        String main = k.label();
        String small = k.shiftLabel();
        int cx = r[0] + r[2] / 2;
        if (shift && small != null) {
            // con SHIFT activo, la función secundaria pasa a ser la principal
            String t = small;
            float ts = fit(t, s, r[2] - 4);
            text(c, t, cx, r[1] + r[3] / 2f - 4 * ts, ts, GREEN, 1);
        } else if (small != null && cellH >= 14) {
            float ss = Math.max(0.6f, s * 0.6f);
            ss = fit(small, ss, r[2] - 4);
            text(c, small, cx, r[1] + 1.5f, ss, GRAY, 1);
            float ms = fit(main, s * 0.95f, r[2] - 4);
            text(c, main, cx, r[1] + r[3] * 0.40f + (r[3] * 0.60f - 8 * ms) / 2f, ms, textColor, 1);
        } else {
            float ms = fit(main, s, r[2] - 4);
            text(c, main, cx, r[1] + r[3] / 2f - 4 * ms, ms, textColor, 1);
        }
    }

    private void drawPanelBase(DrawContext c, String title) {
        c.fill(overlayRect[0], overlayRect[1], overlayRect[0] + overlayRect[2], overlayRect[1] + overlayRect[3], PANEL);
        c.drawBorder(overlayRect[0], overlayRect[1], overlayRect[2], overlayRect[3], GREEN_DIM);
        text(c, title, overlayRect[0] + overlayRect[2] / 2f, overlayRect[1] + 4, textScale(), GREEN, 1);
    }

    private int rowH() { return Math.max(12, (int) (cellH * 1.1f)); }

    private List<String> menuItems() {
        List<String> m = new ArrayList<>();
        m.add("Ángulos: " + (model.angle() == AngleMode.DEG ? "DEG (grados)" : "RAD (radianes)"));
        m.add("Modo complejo: " + (model.complexMode() ? "SÍ" : "NO"));
        m.add("Resultado en fracción: " + (model.fraction() ? "SÍ" : "NO"));
        m.add("Notación científica: " + (model.sci() ? "SÍ" : "NO"));
        m.add("Borrar memoria");
        m.add("Borrar historial");
        m.add("Cerrar menú");
        return m;
    }

    private static final String[] MENU_ACTIONS = {"cmd:drg", "cmd:cplx", "cmd:frac", "cmd:sci", "cmd:clearmem", "cmd:clearhist", null};

    private void drawMenu(DrawContext c, int mx, int my) {
        drawPanelBase(c, "MENÚ");
        List<String> items = menuItems();
        int y = overlayRect[1] + (int) (textScale() * 12) + 6;
        for (int i = 0; i < items.size(); i++) {
            int x = overlayRect[0] + 4, w = overlayRect[2] - 8, h = rowH() - 2;
            boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
            c.fill(x, y, x + w, y + h, hover ? KEY_HOVER : KEY_BG);
            c.drawBorder(x, y, w, h, BORDER);
            float s = fit(items.get(i), Math.min(textScale(), 1.3f), w - 8);
            text(c, items.get(i), x + 4, y + (h - 8 * s) / 2f, s, WHITE, 0);
            y += rowH();
        }
    }

    private void drawHistory(DrawContext c, int mx, int my) {
        drawPanelBase(c, "HISTORIAL (clic = reutilizar resultado)");
        List<History.Entry> entries = model.history().entries();
        int y = overlayRect[1] + (int) (textScale() * 12) + 6;
        int bottom = overlayRect[1] + overlayRect[3] - rowH();
        if (entries.isEmpty()) {
            text(c, "Sin cálculos todavía", overlayRect[0] + overlayRect[2] / 2f, y + 6, 1f, GRAY, 1);
        }
        for (History.Entry e : entries) {
            if (y + rowH() > bottom) break;
            int x = overlayRect[0] + 4, w = overlayRect[2] - 8, h = rowH() - 2;
            boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
            c.fill(x, y, x + w, y + h, hover ? KEY_HOVER : KEY_BG);
            c.drawBorder(x, y, w, h, BORDER);
            String line = e.expression() + " = " + e.result();
            float s = fit(line, Math.min(textScale(), 1.3f), w - 8);
            text(c, line, x + 4, y + (h - 8 * s) / 2f, s, WHITE, 0);
            y += rowH();
        }
        int by = bottom + 1, bh = rowH() - 2, bw = (overlayRect[2] - 12) / 2;
        drawButton(c, overlayRect[0] + 4, by, bw, bh, "Borrar", RED, mx, my);
        drawButton(c, overlayRect[0] + 8 + bw, by, bw, bh, "Cerrar", GREEN, mx, my);
    }

    private void drawButton(DrawContext c, int x, int y, int w, int h, String label, int color, int mx, int my) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        c.fill(x, y, x + w, y + h, hover ? KEY_HOVER : KEY_BG);
        c.drawBorder(x, y, w, h, color);
        float s = Math.min(textScale(), 1.3f);
        text(c, label, x + w / 2f, y + (h - 8 * s) / 2f, s, color, 1);
    }

    // ------------------------------------------------------------------ ratón
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (overlay != Overlay.NONE) {
            handleOverlayClick((int) mx, (int) my);
            return true; // los clics nunca cierran la pantalla
        }
        for (int i = 0; i < keyRects.size(); i++) {
            int[] r = keyRects.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                pressKey(i);
                return true;
            }
        }
        return true;
    }

    private void pressKey(int i) {
        pressedIndex = i;
        pressedAt = System.currentTimeMillis();
        String action = keys.get(i).actionFor(model.shift());
        if ("cmd:menu".equals(action)) overlay = Overlay.MENU;
        else if ("cmd:history".equals(action)) overlay = Overlay.HISTORY;
        // MENU e HISTORY pasan por el modelo solo para limpiar SHIFT (el modelo ignora comandos desconocidos)
        model.press(action);
    }

    private void handleOverlayClick(int mx, int my) {
        if (mx < overlayRect[0] || mx >= overlayRect[0] + overlayRect[2] || my < overlayRect[1] || my >= overlayRect[1] + overlayRect[3]) {
            overlay = Overlay.NONE;
            return;
        }
        int y0 = overlayRect[1] + (int) (textScale() * 12) + 6;
        if (overlay == Overlay.MENU) {
            int idx = (my - y0) / rowH();
            if (my >= y0 && idx >= 0 && idx < MENU_ACTIONS.length) {
                String a = MENU_ACTIONS[idx];
                if (a == null) overlay = Overlay.NONE;
                else model.press(a);
            }
        } else {
            int bottom = overlayRect[1] + overlayRect[3] - rowH();
            if (my >= bottom) {
                int bw = (overlayRect[2] - 12) / 2;
                if (mx < overlayRect[0] + 4 + bw + 2) model.press("cmd:clearhist");
                else overlay = Overlay.NONE;
                return;
            }
            int idx = (my - y0) / rowH();
            List<History.Entry> entries = model.history().entries();
            if (my >= y0 && idx >= 0 && idx < entries.size()) {
                model.reuse(entries.get(idx).result());
                overlay = Overlay.NONE;
            }
        }
    }

    // ------------------------------------------------------------------ teclado
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && overlay != Overlay.NONE) {
            overlay = Overlay.NONE; // ESC cierra primero el panel; otro ESC cierra la calculadora
            return true;
        }
        if (overlay == Overlay.NONE) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ENTER:
                case GLFW.GLFW_KEY_KP_ENTER:
                    model.press("cmd:eval");
                    return true;
                case GLFW.GLFW_KEY_BACKSPACE:
                    model.press("cmd:back");
                    return true;
                case GLFW.GLFW_KEY_DELETE:
                    model.press("cmd:clear");
                    return true;
                case GLFW.GLFW_KEY_LEFT:
                    model.press("cmd:left");
                    return true;
                case GLFW.GLFW_KEY_RIGHT:
                    model.press("cmd:right");
                    return true;
                default:
                    break;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers); // ESC cierra la pantalla
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (overlay != Overlay.NONE) return false;
        String ins;
        if (chr == '*') ins = "×";
        else if (chr == '/') ins = "÷";
        else if (chr == '=') { model.press("cmd:eval"); return true; }
        else if ("0123456789.,;+-^()![]%".indexOf(chr) >= 0 || Character.isLetter(chr) && chr < 128) ins = String.valueOf(chr);
        else return false;
        model.press("ins:" + ins);
        return true;
    }
}
