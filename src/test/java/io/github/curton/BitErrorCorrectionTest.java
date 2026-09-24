package io.github.curton;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Bit-level error correction volume tests:
 * - >= 256 no-error round trips
 * - single-bit errors over every bit position of the 256-byte codeword (2048 >= 256 required)
 * - >= 512 unique random 2-bit error patterns
 * - >= 512 unique random 3-bit error patterns
 * - >= 512 unique random 4-bit error patterns
 */
class BitErrorCorrectionTest {

    private static final int WIRE_BITS = RsCodec.WIRE_BYTES * 8; // 2048

    private final RsCodec rs = new RsCodec();

    @Test
    void noError_256RoundTrips() {
        Random rand = new Random(0xC0DE1);
        for (int i = 0; i < 256; i++) {
            byte[] data = randomData(rand);
            assertArrayEquals(data, rs.decode(rs.encode(data)), "no-error trial " + i);
        }
    }

    @Test
    void singleBitError_everyBitPosition_corrected() {
        byte[] data = randomData(new Random(42));
        byte[] codeword = rs.encode(data);
        for (int bit = 0; bit < WIRE_BITS; bit++) {
            byte[] corrupted = codeword.clone();
            corrupted[bit >>> 3] ^= (byte) (1 << (bit & 7));
            assertArrayEquals(data, rs.decode(corrupted), "bit " + bit + " not corrected");
        }
    }

    @Test
    void twoBitErrors_512UniqueRandomPatterns_corrected() {
        uniqueRandomBitPatterns(2, 512, new Random(2024));
    }

    @Test
    void threeBitErrors_512UniqueRandomPatterns_corrected() {
        uniqueRandomBitPatterns(3, 512, new Random(2025));
    }

    @Test
    void fourBitErrors_512UniqueRandomPatterns_corrected() {
        uniqueRandomBitPatterns(4, 512, new Random(2026));
    }

    /** Runs {@code trials} decodes, each on fresh random data with a never-repeating set of bit positions. */
    private void uniqueRandomBitPatterns(int numBits, int trials, Random rand) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < trials; i++) {
            byte[] data = randomData(rand);
            byte[] codeword = rs.encode(data);
            int[] positions = distinctPositions(numBits, rand);
            while (!seen.add(Arrays.toString(positions))) {
                positions = distinctPositions(numBits, rand);
            }
            byte[] corrupted = codeword.clone();
            for (int bit : positions) {
                corrupted[bit >>> 3] ^= (byte) (1 << (bit & 7));
            }
            assertArrayEquals(data, rs.decode(corrupted),
                    numBits + "-bit pattern " + Arrays.toString(positions) + " not corrected");
        }
    }

    private int[] distinctPositions(int numBits, Random rand) {
        Set<Integer> picked = new HashSet<>();
        while (picked.size() < numBits) {
            picked.add(rand.nextInt(WIRE_BITS));
        }
        int[] positions = new int[numBits];
        int idx = 0;
        for (int p : picked) positions[idx++] = p;
        Arrays.sort(positions);
        return positions;
    }

    private byte[] randomData(Random rand) {
        byte[] data = new byte[RsCodec.MAX_DATA_BYTES];
        rand.nextBytes(data);
        return data;
    }
}
