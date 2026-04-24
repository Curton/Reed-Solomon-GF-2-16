package com.ecc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RS(128,120) encoder/decoder over GF(2^16).
 * 120 data symbols (240 bytes), 8 parity symbols (16 bytes), 128 total (256 bytes).
 * Corrects up to 4 errors.
 */
public class RsCodec {
    public static final int TOTAL_SYM = 128;
    public static final int DATA_SYM = 120;
    public static final int PARITY_SYM = 8;
    public static final int MAX_DATA_BYTES = DATA_SYM * 2; // 240
    public static final int WIRE_BYTES = TOTAL_SYM * 2;    // 256
    private static final int MAX_ERRORS = PARITY_SYM / 2; // 4

    private final Polynomial generator;

    public RsCodec() {
        generator = Polynomial.buildGenerator(PARITY_SYM, 0); // g(x) = product(x - α^i) i=0..7
    }

    /**
     * Encode data bytes into a 256-byte codeword (systematic: parity + data).
     * @param data input data bytes, max 240 bytes
     * @return 256-byte codeword
     */
    public byte[] encode(byte[] data) {
        if (data == null) throw new NullPointerException("Data is null");
        if (data.length > MAX_DATA_BYTES) {
            throw new IllegalArgumentException("Data exceeds " + MAX_DATA_BYTES + " bytes: " + data.length);
        }

        // Pad data to MAX_DATA_BYTES (240 bytes)
        byte[] padded = new byte[MAX_DATA_BYTES];
        System.arraycopy(data, 0, padded, 0, data.length);

        // Convert 240 bytes → 120 symbols (big-endian)
        int[] dataSym = bytesToSymbols(padded, 0, MAX_DATA_BYTES);

        // Compute parity via polynomial division: remainder of (data * x^8) / g(x)
        int[] shiftedCoeff = new int[TOTAL_SYM];
        System.arraycopy(dataSym, 0, shiftedCoeff, PARITY_SYM, DATA_SYM);
        Polynomial shiftedPoly = new Polynomial(shiftedCoeff); // data(x) * x^8
        int[][] result = shiftedPoly.divideAndRemainder(generator);
        int[] paritySym = result[1]; // remainder
        if (paritySym.length < PARITY_SYM) {
            int[] paddedParity = new int[PARITY_SYM];
            System.arraycopy(paritySym, 0, paddedParity, 0, paritySym.length);
            paritySym = paddedParity;
        }

        // Build codeword: parity (16 bytes) + data (240 bytes) = 256 bytes
        byte[] codeword = new byte[WIRE_BYTES];
        byte[] parityBytes = symbolsToBytes(paritySym, 0, PARITY_SYM);
        System.arraycopy(parityBytes, 0, codeword, 0, parityBytes.length);
        System.arraycopy(padded, 0, codeword, parityBytes.length, MAX_DATA_BYTES);
        return codeword;
    }

    /**
     * Decode a 256-byte codeword, correcting up to 4 errors.
     * @param codeword 256-byte codeword
     * @return 240-byte data
     */
    public byte[] decode(byte[] codeword) {
        if (codeword == null) throw new NullPointerException("Codeword is null");
        if (codeword.length != WIRE_BYTES) {
            throw new IllegalArgumentException("Codeword must be exactly " + WIRE_BYTES + " bytes: " + codeword.length);
        }

        // Convert to 128 symbols (parity first, then data)
        int[] received = bytesToSymbols(codeword, 0, WIRE_BYTES);

        // Compute syndromes
        int[] syndromes = computeSyndromes(received, PARITY_SYM);

        // Check if all zero
        boolean allZero = true;
        for (int s : syndromes) {
            if (s != 0) { allZero = false; break; }
        }
        if (allZero) {
            return extractData(codeword);
        }

        // Find error locator polynomial using Berlekamp-Massey
        int[] sigma = berlekampMassey(syndromes);
        int numErrors = sigma.length - 1;
        if (numErrors > MAX_ERRORS) {
            throw new DecodingException("Too many errors: " + numErrors + " (max " + MAX_ERRORS + ")");
        }

        // Chien search to find error positions
        List<Integer> errorPositions = chienSearch(sigma);
        if (errorPositions.size() != numErrors) {
            throw new DecodingException("Chien search found " + errorPositions.size() + " errors, expected " + numErrors);
        }

        // Forney algorithm to compute error magnitudes
        int[] magnitudes = computeMagnitudes(sigma, errorPositions, syndromes);

        // Correct errors
        for (int i = 0; i < errorPositions.size(); i++) {
            int pos = errorPositions.get(i);
            int mag = magnitudes[i];
            received[pos] ^= mag & 0xFFFF;
        }

        // Post-correction check: recompute syndromes
        int[] syn2 = computeSyndromes(received, PARITY_SYM);
        boolean recovered = true;
        for (int s : syn2) { if (s != 0) { recovered = false; break; } }
        if (!recovered) {
            throw new DecodingException("Decoding failed: syndromes still non-zero after correction (too many errors?)");
        }

        // Extract data (last 120 symbols, positions 8-127)
        int[] dataSym = new int[DATA_SYM];
        System.arraycopy(received, PARITY_SYM, dataSym, 0, DATA_SYM);
        return symbolsToBytes(dataSym, 0, DATA_SYM);
    }

