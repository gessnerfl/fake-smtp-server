package de.gessnerfl.fakesmtp.server.smtp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A somewhat smarter abstraction of an SMTP client which doesn't require
 * knowing anything about the nitty gritty of SMTP.
 *
 * @author Jeff Schnitzer
 */
public class SmartClient extends SMTPClient {
	private static Logger log = LoggerFactory.getLogger(SmartClient.class);

	boolean sentFrom;

	int recipientCount;

	/** The host name which is sent in the HELO and EHLO commands */
	private String heloHost;

	/**
	 * True if the server sent a 421 "Service not available, closing transmission
	 * channel" response. In this case the QUIT command should not be sent.
	 */
	private boolean serverClosingTransmissionChannel = false;

	/**
	 * SMTP extensions supported by the server, and their parameters as the server
	 * specified it in response to the EHLO command. Key is the extension keyword in
	 * upper case, like "AUTH", value is the extension parameters string in unparsed
	 * form. If the server does not support EHLO, then this map is empty.
	 */
	private final Map<String, String> extensions = new HashMap<>();

	/**
	 * If supplied (not null), then it will be called after EHLO, to authenticate
	 * this client to the server.
	 */
	private Authenticator authenticator = null;

	/**
	 * Creates an unconnected client.
	 */
	public SmartClient() {
		// nothing to do
	}

	/**
	 * Connects to the specified server and issues the initial HELO command.
	 *
	 * @throws UnknownHostException if problem looking up hostname
	 * @throws SMTPException        if problem reported by the server
	 * @throws IOException          if problem communicating with host
	 */
	public SmartClient(final String host, final int port, final String myHost)
			throws UnknownHostException, IOException, SMTPException {
		this(host, port, null, myHost);
	}

	/**
	 * Connects to the specified server and issues the initial HELO command.
	 *
	 * @throws UnknownHostException if problem looking up hostname
	 * @throws SMTPException        if problem reported by the server
	 * @throws IOException          if problem communicating with host
	 */
	public SmartClient(final String host, final int port, final SocketAddress bindpoint, final String myHost)
			throws UnknownHostException, IOException, SMTPException {
		this.setBindpoint(bindpoint);
		this.setHeloHost(myHost);
		this.connect(host, port);
	}

	/**
	 * Connects to the specified server and issues the initial HELO command. It
	 * gracefully closes the connection if it could be established but subsequently
	 * it fails or if the server does not accept messages.
	 */
	@Override
	public void connect(final String host, final int port)
			throws SMTPException, AuthenticationNotSupportedException, IOException {
		if (heloHost == null) {
			throw new IllegalStateException("Helo host must be specified before connecting");
		}

		super.connect(host, port);
		try {
			this.receiveAndCheck(); // The server announces itself first
			this.sendHeloOrEhlo();
			if (this.authenticator != null) {
				this.authenticator.authenticate();
			}
		} catch (final SMTPException | AuthenticationNotSupportedException e) {
			this.quit();
			throw e;
		} catch (final IOException e) {
			this.close(); // just close the socket, issuing QUIT is hopeless now
			throw e;
		}
	}

	/**
	 * Sends the EHLO command, or HELO if EHLO is not supported, and saves the list
	 * of SMTP extensions which are supported by the server.
	 */
	protected void sendHeloOrEhlo() throws IOException, SMTPException {
		extensions.clear();
		final Response resp = this.sendReceive("EHLO " + heloHost);
		if (resp.isSuccess()) {
			parseEhloResponse(resp);
		} else if (resp.getCode() == 500 || resp.getCode() == 502) {
			// server does not support EHLO, try HELO
			this.sendAndCheck("HELO " + heloHost);
		} else {
			// some serious error
			throw new SMTPException(resp);
		}
	}

