package com.ecc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

class RSIntegrationTest {
    private final RsCodec rs = new RsCodec();

    @Test
    void integration_encodeDecodeMultipleCalls() {
        Random rand = new Random(1);
        for (int i=0; i<100; i++) {
            byte[] data = new byte[240];
            rand.nextBytes(data);
            byte[] codeword = rs.encode(data);
            assertEquals(256, codeword.length);
            byte[] decoded = rs.decode(codeword);
            assertArrayEquals(data, decoded, "Failed at iteration " + i);
        }
    }

    @Test
    void integration_stress_randomInputs() {
        Random rand = new Random(42);
        for (int i=0; i<50; i++) {
            byte[] data = new byte[240];
            rand.nextBytes(data);
            byte[] codeword = rs.encode(data);
            // Inject 0-4 errors
            int errors = rand.nextInt(5); // 0-4
            for (int e=0; e<errors; e++) {
                int pos = rand.nextInt(256);
                codeword[pos] ^= (byte) (rand.nextInt(256));
            }
            byte[] decoded = rs.decode(codeword);
            assertArrayEquals(data, decoded, "Failed with " + errors + " errors at iteration " + i);
        }
    }

    @Test
    void integration_errorPositions_exhaustive() {
        // Test errors at various positions
        int[] positions = {0,1,2, 119,120,121, 254,255, 128, 64};
        for (int pos : positions) {
            byte[] data = new byte[240];
            new Random(pos).nextBytes(data);
            byte[] codeword = rs.encode(data);
            codeword[pos] ^= (byte) 0xFF;
            byte[] decoded = rs.decode(codeword);
            assertArrayEquals(data, decoded, "Failed at position " + pos);
        }
    }
}