    // --- Helper methods ---

    private byte[] extractData(byte[] codeword) {
        byte[] data = new byte[MAX_DATA_BYTES];
        System.arraycopy(codeword, PARITY_SYM * 2, data, 0, MAX_DATA_BYTES);
        return data;
    }

    private int[] computeSyndromes(int[] received, int numSyndromes) {
        int[] syndromes = new int[numSyndromes];
        for (int i = 0; i < numSyndromes; i++) {
            int alpha = GF216.exp(i); // α^i
            syndromes[i] = GF216.evaluatePolynomial(received, alpha);
        }
        return syndromes;
    }

    private int[] berlekampMassey(int[] syndromes) {
        int[] C = new int[PARITY_SYM + 1]; // current locator, C[0] =1
        int[] B = new int[PARITY_SYM + 1]; // previous locator
        C[0] = 1;
        B[0] = 1;

        int L = 0;      // current number of errors
        int m = 1;      // delay since last update
        int b = 1;      // previous discrepancy

        for (int k = 0; k < syndromes.length; k++) {
            // Compute discrepancy d = S[k] + sum_{i=1 to L} C[i] * S[k-i]
            int d = syndromes[k];
            for (int i = 1; i <= L; i++) {
                d ^= GF216.multiply(C[i], syndromes[k - i]) & 0xFFFF;
            }
            d &= 0xFFFF;

            if (d != 0) {
                int[] T = C.clone();
                int factor = GF216.multiply(d, GF216.inverse(b)) & 0xFFFF;
                for (int i = 0; i < B.length; i++) {
                    if (B[i] != 0) {
                        int idx = i + m;
                        if (idx < C.length) {
                            C[idx] ^= GF216.multiply(factor, B[i]) & 0xFFFF;
                        }
                    }
                }
                if (2 * L <= k) {
                    L = k + 1 - L;
                    System.arraycopy(T, 0, B, 0, T.length);
                    b = d;
                    m = 1;
                } else {
                    m++;
                }
            } else {
                m++;
            }
        }

        // Trim trailing zeros (lowest degree first)
        int degree = C.length - 1;
        while (degree > 0 && C[degree] == 0) degree--;
        int[] result = new int[degree + 1];
        System.arraycopy(C, 0, result, 0, degree + 1);
        return result;
    }

