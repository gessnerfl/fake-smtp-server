package de.gessnerfl.fakesmtp.server.smtp;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import de.gessnerfl.fakesmtp.server.smtp.io.DotTerminatedInputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DotTerminatedInputStreamTest {
	@Test
	void testEmpty() throws IOException {
		final InputStream in = new ByteArrayInputStream(".\r\n".getBytes(StandardCharsets.US_ASCII));
		try (DotTerminatedInputStream stream = new DotTerminatedInputStream(in)) {
			assertEquals(-1, stream.read());
		}
	}

	@Test
	void testPreserveLastCrLf() throws IOException {
		final InputStream in = new ByteArrayInputStream("a\r\n.\r\n".getBytes(StandardCharsets.US_ASCII));
		final DotTerminatedInputStream stream = new DotTerminatedInputStream(in);
		assertEquals("a\r\n", readFull(stream));
	}

	@Test
	void testDotDot() throws IOException {
		final InputStream in = new ByteArrayInputStream("..\r\n.\r\n".getBytes(StandardCharsets.US_ASCII));
		final DotTerminatedInputStream stream = new DotTerminatedInputStream(in);
		assertEquals("..\r\n", readFull(stream));
	}

	@Test
	void testMissingDotLine() throws IOException {
		assertThrows(EOFException.class, () -> {
			final InputStream in = new ByteArrayInputStream("a\r\n".getBytes(StandardCharsets.US_ASCII));
			final DotTerminatedInputStream stream = new DotTerminatedInputStream(in);
			readFull(stream);
		});
	}

	private String readFull(final DotTerminatedInputStream in) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		int ch;
		while (-1 != (ch = in.read())) {
			out.write(ch);
		}
		return out.toString(StandardCharsets.US_ASCII);
	}
}
