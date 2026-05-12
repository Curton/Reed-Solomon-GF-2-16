# Reed-Solomon ECC Java Project

[![Build](https://github.com/Curton/Reed-Solomon-GF-2-16/actions/workflows/build.yml/badge.svg)](https://github.com/Curton/Reed-Solomon-GF-2-16/actions/workflows/build.yml)

> A Java implementation of Reed-Solomon error-correcting codes (RS(128,120) over GF(2^16)), capable of encoding 240 bytes of data into 256-byte codewords and correcting up to 4 symbol errors.

## Overview
RS(128,120) encoder/decoder over GF(2^16), implemented in Java with Maven.

## Configuration
- **GF(2^16)** with primitive polynomial `x^16 + x^12 + x^3 + x + 1` (0x1100B)
- **RS(128,120)**: 120 data symbols (240 bytes), 8 parity symbols (16 bytes), 128 total (256 bytes wire size)
- **Error correction**: Corrects up to 4 errors (t=4)

## Status
- ✅ **Encode**: Working correctly (systematic, parity + data)
- ✅ **GF(2^16) arithmetic**: Log/exp tables, all operations
- ✅ **Berlekamp-Massey**: Correctly computes error locator polynomial σ(x)
- ✅ **Chien search**: Correctly finds error positions
- ✅ **Forney algorithm**: Correctly computes error magnitudes
- ✅ **Decode**: Works for 0-4 errors
- ⚠️ **5+ errors**: May silently produce wrong data (fundamental limitation - RS can't always detect >4 errors)

## Building & Testing

### With Maven (recommended)
```bash
mvn clean test
mvn exec:java -Dexec.mainClass="com.ecc.RsCodec"
```

### Manual compilation
```bash
mkdir -p target/classes
javac -d target/classes src/main/java/com/ecc/*.java
java -cp target/classes com.ecc.RsCodec
```

### Run tests only
```bash
mvn test
```

Test results are in `target/surefire-reports/`.

## Project Structure
```
src/
├── main/java/com/ecc/
│   ├── GF216.java              # GF(2^16) arithmetic (log/exp tables)
│   ├── Polynomial.java          # Polynomial ops over GF(2^16)
│   ├── RsCodec.java            # Main encoder/decoder
│   ├── ReedSolomonException.java  # Base exception
│   └── DecodingException.java    # Decoding failure exception
└── test/java/com/ecc/
    ├── GF216Test.java
    ├── PolynomialTest.java
    ├── RsCodecTest.java
    └── RSIntegrationTest.java
```

## Algorithm Details

### Encoding
1. Pad data to 240 bytes (120 symbols) with zeros
2. Convert bytes to GF(2^16) symbols (big-endian, 2 bytes per symbol)
3. Compute parity via polynomial division: remainder of `data(x) * x^8` divided by generator polynomial `g(x)`
4. Generator polynomial: `g(x) = (x - α^0)(x - α^1)...(x - α^7)` where α = 2
5. Output: 16 bytes parity + 240 bytes data = 256 bytes (systematic encoding)

### Decoding (Berlekamp-Massey Algorithm)
1. **Syndrome computation**: Evaluate received polynomial at α^0, α^1, ..., α^7
2. **Error locator (Berlekamp-Massey)**: Find σ(x) whose roots indicate error positions
3. **Chien search**: Find error positions by evaluating σ(α^(-i)) for i = 0..127
4. **Forney algorithm**: Compute error magnitudes using error evaluator polynomial Ω(x)
5. **Correction**: XOR error magnitudes into received symbols
6. **Verification**: Recompute syndromes to confirm correction

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
- `IllegalArgumentException` - Data exceeds 240 bytes
- `NullPointerException` - Null data or codeword
- `DecodingException` - Too many errors (>4) or decoding failure

## Notes
- The 5+ error case may not always throw `DecodingException` because RS decoding might "correct" to a different valid codeword
- This is a fundamental limitation: with 5+ errors, the received word might be closer to a different valid codeword
- The post-correction syndrome check helps detect some failures, but not all
- All arithmetic is performed in GF(2^16) using log/exp tables for O(1) multiplication
