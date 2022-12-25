/*
 * $Id$ $Source:
 * /cvsroot/Similarity4/src/java/com/similarity/mbean/BindStatisticsManagerMBean
 * .java,v $
 */
package de.gessnerfl.fakesmtp.server.smtp;

import java.io.IOException;

/**
 * Thrown by message listeners if an input stream provides more data than the
 * listener can handle.
 *
 * @author Jeff Schnitzer
 */
@SuppressWarnings("serial")
public class TooMuchDataException extends IOException {
	public TooMuchDataException() {}

	public TooMuchDataException(final String message) {
		super(message);
	}
}
