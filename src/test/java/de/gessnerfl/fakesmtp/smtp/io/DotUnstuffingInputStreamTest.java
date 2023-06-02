package de.gessnerfl.fakesmtp.smtp.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class DotUnstuffingInputStreamTest {

    public static final byte[] TEST_STRING_BYTES = "this is a test\r\n...this continues...".getBytes(StandardCharsets.UTF_8);
    public static final byte[] EXPECTED_RESULT_STRING_BYTES = "this is a test\r\n..this continues...".getBytes(StandardCharsets.UTF_8);

    @Test
    void shouldRemoveDotsWhenReadingThroughStream() throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(TEST_STRING_BYTES);
        DotUnstuffingInputStream sut = new DotUnstuffingInputStream(bis);

        byte[] result = new byte[EXPECTED_RESULT_STRING_BYTES.length];
        int b = sut.read();
        int pos = 0;
        while (b != -1) {
            result[pos] = (byte)b;
            pos++;
            b = sut.read();
        }

        assertArrayEquals(EXPECTED_RESULT_STRING_BYTES, result);
    }

    @Test
    void shouldRemoveDotsWhenReadingThroughStreamWithBuffer() throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(TEST_STRING_BYTES);
        DotUnstuffingInputStream sut = new DotUnstuffingInputStream(bis);

        byte[] result = new byte[EXPECTED_RESULT_STRING_BYTES.length];
        int offset = 0;
        int len = sut.read(result, offset, result.length);
        assertEquals(result.length, len);
        assertArrayEquals(EXPECTED_RESULT_STRING_BYTES, result);
    }

}