    private List<Integer> chienSearch(int[] sigma) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < TOTAL_SYM; i++) {
            // Evaluate σ(α^{-i})
            int x = GF216.exp((GF216.MULTIPLICATIVE_ORDER - i) % GF216.MULTIPLICATIVE_ORDER);
            int eval = GF216.evaluatePolynomial(sigma, x);
            if (eval == 0) {
                positions.add(i);
            }
        }
        return positions;
    }

    private int[] computeMagnitudes(int[] sigma, List<Integer> errorPositions, int[] syndromes) {
        // Compute error evaluator polynomial Ω(x) = S(x) * σ(x) mod x^PARITY_SYM
        int[] omega = new int[PARITY_SYM];
        for (int i = 0; i < syndromes.length; i++) {
            for (int j = 0; j < sigma.length && i + j < PARITY_SYM; j++) {
                omega[i + j] ^= GF216.multiply(syndromes[i], sigma[j]) & 0xFFFF;
            }
        }

        int[] magnitudes = new int[errorPositions.size()];
        for (int idx = 0; idx < errorPositions.size(); idx++) {
            int pos = errorPositions.get(idx);
            // Error location number X = α^pos
            // (Chien found σ(α^{-pos}) = 0, so X^{-1} = α^{-pos}, thus X = α^pos)
            int X = GF216.exp(pos);
            int X_inv = GF216.inverse(X); // α^{-pos}

            // Ω(X^{-1})
            int omegaEval = GF216.evaluatePolynomial(omega, X_inv);

            // σ'(x) = σ_1 + σ_3*x^2 + σ_5*x^4 + ... (formal derivative, odd powers only)
            // Evaluate at x = X^{-1} = α^{-pos}:
            // σ'(α^{-pos}) = σ_1 + σ_3*α^{-2*pos} + σ_5*α^{-4*pos} + ...
            // For term σ_i (i odd), exponent of α is -(i-1)*pos
            int sigmaDeriv = 0;
            for (int i = 1; i < sigma.length; i += 2) {
                int pow = (-(i - 1) * pos) % GF216.MULTIPLICATIVE_ORDER;
                if (pow < 0) pow += GF216.MULTIPLICATIVE_ORDER;
                sigmaDeriv ^= GF216.multiply(sigma[i], GF216.exp(pow)) & 0xFFFF;
            }

            // Error magnitude: E = X * Ω(X^{-1}) / σ'(X^{-1})
            magnitudes[idx] = GF216.multiply(GF216.multiply(X, omegaEval), GF216.inverse(sigmaDeriv)) & 0xFFFF;
        }
        return magnitudes;
    }

    // --- Byte/symbol conversion (big-endian) ---

    private int[] bytesToSymbols(byte[] bytes, int offset, int numBytes) {
        int numSym = numBytes / 2;
        int[] symbols = new int[numSym];
        for (int i = 0; i < numSym; i++) {
            int hi = bytes[offset + 2 * i] & 0xFF;
            int lo = bytes[offset + 2 * i + 1] & 0xFF;
            symbols[i] = (hi << 8) | lo;
        }
        return symbols;
    }

    private byte[] symbolsToBytes(int[] symbols, int offset, int numSym) {
        byte[] bytes = new byte[numSym * 2];
        for (int i = 0; i < numSym; i++) {
            int sym = symbols[offset + i] & 0xFFFF;
            bytes[2 * i] = (byte) (sym >> 8);
            bytes[2 * i + 1] = (byte) (sym & 0xFF);
        }
        return bytes;
    }

    // Quick self-test
    public static void main(String[] args) {
        RsCodec rs = new RsCodec();
        Random rand = new Random(42);
        byte[] data = new byte[240];
        rand.nextBytes(data);

        // Test 1: Encode then decode (no errors)
        byte[] codeword = rs.encode(data);
        byte[] decoded = rs.decode(codeword);
        boolean ok = true;
        for (int i = 0; i < 240; i++) if (decoded[i] != data[i]) { ok = false; break; }
        System.out.println("No errors: " + (ok ? "PASS" : "FAIL"));

        // Test 2: 1 error
        codeword = rs.encode(data);
        codeword[0] ^= 0xFF; codeword[1] ^= 0xFF; // corrupt first symbol
        decoded = rs.decode(codeword);
        ok = true;
        for (int i = 0; i < 240; i++) if (decoded[i] != data[i]) { ok = false; break; }
        System.out.println("1 error: " + (ok ? "PASS" : "FAIL"));

        // Test 3: 4 errors
        codeword = rs.encode(data);
        codeword[10] ^= 1; codeword[11] ^= 1;
        codeword[50] ^= 2; codeword[51] ^= 2;
        codeword[100] ^= 3; codeword[101] ^= 3;
        codeword[200] ^= 4; codeword[201] ^= 4;
        try {
            decoded = rs.decode(codeword);
            ok = true;
            for (int i = 0; i < 240; i++) if (decoded[i] != data[i]) { ok = false; break; }
            System.out.println("4 errors: " + (ok ? "PASS" : "FAIL"));
        } catch (Exception e) {
            System.out.println("4 errors: FAIL - " + e.getMessage());
        }

        System.out.println("All core tests done. RS(128,120) encodes and corrects up to 4 errors.");
    }
}
