package com.ecc;

/**
 * Galois Field GF(2^16) arithmetic using primitive polynomial
 * x^16 + x^12 + x^3 + x + 1 (hex 0x1100B, reduction poly 0x100B).
 * Primitive element α = 2.
 */
public final class GF216 {
    private static final int FIELD_SIZE = 65536;       // 2^16
    public static final int MULTIPLICATIVE_ORDER = 65535; // 2^16 - 1
    private static final int REDUCE_POLY = 0x100B;      // lower 16 bits of primitive poly
    private static final int ALPHA = 2;                  // primitive element

    // expTable[i] = α^i mod poly, for i = 0..131071 (double-sized for wrap-around)
    private static final int[] expTable = new int[2 * MULTIPLICATIVE_ORDER];
    // logTable[v] = log_α(v), undefined for v=0 (set to -1)
    private static final int[] logTable = new int[FIELD_SIZE];

    static {
        // Initialize exp and log tables
        int val = 1;
        for (int i = 0; i < MULTIPLICATIVE_ORDER; i++) {
            expTable[i] = val;
            expTable[i + MULTIPLICATIVE_ORDER] = val; // wrap-around
            logTable[val] = i;
            // Multiply by α (2) in GF(2^16)
            val = (val << 1) ^ ((val & 0x8000) != 0 ? REDUCE_POLY : 0);
            val &= 0xFFFF; // keep 16 bits
        }
        logTable[0] = -1; // log(0) is undefined
    }

    private GF216() {} // utility class

    /** Addition in GF(2^16) is XOR. */
    public static int add(int a, int b) {
        return (a ^ b) & 0xFFFF;
    }

    /** Subtraction is same as addition in GF(2^16). */
    public static int subtract(int a, int b) {
        return add(a, b);
    }

    /** Multiplication using log/exp tables. */
    public static int multiply(int a, int b) {
        if (a == 0 || b == 0) return 0;
        int logA = logTable[a];
        int logB = logTable[b];
        return expTable[logA + logB] & 0xFFFF;
    }

    /** Division: a / b in GF(2^16). Throws if b == 0. */
    public static int divide(int a, int b) {
        if (b == 0) throw new ArithmeticException("Division by zero in GF(2^16)");
        if (a == 0) return 0;
        int logA = logTable[a];
        int logB = logTable[b];
        return expTable[(logA - logB + MULTIPLICATIVE_ORDER) % MULTIPLICATIVE_ORDER] & 0xFFFF;
    }

    /** Multiplicative inverse: a^(-1) = a^(65534). */
    public static int inverse(int a) {
        if (a == 0) throw new ArithmeticException("Inverse of zero in GF(2^16)");
        return expTable[MULTIPLICATIVE_ORDER - logTable[a]] & 0xFFFF;
    }

    /** Evaluate polynomial at point x using Horner's method. */
    public static int evaluatePolynomial(int[] coefficients, int x) {
        int result = 0;
        for (int i = coefficients.length - 1; i >= 0; i--) {
            result = add(multiply(result, x), coefficients[i] & 0xFFFF);
        }
        return result & 0xFFFF;
    }

    /** Get primitive element α = 2. */
    public static int getAlpha() {
        return ALPHA;
    }

    /** Get α^i for given exponent i (mod 65535). */
    public static int exp(int i) {
        return expTable[i % MULTIPLICATIVE_ORDER] & 0xFFFF;
    }

    /** Get log_α(v). Throws if v == 0. */
    public static int log(int v) {
        if (v == 0) throw new ArithmeticException("log(0) undefined");
        return logTable[v];
    }
}
