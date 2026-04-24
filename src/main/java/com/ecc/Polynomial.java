package com.ecc;

import java.util.Arrays;

/**
 * Polynomial over GF(2^16), coefficients stored lowest degree first.
 * e.g., [c0, c1, c2] represents c0 + c1*x + c2*x^2.
 */
public final class Polynomial {
    private final int[] coefficients; // lowest degree first

    public Polynomial(int... coefficients) {
        // Trim trailing zeros (highest degree terms)
        int len = coefficients.length;
        while (len > 0 && (coefficients[len - 1] & 0xFFFF) == 0) {
            len--;
        }
        this.coefficients = len < coefficients.length ? Arrays.copyOf(coefficients, len) : coefficients.clone();
    }

    public int degree() {
        return coefficients.length - 1;
    }

    public int getCoefficient(int degree) {
        if (degree < 0 || degree >= coefficients.length) return 0;
        return coefficients[degree] & 0xFFFF;
    }

    public Polynomial add(Polynomial other) {
        int maxLen = Math.max(coefficients.length, other.coefficients.length);
        int[] result = new int[maxLen];
        for (int i = 0; i < maxLen; i++) {
            int a = i < coefficients.length ? coefficients[i] : 0;
            int b = i < other.coefficients.length ? other.coefficients[i] : 0;
            result[i] = GF216.add(a, b) & 0xFFFF;
        }
        return new Polynomial(result);
    }

    public Polynomial multiply(Polynomial other) {
        int[] result = new int[coefficients.length + other.coefficients.length - 1];
        for (int i = 0; i < coefficients.length; i++) {
            for (int j = 0; j < other.coefficients.length; j++) {
                result[i + j] ^= GF216.multiply(coefficients[i], other.coefficients[j]) & 0xFFFF;
            }
        }
        return new Polynomial(result);
    }

    public int[][] divideAndRemainder(Polynomial divisor) {
        if (divisor.degree() < 0) throw new ArithmeticException("Division by zero polynomial");
        int[] rem = coefficients.clone();
        int[] quot = new int[Math.max(0, coefficients.length - divisor.coefficients.length + 1)];
        int dLead = divisor.getCoefficient(divisor.degree());
        int dLeadInv = GF216.inverse(dLead);
        for (int i = coefficients.length - 1; i >= divisor.degree(); i--) {
            if (rem[i] == 0) continue;
            int factor = GF216.multiply(rem[i], dLeadInv) & 0xFFFF;
            quot[i - divisor.degree()] = factor;
            for (int j = 0; j <= divisor.degree(); j++) {
                rem[i - j] ^= GF216.multiply(factor, divisor.getCoefficient(divisor.degree() - j)) & 0xFFFF;
            }
        }
        // Trim quotient
        int quotLen = quot.length;
        while (quotLen > 0 && (quot[quotLen -1] & 0xFFFF) ==0) quotLen--;
        int[] trimmedQuot = quotLen < quot.length ? Arrays.copyOf(quot, quotLen) : quot;
        // Trim remainder (should be < divisor.degree())
        int remLen = Math.min(divisor.degree(), rem.length);
        while (remLen >0 && (rem[remLen -1] &0xFFFF) ==0) remLen--;
        int[] trimmedRem = remLen < rem.length ? Arrays.copyOf(rem, remLen) : rem;
        return new int[][]{trimmedQuot, trimmedRem};
    }

    public int evaluate(int x) {
        int result =0;
        for (int i = coefficients.length -1; i >=0; i--) {
            result = GF216.add(GF216.multiply(result, x), coefficients[i]) & 0xFFFF;
        }
        return result & 0xFFFF;
    }

    public static Polynomial buildGenerator(int numRoots, int rootBase) {
        Polynomial gen = new Polynomial(1); // g(x) = 1
        for (int i=0; i < numRoots; i++) {
            int root = GF216.exp(rootBase + i); // α^(rootBase +i)
            // Factor: (x + root) = [root, 1]
            Polynomial factor = new Polynomial(root, 1);
            gen = gen.multiply(factor);
        }
        return gen;
    }

    public int[] getCoefficients() {
        return coefficients.clone();
    }

    @Override
    public String toString() {
        return Arrays.toString(coefficients);
    }
}