	/**
	 * Extracts the list of SMTP extensions from the server's response to EHLO, and
	 * stores them in {@link #extensions}.
	 */
	private void parseEhloResponse(final Response resp) throws IOException {
		final BufferedReader reader = new BufferedReader(new StringReader(resp.getMessage()));
		//the first line is skipped as it contains server information only
		reader.lines().skip(1).forEach(line -> {
			final int iFirstSpace = line.indexOf(' ');
			final String keyword = iFirstSpace == -1 ? line : line.substring(0, iFirstSpace);
			final String parameters = iFirstSpace == -1 ? "" : line.substring(iFirstSpace + 1);
			extensions.put(keyword.toUpperCase(Locale.ENGLISH), parameters);
		});
	}

	/**
	 * Returns the server response. It takes note of a 421 response code, so QUIT
	 * will not be issued unnecessarily.
	 */
	@Override
	protected Response receive() throws IOException {
		final Response response = super.receive();
		if (response.getCode() == 421) {
			serverClosingTransmissionChannel = true;
		}
		return response;
	}

	public void from(final String from) throws IOException, SMTPException {
		this.sendAndCheck("MAIL FROM: <" + from + ">");
		this.sentFrom = true;
	}

	public void to(final String to) throws IOException, SMTPException {
		this.sendAndCheck("RCPT TO: <" + to + ">");
		this.recipientCount++;
	}

	/**
	 * Prelude to writing data
	 */
	public void dataStart() throws IOException, SMTPException {
		this.sendAndCheck("DATA");
	}

	/**
	 * Actually write some data
	 */
	public void dataWrite(final byte[] data, final int numBytes) throws IOException {
		this.dataOutput.write(data, 0, numBytes);
	}

	/**
	 * Last step after writing data
	 */
	public void dataEnd() throws IOException, SMTPException {
		this.dataOutput.flush();
		this.dotTerminatedOutput.writeTerminatingSequence();
		this.dotTerminatedOutput.flush();

		this.receiveAndCheck();
	}

	/**
	 * Quit and close down the connection. Ignore any errors.
	 * <p>
	 * It still closes the connection, but it does not send the QUIT command if a
	 * 421 Service closing transmission channel is received previously. In these
	 * cases QUIT would fail anyway.
	 *
	 * @see <a href="http://tools.ietf.org/html/rfc5321#section-3.8">RFC 5321
	 *      Terminating Sessions and Connections</a>
	 */
	public void quit() {
		try {
			if (this.isConnected() && !this.serverClosingTransmissionChannel) {
				this.sendAndCheck("QUIT");
			}
		} catch (final IOException ex) {
			log.warn("Failed to issue QUIT to " + this.hostPort);
		}

		this.close();
	}

	/**
	 * @return true if we have already specified from()
	 */
	public boolean sentFrom() {
		return this.sentFrom;
	}

	/**
	 * @return true if we have already specified to()
	 */
	public boolean sentTo() {
		return this.recipientCount > 0;
	}

	/**
	 * @return the number of recipients that have been accepted by the server
	 */
	public int getRecipientCount() {
		return this.recipientCount;
	}

	/**
	 * Returns the SMTP extensions supported by the server.
	 *
	 * @return the extension map. Key is the extension keyword in upper case, value
	 *         is the unparsed string of extension parameters.
	 */
	public Map<String, String> getExtensions() {
		return extensions;
	}

	/**
	 * Sets the domain name or address literal of this system, which name will be
	 * sent to the server in the parameter of the HELO and EHLO commands. This has
	 * no default and is required.
	 */
	public void setHeloHost(final String myHost) {
		this.heloHost = myHost;
	}

	/**
	 * Returns the HELO name of this system.
	 */
	public String getHeloHost() {
		return heloHost;
	}

	/**
	 * Returns the Authenticator object, which is used to authenticate this client
	 * to the server, or null, if no authentication is required.
	 */
	public Authenticator getAuthenticator() {
		return authenticator;
	}

	/**
	 * Sets the Authenticator object which will be called after the EHLO command to
	 * authenticate this client to the server. The default is that no authentication
	 * will happen.
	 */
	public void setAuthenticator(final Authenticator authenticator) {
		this.authenticator = authenticator;
	}
}
