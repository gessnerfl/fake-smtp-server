/*
 * $Id: RejectException.java 337 2009-06-29 19:20:58Z latchkey $ $Source:
 * /cvsroot/Similarity4/src/java/com/similarity/mbean/BindStatisticsManagerMBean
 * .java,v $
 */
package de.gessnerfl.fakesmtp.server.smtp;

/**
 * A type of RejectException that additionally causes the server to close the
 * connection to the client.
 *
 * @author Jeff Schnitzer
 */
@SuppressWarnings("serial")
public class DropConnectionException extends RejectException {
	int code;

	public DropConnectionException() {}

	public DropConnectionException(final String message) {
		super(message);
	}

	public DropConnectionException(final int code, final String message) {
		super(code, message);
	}
}
