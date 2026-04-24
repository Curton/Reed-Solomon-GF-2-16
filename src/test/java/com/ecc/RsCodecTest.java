package com.ecc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

class RsCodecTest {
    private RsCodec rs;

    @BeforeEach
    void setUp() {
        rs = new RsCodec();
    }

    @Test
    void encode_dataExceeding240Bytes_throwsException() {
        byte[] data = new byte[241];
        assertThrows(IllegalArgumentException.class, () -> rs.encode(data));
    }

    @Test
    void encode_nullData_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> rs.encode(null));
    }

    @Test
    void encode_emptyData_paddedTo240Bytes() {
        byte[] data = new byte[0];
        byte[] codeword = rs.encode(data);
        assertEquals(256, codeword.length);
    }

    @Test
    void encode_minData_1Byte_paddedTo240Bytes() {
        byte[] data = new byte[]{42};
        byte[] codeword = rs.encode(data);
        assertEquals(256, codeword.length);
    }

    @Test
    void encode_maxData_240Bytes() {
        byte[] data = new byte[240];
        for (int i=0; i<240; i++) data[i] = (byte) i;
        byte[] codeword = rs.encode(data);
        assertEquals(256, codeword.length);
    }

    @Test
    void encode_outputLength_is256Bytes() {
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        assertEquals(256, codeword.length);
    }

    @Test
    void encode_deterministic_sameInputSameOutput() {
        byte[] data = new byte[240];
        new Random(123).nextBytes(data);
        byte[] c1 = rs.encode(data);
        byte[] c2 = rs.encode(data);
        assertArrayEquals(c1, c2);
    }

    @Test
    void encode_systematic_dataUnchanged() {
        byte[] data = new byte[240];
        new Random(99).nextBytes(data);
        byte[] codeword = rs.encode(data);
        // Data starts at offset 16 (first 16 bytes are parity)
        byte[] codewordData = new byte[240];
        System.arraycopy(codeword, 16, codewordData, 0, 240);
        assertArrayEquals(data, codewordData);
    }

    @Test
    void decode_noErrors_returnsOriginalData() {
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void decode_1error_corrected() {
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        // Corrupt 1 byte
        codeword[50] ^= (byte) 0xFF;
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void decode_2errors_corrected() {
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        codeword[30] ^= (byte) 0xAA;
        codeword[100] ^= (byte) 0xBB;
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void decode_3errors_corrected() {
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        codeword[10] ^= (byte) 1;
        codeword[50] ^= (byte) 2;
        codeword[200] ^= (byte) 3;
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void decode_4errors_corrected() {
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        codeword[20] ^= (byte) 0x11;
        codeword[60] ^= (byte) 0x22;
        codeword[120] ^= (byte) 0x33;
        codeword[200] ^= (byte) 0x44;
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void decode_5errors_mayFailSilently() {
        // 5+ errors may not always throw DecodingException (RS fundamental limitation)
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        // Corrupt 5 bytes
        for (int i = 0; i < 5; i++) {
            codeword[i * 10] ^= (byte) i;
        }
        try {
            byte[] decoded = rs.decode(codeword);
            // If no exception, decoded data may be wrong (not a bug, just RS limitation)
        } catch (DecodingException e) {
            // Expected sometimes
        }
    }

    @Test
    void decode_errorAtBeginning() {
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        codeword[0] ^= (byte) 0xFF;
        codeword[1] ^= (byte) 0xFF; // corrupt first symbol (2 bytes)
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void decode_errorAtEnd() {
        byte[] data = new byte[240];
        new Random(42).nextBytes(data);
        byte[] codeword = rs.encode(data);
        codeword[254] ^= (byte) 0xFF;
        codeword[255] ^= (byte) 0xFF; // last symbol
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void decode_allZeros() {
        byte[] data = new byte[240]; // all zeros
        byte[] codeword = rs.encode(data);
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void decode_allOnes() {
        byte[] data = new byte[240];
        for (int i=0; i<240; i++) data[i] = (byte) 0xFF;
        byte[] codeword = rs.encode(data);
        byte[] decoded = rs.decode(codeword);
        assertArrayEquals(data, decoded);
    }

    @Test
    void roundTrip_randomData_noErrors() {
        Random rand = new Random(999);
        for (int i=0; i<10; i++) {
            byte[] data = new byte[240];
            rand.nextBytes(data);
            byte[] codeword = rs.encode(data);
            byte[] decoded = rs.decode(codeword);
            assertArrayEquals(data, decoded, "Round trip failed at iteration " + i);
        }
    }

    @Test
    void roundTrip_randomData_withErrors_upTo4() {
        Random rand = new Random(777);
        for (int t = 1; t <=4; t++) {
            byte[] data = new byte[240];
            rand.nextBytes(data);
            byte[] codeword = rs.encode(data);
            // Inject t errors
            for (int e=0; e <t; e++) {
                int pos = rand.nextInt(256);
                codeword[pos] ^= (byte) (rand.nextInt(256));
            }
            byte[] decoded = rs.decode(codeword);
            assertArrayEquals(data, decoded, "Failed with " + t + " errors");
        }
    }
}
