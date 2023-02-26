package de.gessnerfl.fakesmtp.smtp.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CRLFOutputStreamTest {

    public static final byte[] TEST_STRING_BYTES = "this is a test\rstring\nwhich should be normalized".getBytes(StandardCharsets.UTF_8);
    public static final byte[] EXPECTED_RESULT_STRING_BYTES = "this is a test\r\nstring\r\nwhich should be normalized".getBytes(StandardCharsets.UTF_8);

    @Test
    void shouldNormalizeIsolatedCarriageReturnOrLineFeed() throws IOException {
        var baos = new ByteArrayOutputStream();
        var sut = new CRLFOutputStream(baos);

        for (byte testStringByte : TEST_STRING_BYTES) {
            sut.write(testStringByte);
        }

        assertArrayEquals(EXPECTED_RESULT_STRING_BYTES, baos.toByteArray());
    }

    @Test
    void shouldNormalizeIsolatedCarriageReturnOrLineFeedIntoArray() throws IOException {
        var baos = new ByteArrayOutputStream();
        var sut = new CRLFOutputStream(baos);

        sut.write(TEST_STRING_BYTES, 0, TEST_STRING_BYTES.length);

        assertArrayEquals(EXPECTED_RESULT_STRING_BYTES, baos.toByteArray());
    }

}