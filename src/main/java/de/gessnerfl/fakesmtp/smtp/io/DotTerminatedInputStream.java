package de.gessnerfl.fakesmtp.smtp.io;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream class that terminates the stream when it encounters a US-ASCII
 * encoded dot CR LF byte sequence immediately following a CR LF line end.
 */
public class DotTerminatedInputStream extends FilterInputStream {
	/**
	 * The last bytes returned by the {@link #read()} function. The first byte in
	 * the array contains the byte returned by the penultimate read() call. The
	 * second byte in the array contains the byte returned by the last read() call.
	 * EOF (-1) is not shifted into the array. It's initial value is CR LF, so the
	 * first character of the stream is considered to be the first character of a
	 * line. This makes it possible to receive empty data.
	 */
	private final byte[] lastBytes = new byte[] { '\r', '\n' };

	/**
	 * The buffer which contains the bytes read from the underlying stream in
	 * advance. These bytes are not yet returned by the {@link #read()} function.
	 * Null means uninitialized.
	 */
	private int[] nextBytes = null;

	/**
	 * Indicates that the last byte - not including the terminating sequence - of
	 * the wrapped stream was already returned by {@link #read()}
	 */
	private boolean endReached = false;

	/**
	 * A constructor for this object that takes a stream to be wrapped and a
	 * terminating character sequence.
	 *
	 * @param in the <code>InputStream</code> to be wrapped
	 * @throws IllegalArgumentException if the terminator array is null or empty
	 */
	public DotTerminatedInputStream(final InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		if (nextBytes == null) {
			initNextBytes();
		}
		if (endReached) {
			return -1;
		}
		if (lastBytesAreCrLf() && nextBytesAreDotCrLf()) {
			endReached = true;
			return -1;
		}
		final int result = nextBytes[0];
		if (result == -1) {
			// End of stream reached without seeing the terminator
			throw new EOFException("Pre-mature end of <CRLF>.<CRLF> terminated data");
		}
		readWrappedStream();
		return result;
	}

	private void initNextBytes() throws IOException {
		nextBytes = new int[3];
		nextBytes[0] = super.read();
		nextBytes[1] = super.read();
		nextBytes[2] = super.read();
	}

	private boolean lastBytesAreCrLf() {
		return lastBytes[0] == '\r' && lastBytes[1] == '\n';
	}

	private boolean nextBytesAreDotCrLf() {
		return nextBytes[0] == '.' && nextBytes[1] == '\r' && nextBytes[2] == '\n';
	}

	/**
	 * Shifts bytes in the buffers, reads a byte from the wrapped stream, and places
	 * it at the end of the nextBytes buffer.
	 */
	private void readWrappedStream() throws IOException {
		lastBytes[0] = lastBytes[1];
		// casting is safe, this function is not called if an - unexpected - EOF
		// was read
		lastBytes[1] = (byte) nextBytes[0];
		nextBytes[0] = nextBytes[1];
		nextBytes[1] = nextBytes[2];
		nextBytes[2] = super.read();
	}
}
