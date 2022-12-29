package de.gessnerfl.fakesmtp.smtp.io;

import java.io.FilterReader;

/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation. * All rights
 * reserved. *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you * may not
 * use this file except in compliance with the License. You * may obtain a copy
 * of the License at: * * http://www.apache.org/licenses/LICENSE-2.0 * * Unless
 * required by applicable law or agreed to in writing, software * distributed
 * under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or * implied. See the License for the
 * specific language governing * permissions and limitations under the License.
 * *
 ***********************************************************************/

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * A Reader for use with SMTP or other protocols in which lines must end with
 * CRLF. Extends Reader and overrides its readLine() method. The Reader
 * readLine() method cannot serve for SMTP because it ends lines with either CR
 * or LF alone.
 *
 * JSS: The readline() method of this class has been 'enchanced' from the Apache
 * JAMES version to throw an IOException if the line is greater than or equal to
 * MAX_LINE_LENGTH (998) which is defined in
 * <a href="http://rfc.net/rfc2822.html#s2.1.1.">RFC 2822</a>.
 */
public class CRLFTerminatedReader extends FilterReader {
	private static final int MAX_LINE_LENGTH = 998;
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
		/*
		 * This boolean tells which state we are in, depending upon whether or not we
		 * got a CR in the preceding read().
		 */
		boolean crJustReceived = false;
		/* If not -1 this int tells us where the first "wrong" line break is */
		int tainted = -1;
		final StringBuilder lineBuilder = new StringBuilder();

		while (true) {
			final int inChar = this.read();

			if (!crJustReceived) {
				// the most common case, somewhere before the end of a line
				switch (inChar) {
				case CR:
					crJustReceived = true;
					break;
				case EOF:
					return null; // premature EOF -- discards data(?)
				case LF: // the normal ending of a line
					if (tainted == -1) {
						tainted = lineBuilder.length();
					}
					// intentional fall-through
				default:
					lineBuilder.append((char) inChar);
				}
			} else {
				// CR has been received, we may be at end of line
				switch (inChar) {
				case LF: // LF without a preceding CR
					if (tainted != -1) {
						throw new TerminationException("\"bare\" CR or LF in data stream", tainted);
					}
					return lineBuilder.toString();
				case EOF:
					return null; // premature EOF -- discards data(?)
				case CR: // we got two (or more) CRs in a row
					if (tainted == -1) {
						tainted = lineBuilder.length();
					}
					lineBuilder.append(CR);
					break;
				default: // we got some other character following a CR
					if (tainted == -1) {
						tainted = lineBuilder.length();
					}
					lineBuilder.append(CR);
					lineBuilder.append((char) inChar);
					crJustReceived = false;
				}
			}
			if (lineBuilder.length() >= MAX_LINE_LENGTH) {
				throw new MaxLineLengthException("Input line length is too long!");
			}
		}
	}

}
