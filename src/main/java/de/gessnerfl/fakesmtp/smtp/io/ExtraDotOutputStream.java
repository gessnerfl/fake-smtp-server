package de.gessnerfl.fakesmtp.smtp.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Adds extra dot if dot occurs in message body at beginning of line (according
 * to RFC1939) Compare also org.apache.james.smtpserver.SMTPInputStream
 */
public class ExtraDotOutputStream extends CRLFOutputStream {
	/**
	 * Constructor that wraps an OutputStream.
	 *
	 * @param out the OutputStream to be wrapped
	 */
	public ExtraDotOutputStream(final OutputStream out) {
		super(out);
	}

	/**
	 * Overrides super writeChunk in order to add a "." if the previous chunk ended
	 * with a new line and a new chunk starts with "."
	 *
	 * @see CRLFOutputStream#writeChunk(byte[], int, int)
	 */
	@Override
	protected void writeChunk(final byte[] buffer, final int offset, final int length) throws IOException {
		if (length > 0 && buffer[offset] == '.' && this.startOfLine) {
			// add extra dot (the first of the pair)
			this.out.write('.');
		}
		super.writeChunk(buffer, offset, length);
	}

	/**
	 * Writes a byte to the stream, adding dots where appropriate. Also fixes any
	 * naked CR or LF to the RFC 2821 mandated CRLF pairing.
	 *
	 * @param b the byte to write
	 *
	 * @throws IOException if an error occurs writing the byte
	 */
	@Override
	public void write(final int b) throws IOException {
		if (b == '.' && this.statusLast != LAST_WAS_OTHER) {
			// add extra dot (the first of the pair)
			this.out.write('.');
		}
		super.write(b);
	}
}
