package com.ecc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GF216Test {
    @Test
    void expTable_initialValues() {
        assertEquals(1, GF216.exp(0)); // α^0=1
        assertEquals(2, GF216.exp(1)); // α^1=2
        assertEquals(4, GF216.exp(2)); // α^2=4
    }

    @Test
    void expTable_wrapAround() {
        // α^65535 =1
        assertEquals(1, GF216.exp(65535));
        assertEquals(2, GF216.exp(65536)); // α^65536 = α^1
    }

    @Test
    void logTable_inverseOfExp() {
        for (int i = 0; i < 100; i++) {
            int exp = GF216.exp(i);
            assertEquals(i % 65535, GF216.log(exp));
        }
    }

    @Test
    void add_isXor() {
        assertEquals(0xAAAA ^ 0x5555, GF216.add(0xAAAA, 0x5555) & 0xFFFF);
        assertEquals(0, GF216.add(123, 123));
    }

    @Test
    void multiply_identity() {
        assertEquals(5, GF216.multiply(5, 1));
        assertEquals(5, GF216.multiply(1, 5));
    }

    @Test
    void multiply_byZero() {
        assertEquals(0, GF216.multiply(123, 0));
        assertEquals(0, GF216.multiply(0, 456));
    }

    @Test
    void multiply_knownValues() {
        // α^a * α^b = α^(a+b mod 65535)
        int a = 10, b = 20;
        int expected = GF216.exp(a + b);
        assertEquals(expected, GF216.multiply(GF216.exp(a), GF216.exp(b)));
    }

    @Test
    void divide_knownValues() {
        int a = GF216.exp(10);
        int b = GF216.exp(3);
        int expected = GF216.exp(10 - 3);
        assertEquals(expected, GF216.divide(a, b));
    }

    @Test
    void inverse_multiplicative() {
        for (int i = 1; i <= 10; i++) {
            int val = GF216.exp(i);
            int inv = GF216.inverse(val);
            assertEquals(1, GF216.multiply(val, inv));
        }
    }

    @Test
    void evaluatePolynomial_linear() {
        // p(x) = 3 + 5x. In GF(2^16), addition is XOR, multiplication uses log/exp.
        // Evaluate at x=0: should be constant term 3
        int[] poly = {3,5}; // 3 +5x
        assertEquals(3, GF216.evaluatePolynomial(poly, 0));
        // Evaluate at x=1: 3 + 5*1 = 3 XOR 5 = 6
        assertEquals(6, GF216.evaluatePolynomial(poly, 1));
    }
}
