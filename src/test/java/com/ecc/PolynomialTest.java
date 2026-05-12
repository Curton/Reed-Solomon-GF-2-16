package com.ecc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PolynomialTest {
    @Test
    void degree_emptyPolynomial() {
        Polynomial p = new Polynomial(0);
        assertEquals(-1, p.degree());
    }

    @Test
    void degree_nonZeroPolynomial() {
        Polynomial p = new Polynomial(1, 2, 3); // 1 +2x +3x²
        assertEquals(2, p.degree());
    }

    @Test
    void add_polynomials() {
        Polynomial a = new Polynomial(1, 2); // 1+2x
        Polynomial b = new Polynomial(3, 4); // 3+4x
        Polynomial sum = a.add(b);
        assertArrayEquals(new int[]{1^3, 2^4}, sum.getCoefficients());
    }

    @Test
    void multiply_polynomials() {
        // (1 + x) * (1 + x) = 1 + (1+1)x + x² = 1 + 0x + x² (since addition is XOR in GF)
        Polynomial a = new Polynomial(1, 1); // 1+x
        Polynomial prod = a.multiply(a);
        int[] coeff = prod.getCoefficients();
        assertEquals(3, coeff.length);
        assertEquals(1, coeff[0]); // constant term
        assertEquals(0, coeff[1]); // x term (1+1=0 in GF)
        assertEquals(1, coeff[2]); // x² term
    }

    @Test
    void divideAndRemainder_exact() {
        // (x² + 3x +2) / (x+1) = x+2, remainder 0
        // x²+3x+2 = (x+1)(x+2)
        Polynomial dividend = new Polynomial(2, 3, 1); // 2 +3x +x²
        Polynomial divisor = new Polynomial(1, 1); // 1 +x
        int[][] result = dividend.divideAndRemainder(divisor);
        int[] quot = result[0], rem = result[1];
        assertArrayEquals(new int[]{2, 1}, quot); // 2 +x
        assertEquals(0, rem.length); // remainder 0
    }

    @Test
    void generatorPolynomial_degree8() {
        Polynomial gen = Polynomial.buildGenerator(8, 0); // g(x) for RS(128,120)
        assertEquals(8, gen.degree());
    }

    @Test
    void generatorPolynomial_rootsArePowersOfAlpha() {
        Polynomial gen = Polynomial.buildGenerator(8, 0);
        for (int i = 0; i < 8; i++) {
            int alpha = GF216.exp(i); // α^i
            int eval = gen.evaluate(alpha);
            assertEquals(0, eval, "g(α^" + i + ") should be 0");
        }
    }

    @Test
    void evaluate_polynomial() {
        Polynomial p = new Polynomial(1, 2, 3); // 1 +2x +3x²
        // Evaluate at x=0: should be constant term 1
        assertEquals(1, p.evaluate(0));
        // Evaluate at x=1: 1 +2 +3 = 1 XOR 2 XOR 3 = 0
        assertEquals(0, p.evaluate(1));
    }

    @Test
    void toString_returnsArrayRepresentation() {
        Polynomial p = new Polynomial(1, 2, 3);
        assertEquals("[1, 2, 3]", p.toString());
    }

    @Test
    void getCoefficient_negativeDegree_returnsZero() {
        Polynomial p = new Polynomial(1, 2, 3);
        assertEquals(0, p.getCoefficient(-1));
    }

    @Test
    void getCoefficient_exceedsDegree_returnsZero() {
        Polynomial p = new Polynomial(1, 2, 3); // degree 2
        assertEquals(0, p.getCoefficient(5));
    }

    @Test
    void add_differentLengths() {
        Polynomial a = new Polynomial(1, 2, 3); // degree 2
        Polynomial b = new Polynomial(4);        // degree 0
        Polynomial sum = a.add(b);
        // 1^4=5, 2, 3
        assertArrayEquals(new int[]{5, 2, 3}, sum.getCoefficients());
    }

    @Test
    void add_thisShorterThanOther() {
        Polynomial a = new Polynomial(4);        // degree 0
        Polynomial b = new Polynomial(1, 2, 3); // degree 2
        Polynomial sum = a.add(b);
        // 4^1=5, 2, 3
        assertArrayEquals(new int[]{5, 2, 3}, sum.getCoefficients());
    }

    @Test
    void divideAndRemainder_divisionByZero_throwsArithmeticException() {
        Polynomial dividend = new Polynomial(1, 2, 3);
        Polynomial zero = new Polynomial(0);
        assertThrows(ArithmeticException.class, () -> dividend.divideAndRemainder(zero));
    }
}
