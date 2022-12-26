package de.gessnerfl.fakesmtp.server.smtp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A crude telnet client that can be used to send SMTP messages and test the
 * responses.
 */
public class Client {
	Socket socket;

	BufferedReader reader;

	PrintWriter writer;

	/**
	 * Establishes a connection to host and port.
	 *
	 * @param host the SMTP host
	 * @param port the SMTP port
	 * @throws IOException          on IO error
	 * @throws UnknownHostException on unknown host
	 */
	public Client(final String host, final int port) throws UnknownHostException, IOException {
		this.socket = new Socket(host, port);
		this.writer = new PrintWriter(this.socket.getOutputStream(), true);
		this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
	}

	/**
	 * Sends a message to the server, ie "HELO foo.example.com". A newline will be
	 * appended to the message.
	 *
	 * @param msg the message to send
	 * @throws Exception an exception if the method cannot send for any reason
	 */
	public void send(final String msg) throws Exception {
		// Force \r\n since println() behaves differently on different platforms
		this.writer.print(msg + "\r\n");
		this.writer.flush();
	}

	/**
	 * Throws an exception if the response does not start with the specified string.
	 *
	 * @param expect the expected response
	 * @throws Exception on error
	 */
	public void expect(final String expect) throws Exception {
		final String response = this.readResponse();
		if (!response.startsWith(expect)) {
			throw new Exception("Got: " + response + " Expected: " + expect);
		}
	}

	/**
	 * Throws an exception if the response does not contain the specified string.
	 *
	 * @param expect a part of the expected response
	 * @throws Exception on error
	 */
	public void expectContains(final String expect) throws Exception {
		final String response = this.readResponse();
		if (!response.contains(expect)) {
			throw new Exception("Got: " + response + " Expected to contain: " + expect);
		}
	}

	/**
	 * Get the complete response, including a multiline response. Newlines are
	 * included.
	 *
	 * @return the response
	 * @throws Exception on error
	 */
	protected String readResponse() throws Exception {
		final StringBuilder builder = new StringBuilder();
		boolean done = false;
		while (!done) {
			final String line = this.reader.readLine();
			if (line.charAt(3) != '-') {
				done = true;
			}

			builder.append(line);
			builder.append('\n');
		}

		return builder.toString();
	}

	public void close() throws Exception {
		if (!this.socket.isClosed()) {
			this.socket.close();
		}
	}
}
