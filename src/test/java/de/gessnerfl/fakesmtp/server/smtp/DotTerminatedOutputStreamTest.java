package de.gessnerfl.fakesmtp.server.smtp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.gessnerfl.fakesmtp.server.smtp.io.DotTerminatedOutputStream;
import org.junit.jupiter.api.Test;

public class DotTerminatedOutputStreamTest {
	@Test
	public void testEmpty() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.writeTerminatingSequence();
			assertArrayEquals(".\r\n".getBytes("US-ASCII"), out.toByteArray());
		}
	}

	@Test
	public void testMissingCrLf() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.write('a');
			stream.writeTerminatingSequence();
			assertArrayEquals("a\r\n.\r\n".getBytes("US-ASCII"), out.toByteArray());
		}
	}

	@Test
	public void testMissingCrLfByteArray() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.write(new byte[] { 'a' });
			stream.writeTerminatingSequence();
			assertArrayEquals("a\r\n.\r\n".getBytes("US-ASCII"), out.toByteArray());
		}
	}

	@Test
	public void testExistingCrLf() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.write('a');
			stream.write('\r');
			stream.write('\n');
			stream.writeTerminatingSequence();
			assertArrayEquals("a\r\n.\r\n".getBytes("US-ASCII"), out.toByteArray());
		}
	}

	@Test
	public void testExistingCrLfByteArray() throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DotTerminatedOutputStream stream = new DotTerminatedOutputStream(out)) {
			stream.write(new byte[] { 'a', '\r', '\n' });
			stream.writeTerminatingSequence();
			assertArrayEquals("a\r\n.\r\n".getBytes("US-ASCII"), out.toByteArray());
		}
	}
}
