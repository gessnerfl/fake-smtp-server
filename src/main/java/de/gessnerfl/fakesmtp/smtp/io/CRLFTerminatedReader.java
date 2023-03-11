package de.gessnerfl.fakesmtp.smtp.io;

import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * A Reader for use with SMTP or other protocols in which lines must end with
 * CRLF. Extends Reader and overrides its readLine() method. The Reader
 * readLine() method cannot serve for SMTP because it ends lines with either CR
 * or LF alone.
 * JSS: The readline() method of this class has been 'enhanced' from the Apache
 * JAMES version to throw an IOException if the line is greater than or equal to
 * MAX_LINE_LENGTH (998) which is defined in
 * <a href="http://rfc.net/rfc2822.html#s2.1.1.">RFC 2822</a>.
 */
public class CRLFTerminatedReader extends FilterReader {
	static final int MAX_LINE_LENGTH = 998;
	private static final int EOF = -1;
	private static final char CR = 13;
	private static final char LF = 10;

	public static class TerminationException extends IOException {
		private final int where;

		public TerminationException(final String s, final int where) {
			super(s);
			this.where = where;
		}

		public int position() {
			return this.where;
		}
	}

	public static class MaxLineLengthException extends IOException {
		public MaxLineLengthException(final String s) {
			super(s);
		}
	}

	/**
	 * Constructs this CRLFTerminatedReader.
	 *
	 * @param in      an InputStream
	 * @param charset the {@link Charset} to use
	 */
	public CRLFTerminatedReader(final InputStream in, final Charset charset) {
		super(new InputStreamReader(in, charset));
	}

	/**
	 * Read a line of text which is terminated by CRLF. The concluding CRLF
	 * characters are not returned with the String, but if either CR or LF appears
	 * in the text in any other sequence it is returned in the String like any other
	 * character. Some characters at the end of the stream may be lost if they are
	 * in a "line" not terminated by CRLF.
	 *
	 * @return either a String containing the contents of a line which must end with
	 *         CRLF, or null if the end of the stream has been reached, possibly
	 *         discarding some characters in a line not terminated with CRLF.
	 * @throws IOException if an I/O error occurs.
	 */
	public String readLine() throws IOException {
		final var state = new ReadState();
		while (true) {
			final int inChar = this.read();

			processCharacter(state, inChar);
			if (state.isPrematureEol){
				return null;
			}
			if (state.isComplete){
				return state.lineBuilder.toString();
			}
			if (state.lineBuilder.length() >= MAX_LINE_LENGTH) {
				throw new MaxLineLengthException("Input line length is too long!");
			}
		}
	}

	private static void processCharacter(ReadState state, int inChar) throws TerminationException {
		if (!state.crJustReceived) {
			processCharacterBeforeCarriageReturn(state, inChar);
		} else {
			processCharacterAfterCarriageReturn(state, inChar);
		}
	}

	private static void processCharacterBeforeCarriageReturn(ReadState state, int inChar) {
		// the most common case, somewhere before the end of a line
		switch (inChar) {
		case CR:
			state.crJustReceived = true;
			break;
		case EOF:
			state.isPrematureEol = true;
			break;
		case LF: // the normal ending of a line
			if (state.tainted == -1) {
				state.tainted = state.lineBuilder.length();
			}
			// intentional fall-through
		default:
			state.lineBuilder.append((char) inChar);
		}
	}

	private static void processCharacterAfterCarriageReturn(ReadState state, int inChar) throws TerminationException {
		switch (inChar) {
			case LF -> { // LF without a preceding CR
				if (state.tainted != -1) {
					throw new TerminationException("\"bare\" CR or LF in data stream", state.tainted);
				}
				state.isComplete = true;
			}
			case EOF -> state.isPrematureEol = true;
			case CR -> { // we got two (or more) CRs in a row
				if (state.tainted == -1) {
					state.tainted = state.lineBuilder.length();
				}
				state.lineBuilder.append(CR);
			}
			default -> { // we got some other character following a CR
				if (state.tainted == -1) {
					state.tainted = state.lineBuilder.length();
				}
				state.lineBuilder.append(CR);
				state.lineBuilder.append((char) inChar);
				state.crJustReceived = false;
			}
		}
	}

	private static final class ReadState {
		/*
		 * This boolean tells which state we are in, depending upon whether we
		 * got a CR in the preceding read().
		 */
		boolean crJustReceived = false;
		/* If not -1 this int tells us where the first "wrong" line break is */
		int tainted = -1;
		final StringBuilder lineBuilder = new StringBuilder();
		boolean isPrematureEol = false;
		boolean isComplete = false;
	}
}
