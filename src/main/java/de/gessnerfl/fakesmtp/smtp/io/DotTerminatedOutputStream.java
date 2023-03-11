package de.gessnerfl.fakesmtp.smtp.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * DotTerminatedOutputStream makes possible to end mail data with a "CRLF .
 * CRLF" sequence in such a way that no unnecessary beginning CRLF pair is added
 * if the original output already ends with it.
 *
 * See RFC 5321 4.1.1.4. second paragraph
 */
public class DotTerminatedOutputStream extends FilterOutputStream {
	private static final byte[] DOT_CRLF = new byte[] { '.', '\r', '\n' };

	private static final byte[] CRLF_DOT_CRLF = new byte[] { '\r', '\n', '.', '\r', '\n' };

	/**
	 * The last bytes written out by the {@link #write()} function. The first byte
	 * in the array contains the penultimate, the second contains the last byte
	 * written out by the write function. It's initial value is CR LF, so it is
	 * possible to write out empty data.
	 */
	private final byte[] lastBytes = new byte[] { '\r', '\n' };

	public DotTerminatedOutputStream(final OutputStream out) {
		super(out);
	}

	@Override
	public void write(final int b) throws IOException {
		lastBytes[0] = lastBytes[1];
		lastBytes[1] = (byte) b;
		super.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		if (len == 1) {
			lastBytes[0] = lastBytes[1];
			lastBytes[1] = b[off];
		} else if (len >= 2) {
			lastBytes[0] = b[off + len - 2];
			lastBytes[1] = b[off + len - 1];
		}
		super.write(b, off, len);
	}

	/**
	 * Writes ". CR LF" to the wrapped stream, but prefixes it with another CR LF
	 * sequence if it is missing from the end.
	 *
	 * @throws IOException on IO error
	 */
	public void writeTerminatingSequence() throws IOException {
		if (lastBytes[0] == '\r' && lastBytes[1] == '\n') {
			super.write(DOT_CRLF);
		} else {
			super.write(CRLF_DOT_CRLF);
		}
	}
}
