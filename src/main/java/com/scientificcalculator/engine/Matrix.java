package com.scientificcalculator.engine;

/** Matriz real pequeña (hasta 6x6) con las operaciones básicas. */
public final class Matrix {
    public static final int MAX = 6;
    public final int rows;
    public final int cols;
    private final double[][] a;

    public Matrix(double[][] data) {
        this.rows = data.length;
        this.cols = data[0].length;
        if (rows > MAX || cols > MAX) throw new CalcException("Matriz demasiado grande (máximo 6x6)");
        this.a = new double[rows][];
        for (int i = 0; i < rows; i++) {
            if (data[i].length != cols) throw new CalcException("Las filas de la matriz deben tener igual longitud");
            this.a[i] = data[i].clone();
        }
    }

    public double get(int r, int c) {
        return a[r][c];
    }

    public Matrix add(Matrix o) {
        sameShape(o);
        double[][] r = new double[rows][cols];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) r[i][j] = a[i][j] + o.a[i][j];
        return new Matrix(r);
    }

    public Matrix sub(Matrix o) {
        sameShape(o);
        double[][] r = new double[rows][cols];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) r[i][j] = a[i][j] - o.a[i][j];
        return new Matrix(r);
    }

    public Matrix scale(double k) {
        double[][] r = new double[rows][cols];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) r[i][j] = a[i][j] * k;
        return new Matrix(r);
    }

    public Matrix mul(Matrix o) {
        if (cols != o.rows) throw new CalcException("Dimensiones incompatibles para multiplicar matrices");
        double[][] r = new double[rows][o.cols];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < o.cols; j++) {
                double s = 0;
                for (int k = 0; k < cols; k++) s += a[i][k] * o.a[k][j];
                r[i][j] = s;
            }
        return new Matrix(r);
    }

    public Matrix transpose() {
        double[][] r = new double[cols][rows];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) r[j][i] = a[i][j];
        return new Matrix(r);
    }

    public double det() {
        square();
        double[][] m = copy();
        double det = 1;
        for (int c = 0; c < rows; c++) {
            int p = c;
            for (int r = c + 1; r < rows; r++) if (Math.abs(m[r][c]) > Math.abs(m[p][c])) p = r;
            if (Math.abs(m[p][c]) < 1e-14) return 0;
            if (p != c) {
                double[] t = m[p]; m[p] = m[c]; m[c] = t;
                det = -det;
            }
            det *= m[c][c];
            for (int r = c + 1; r < rows; r++) {
                double f = m[r][c] / m[c][c];
                for (int k = c; k < rows; k++) m[r][k] -= f * m[c][k];
            }
        }
        return det;
    }

    public Matrix inverse() {
        square();
        int n = rows;
        double[][] m = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            m[i][n + i] = 1;
        }
        for (int c = 0; c < n; c++) {
            int p = c;
            for (int r = c + 1; r < n; r++) if (Math.abs(m[r][c]) > Math.abs(m[p][c])) p = r;
            if (Math.abs(m[p][c]) < 1e-12) throw new CalcException("La matriz es singular (no tiene inversa)");
            double[] t = m[p]; m[p] = m[c]; m[c] = t;
            double d = m[c][c];
            for (int k = 0; k < 2 * n; k++) m[c][k] /= d;
            for (int r = 0; r < n; r++) {
                if (r == c) continue;
                double f = m[r][c];
                if (f == 0) continue;
                for (int k = 0; k < 2 * n; k++) m[r][k] -= f * m[c][k];
            }
        }
        double[][] r = new double[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(m[i], n, r[i], 0, n);
        return new Matrix(r);
    }

    public Matrix pow(int n) {
        square();
        if (n < 0) return inverse().pow(-n);
        double[][] id = new double[rows][rows];
        for (int i = 0; i < rows; i++) id[i][i] = 1;
        Matrix result = new Matrix(id);
        Matrix base = this;
        while (n > 0) {
            if ((n & 1) == 1) result = result.mul(base);
            base = base.mul(base);
            n >>= 1;
        }
        return result;
    }

    private void sameShape(Matrix o) {
        if (rows != o.rows || cols != o.cols) throw new CalcException("Las matrices deben tener las mismas dimensiones");
    }

    private void square() {
        if (rows != cols) throw new CalcException("La matriz debe ser cuadrada");
    }

    private double[][] copy() {
        double[][] c = new double[rows][];
        for (int i = 0; i < rows; i++) c[i] = a[i].clone();
        return c;
    }

    public boolean isFinite() {
        for (double[] row : a) for (double v : row) if (!Double.isFinite(v)) return false;
        return true;
    }
}
