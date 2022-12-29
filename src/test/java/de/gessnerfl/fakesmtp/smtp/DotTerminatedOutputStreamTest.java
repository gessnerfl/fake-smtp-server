package de.gessnerfl.fakesmtp.smtp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import de.gessnerfl.fakesmtp.smtp.io.DotTerminatedOutputStream;
import org.junit.jupiter.api.Test;

class DotTerminatedOutputStreamTest {
	@Test
	void testEmpty() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.writeTerminatingSequence();
			assertArrayEquals(".\r\n".getBytes(StandardCharsets.US_ASCII), out.toByteArray());
		}
	}

	@Test
	void testMissingCrLf() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.write('a');
			stream.writeTerminatingSequence();
			assertArrayEquals("a\r\n.\r\n".getBytes(StandardCharsets.US_ASCII), out.toByteArray());
		}
	}

	@Test
	void testMissingCrLfByteArray() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.write(new byte[] { 'a' });
			stream.writeTerminatingSequence();
			assertArrayEquals("a\r\n.\r\n".getBytes(StandardCharsets.US_ASCII), out.toByteArray());
		}
	}

	@Test
	void testExistingCrLf() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.write('a');
			stream.write('\r');
			stream.write('\n');
			stream.writeTerminatingSequence();
			assertArrayEquals("a\r\n.\r\n".getBytes(StandardCharsets.US_ASCII), out.toByteArray());
		}
	}

	@Test
	void testExistingCrLfByteArray() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.write(new byte[] { 'a', '\r', '\n' });
			stream.writeTerminatingSequence();
			assertArrayEquals("a\r\n.\r\n".getBytes(StandardCharsets.US_ASCII), out.toByteArray());
		}
	}
}
