package de.gessnerfl.fakesmtp.server.smtp.util;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Jeff Schnitzer
 */
public class TextUtils {
	/**
	 * @return a delimited string containing the specified items
	 */
	public static String joinTogether(final Collection<String> items, final String delim) {
		final StringBuffer ret = new StringBuffer();

		for (final Iterator<String> it = items.iterator(); it.hasNext();) {
			ret.append(it.next());
			if (it.hasNext()) {
				ret.append(delim);
			}
		}

		return ret.toString();
	}

	/**
	 * @return the value of str.getBytes() without the idiotic checked exception
	 */
	public static byte[] getBytes(final String str, final String charset) {
		try {
			return str.getBytes(charset);
		} catch (final UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/** @return the string as US-ASCII bytes */
	public static byte[] getAsciiBytes(final String str) {
		return getBytes(str, "US-ASCII");
	}

	/** @return the string as UTF-8 bytes */
	public static byte[] getUtf8Bytes(final String str) {
		return getBytes(str, "UTF-8");
	}

	/**
	 * Converts the specified byte array to a string using the specified encoding
	 * but without throwing a checked exception. This is useful if the specified
	 * encoding is required to be available by the JRE specification, so the
	 * exception would be guaranteed to be not thrown anyway.
	 */
	private static String getString(final byte[] bytes, final String charset) {
		try {
			return new String(bytes, charset);
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Converts the specified bytes to String using US-ASCII encoding.
	 */
	public static String getStringAscii(final byte[] bytes) {
		return getString(bytes, "US-ASCII");
	}

	/**
	 * Converts the specified bytes to String using UTF-8 encoding.
	 */
	public static String getStringUtf8(final byte[] bytes) {
		return getString(bytes, "UTF-8");
	}
}
