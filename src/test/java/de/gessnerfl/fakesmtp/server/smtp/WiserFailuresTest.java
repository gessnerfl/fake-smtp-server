package de.gessnerfl.fakesmtp.server.smtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * This class tests various aspects of the server for smtp compliance by using
 * Wiser
 */
class WiserFailuresTest {
	private final static String FROM_ADDRESS = "from-addr@localhost";

	private final static String HOST_NAME = "localhost";

	private final static String TO_ADDRESS = "to-addr@localhost";

	private final static int SMTP_PORT = 1081;

	private static final Logger LOGGER = LoggerFactory.getLogger(WiserFailuresTest.class);

	private BufferedReader input;

	private PrintWriter output;

	private Wiser server;

	private Socket socket;

	@BeforeEach
	void setUp() throws Exception {
		this.server = new Wiser();
		this.server.setPort(SMTP_PORT);
		this.server.start();
		this.socket = new Socket(HOST_NAME, SMTP_PORT);
		this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		this.output = new PrintWriter(this.socket.getOutputStream(), true);
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			this.input.close();
		} catch (final Exception e) {}
		try {
			this.output.close();
		} catch (final Exception e) {}
		try {
			this.socket.close();
		} catch (final Exception e) {}
		try {
			this.server.stop();
		} catch (final Exception e) {}
	}

	/**
	 * See
	 * http://sourceforge.net/tracker/index.php?func=detail&amp;aid=1474700&amp;group_id=78413&amp;atid=553186
	 * for discussion about this bug
	 *
	 * @throws IOException        on IO error
	 * @throws MessagingException on messaging error
	 */
	@Test
	void testMailFromAfterReset() throws IOException, MessagingException {
		LOGGER.info("testMailFromAfterReset() start");

		this.assertConnect();
		this.sendExtendedHello(HOST_NAME);
		this.sendMailFrom(FROM_ADDRESS);
		this.sendReceiptTo(TO_ADDRESS);
		this.sendReset();
		this.sendMailFrom(FROM_ADDRESS);
		this.sendReceiptTo(TO_ADDRESS);
		this.sendDataStart();
		this.send("");
		this.send("Body");
		this.sendDataEnd();
		this.sendQuit();

		assertEquals(1, this.server.getMessages().size());
		final Iterator<WiserMessage> emailIter = this.server.getMessages().iterator();
		final WiserMessage email = emailIter.next();
		assertEquals("Body" + "\r\n", email.getMimeMessage().getContent().toString());
	}

	/**
	 * See
	 * http://sourceforge.net/tracker/index.php?func=detail&amp;aid=1474700&amp;group_id=78413&amp;atid=553186
	 * for discussion about this bug
	 *
	 * @throws IOException        on IO error
	 * @throws MessagingException on messaging error
	 */
	@Test
	void testMailFromWithInitialReset() throws IOException, MessagingException {
		this.assertConnect();
		this.sendReset();
		this.sendMailFrom(FROM_ADDRESS);
		this.sendReceiptTo(TO_ADDRESS);
		this.sendDataStart();
		this.send("");
		this.send("Body");
		this.sendDataEnd();
		this.sendQuit();

		assertEquals(1, this.server.getMessages().size());
		final Iterator<WiserMessage> emailIter = this.server.getMessages().iterator();
		final WiserMessage email = emailIter.next();
		assertEquals("Body" + "\r\n", email.getMimeMessage().getContent().toString());
	}

	@Test
	void testSendEncodedMessage() throws IOException, MessagingException {
		final String body = "\u3042\u3044\u3046\u3048\u304a"; // some Japanese letters
		final String charset = "iso-2022-jp";

		try {
			this.sendMessageWithCharset(SMTP_PORT,
					"sender@hereagain.com",
					"EncodedMessage",
					body,
					"receivingagain@there.com",
					charset);
		} catch (final Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}

		assertEquals(1, this.server.getMessages().size());
		final Iterator<WiserMessage> emailIter = this.server.getMessages().iterator();
		final WiserMessage email = emailIter.next();
		assertEquals(body + "\r\n", email.getMimeMessage().getContent().toString());
	}

	@Test
	void testSendMessageWithCarriageReturn() throws IOException, MessagingException {
		final String bodyWithCR = "\r\n\r\nKeep these\r\npesky\r\n\r\ncarriage returns\r\n";
		try {
			this.sendMessage(SMTP_PORT, "sender@hereagain.com", "CRTest", bodyWithCR, "receivingagain@there.com");
		} catch (final Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}

		assertEquals(1, this.server.getMessages().size());
		final Iterator<WiserMessage> emailIter = this.server.getMessages().iterator();
		final WiserMessage email = emailIter.next();
		assertEquals(email.getMimeMessage().getContent().toString(), bodyWithCR);
	}

	@Test
	void testSendTwoMessagesSameConnection() throws IOException {
		try {
			final MimeMessage[] mimeMessages = new MimeMessage[2];
			final Properties mailProps = this.getMailProperties(SMTP_PORT);
			final Session session = Session.getInstance(mailProps, null);
			// session.setDebug(true);

			mimeMessages[0]
					= this.createMessage(session, "sender@whatever.com", "receiver@home.com", "Doodle1", "Bug1");
			mimeMessages[1]
					= this.createMessage(session, "sender@whatever.com", "receiver@home.com", "Doodle2", "Bug2");

			final Transport transport = session.getTransport("smtp");
			transport.connect("localhost", SMTP_PORT, null, null);

			for (final MimeMessage mimeMessage : mimeMessages) {
				transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
			}

			transport.close();
		} catch (final MessagingException e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}

		assertEquals(2, this.server.getMessages().size());
	}

	@Test
	void testSendTwoMsgsWithLogin() throws MessagingException, IOException {
		try {
			final String From = "sender@here.com";
			final String To = "receiver@there.com";
			final String Subject = "Test";
			final String body = "Test Body";

			final Session session = Session.getInstance(this.getMailProperties(SMTP_PORT), null);
			final Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(From));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(To, false));
			msg.setSubject(Subject);

			msg.setText(body);
			msg.setHeader("X-Mailer", "musala");
			msg.setSentDate(new Date());

			Transport transport = null;

			try {
				transport = session.getTransport("smtp");
				transport.connect(HOST_NAME, SMTP_PORT, "ddd", "ddd");
				assertEquals(0, this.server.getMessages().size());
				transport.sendMessage(msg, InternetAddress.parse(To, false));
				assertEquals(1, this.server.getMessages().size());
				transport.sendMessage(msg, InternetAddress.parse("dimiter.bakardjiev@musala.com", false));
				assertEquals(2, this.server.getMessages().size());
			} catch (final Exception e) {
				e.printStackTrace();
			} finally {
				if (transport != null) {
					transport.close();
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final Iterator<WiserMessage> emailIter = this.server.getMessages().iterator();
		final WiserMessage email = emailIter.next();
		final MimeMessage mime = email.getMimeMessage();
		assertEquals("Test", mime.getHeader("Subject")[0]);
		assertEquals("Test Body" + "\r\n", mime.getContent().toString());
	}

	private Properties getMailProperties(final int port) {
		final Properties mailProps = new Properties();
		mailProps.setProperty("mail.smtp.host", "localhost");
		mailProps.setProperty("mail.smtp.port", "" + port);
		mailProps.setProperty("mail.smtp.sendpartial", "true");
		return mailProps;
	}

	private void sendMessage(final int port,
			final String from,
			final String subject,
			final String body,
			final String to) throws MessagingException, IOException {
		final Properties mailProps = this.getMailProperties(SMTP_PORT);
		final Session session = Session.getInstance(mailProps, null);
		// session.setDebug(true);

		final MimeMessage msg = this.createMessage(session, from, to, subject, body);
		Transport.send(msg);
	}

	private MimeMessage createMessage(final Session session,
			final String from,
			final String to,
			final String subject,
			final String body) throws MessagingException, IOException {
		final MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		msg.setSubject(subject);
		msg.setSentDate(new Date());
		msg.setText(body);
		msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
		return msg;
	}

	private void sendMessageWithCharset(final int port,
			final String from,
			final String subject,
			final String body,
			final String to,
			final String charset) throws MessagingException {
		final Properties mailProps = this.getMailProperties(port);
		final Session session = Session.getInstance(mailProps, null);
		// session.setDebug(true);

		final MimeMessage msg = this.createMessageWithCharset(session, from, to, subject, body, charset);
		Transport.send(msg);
	}

	private MimeMessage createMessageWithCharset(final Session session,
			final String from,
			final String to,
			final String subject,
			final String body,
			final String charset) throws MessagingException {
		final MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(from));
		msg.setSubject(subject);
		msg.setSentDate(new Date());
		if (charset != null) {
			msg.setText(body, charset);
			msg.setHeader("Content-Transfer-Encoding", "7bit");
		} else {
			msg.setText(body);
		}

		msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
		return msg;
	}

	private void assertConnect() throws IOException {
		final String response = this.readInput();
		assertTrue(response, response.startsWith("220"));
	}

	private void sendDataEnd() throws IOException {
		this.send(".");
		final String response = this.readInput();
		assertTrue(response, response.startsWith("250"));
	}

	private void sendDataStart() throws IOException {
		this.send("DATA");
		final String response = this.readInput();
		assertTrue(response, response.startsWith("354"));
	}

	private void sendExtendedHello(final String hostName) throws IOException {
		this.send("EHLO " + hostName);
		final String response = this.readInput();
		assertTrue(response, response.startsWith("250"));
	}

	private void sendMailFrom(final String fromAddress) throws IOException {
		this.send("MAIL FROM:<" + fromAddress + ">");
		final String response = this.readInput();
		assertTrue(response, response.startsWith("250"));
	}

	private void sendQuit() throws IOException {
		this.send("QUIT");
		final String response = this.readInput();
		assertTrue(response, response.startsWith("221"));
	}

	private void sendReceiptTo(final String toAddress) throws IOException {
		this.send("RCPT TO:<" + toAddress + ">");
		final String response = this.readInput();
		assertTrue(response, response.startsWith("250"));
	}

	private void sendReset() throws IOException {
		this.send("RSET");
		final String response = this.readInput();
		assertTrue(response, response.startsWith("250"));
	}

	private void send(final String msg) throws IOException {
		// Force \r\n since println() behaves differently on different platforms
		this.output.print(msg + "\r\n");
		this.output.flush();
	}

	private String readInput() {
		final StringBuffer sb = new StringBuffer();
		try {
			do {
				sb.append(this.input.readLine()).append("\n");
			} while (this.input.ready());
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}
}
