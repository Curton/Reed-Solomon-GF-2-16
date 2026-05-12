# Reed-Solomon ECC Java Project

[![Build](https://img.shields.io/github/actions/workflow/status/Curton/Reed-Solomon-GF-2-16/build.yml?branch=master&logo=github)](https://github.com/Curton/Reed-Solomon-GF-2-16/actions/workflows/build.yml)

> A Java implementation of Reed-Solomon error-correcting codes (RS(128,120) over GF(2^16)), capable of encoding 240 bytes of data into 256-byte codewords and correcting up to 4 symbol errors.

## Configuration
- **GF(2^16)** with primitive polynomial `x^16 + x^12 + x^3 + x + 1` (0x1100B)
- **RS(128,120)**: 120 data symbols (240 bytes), 8 parity symbols (16 bytes), 128 total (256 bytes wire size)
- **Error correction**: Corrects up to 4 symbol errors (t=4)

## Building & Testing

```bash
# Build and run all tests
mvn clean test

# Run the self-test harness
java -cp target/classes com.ecc.RsCodec
```

Test results and coverage reports in `target/surefire-reports/` and `target/site/jacoco/`.

## Project Structure
```
src/
├── main/java/com/ecc/
│   ├── GF216.java                # GF(2^16) arithmetic (log/exp tables)
│   ├── Polynomial.java            # Polynomial ops over GF(2^16)
│   ├── RsCodec.java              # RS(128,120) encoder/decoder
│   ├── ReedSolomonException.java  # Base exception
│   └── DecodingException.java    # Decoding failure exception
└── test/java/com/ecc/
    ├── GF216Test.java
    ├── PolynomialTest.java
    ├── RsCodecTest.java
    ├── RSIntegrationTest.java
    └── ExceptionTest.java
```

## Algorithm Details

### Encoding (Systematic)
1. Pad data to 240 bytes (120 symbols) with zeros
2. Convert bytes to GF(2^16) symbols (big-endian, 2 bytes per symbol)
3. Compute parity via polynomial division: remainder of `data(x) * x^8` divided by generator `g(x)`
4. Generator: `g(x) = (x - α^0)(x - α^1)...(x - α^7)` where `α = 2` in GF(2^16)
5. Output: 16 bytes parity + 240 bytes data = 256 bytes

### Decoding (Berlekamp-Massey)
1. **Syndrome computation** — evaluate received polynomial at `α^0 ... α^7`
2. **Error locator (Berlekamp-Massey)** — find σ(x) whose roots are error positions
3. **Chien search** — find error positions by evaluating `σ(α^(-i))` for `i = 0..127`
4. **Forney algorithm** — compute error magnitudes via error evaluator polynomial Ω(x)
5. **Correction** — XOR error magnitudes into received symbols at error positions
6. **Post-correction verification** — recompute syndromes; if non-zero, throw `DecodingException`
7. **Re-encode verification** — re-encode the corrected data and compare with corrected word; if mismatch, throw `DecodingException`

## Usage
```java
RsCodec rs = new RsCodec();

// Encode: 240 bytes → 256 bytes (16B parity + 240B data)
byte[] data = new byte[240];
new Random().nextBytes(data);
byte[] codeword = rs.encode(data); // 256 bytes

// Decode: 256 bytes → 240 bytes (corrects 0-4 errors)
byte[] decoded = rs.decode(codeword);
```

## Exception Handling
- `IllegalArgumentException` — data exceeds 240 bytes or codeword length ≠ 256
- `NullPointerException` — null data or codeword
- `DecodingException` — decoding failure (Chien search mismatch, post-correction syndrome non-zero, or re-encode verification failure)
- `ArithmeticException` — invalid GF(2^16) operations (division/inverse/log of zero)

## Error Detection Beyond t=4
RS(128,120) can correct up to 4 symbol errors by design. With 5+ symbol errors:
- The decoder detects most cases and throws `DecodingException` (via Chien search count mismatch, post-correction syndrome check, or re-encode verification)
- In rare cases, the corrupted word may be closer to a different valid codeword, resulting in a silent miscorrection (the decoder returns valid data that differs from the original)

The two-layer verification (syndrome check + re-encode) catches the majority of correction failures beyond the guaranteed correction capability.
