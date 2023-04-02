package de.gessnerfl.fakesmtp.smtp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

import de.gessnerfl.fakesmtp.smtp.server.SmtpServer;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ActiveProfiles({"integrationtest", "default"})
@ExtendWith(SpringExtension.class)
@SpringBootTest
class MessageContentTest {

	@Autowired
	private SmtpServer smtpServer;
	@Autowired
	private StoringMessageListener storingMessageListener;
	private Session session;

	@BeforeEach
	void setUp() {
		final Properties props = new Properties();
		props.setProperty("mail.smtp.host", "localhost");
		props.setProperty("mail.smtp.port", Integer.toString(smtpServer.getPort()));
		session = Session.getInstance(props);
		storingMessageListener.reset();
	}

	@AfterEach
	void cleanup(){
		storingMessageListener.reset();
	}

	@Test
	void testReceivedHeader() throws Exception {
		final MimeMessage message = new MimeMessage(this.session);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
		message.setFrom(new InternetAddress("someone@somewhereelse.com"));
		message.setSubject("barf");
		message.setText("body");

		Transport.send(message);

		assertEquals(1, storingMessageListener.getMessages().size());
		final String[] receivedHeaders = storingMessageListener.getMessages().get(0).getMimeMessage().getHeader("Received");

		assertEquals(1, receivedHeaders.length);
	}

	@ParameterizedTest
	@CsvSource(value = {
			"\u00a4uro ma\u00f1ana;UTF-8",
			"ma\u00f1ana;ISO-8859-1",
			"\u3042\u3044\u3046\u3048\u304a;iso-2022-jp",
			"\u0080uro;ISO-8859-15"
	}, delimiter = ';')
	void testEightBitMessage(String inputBody, String charset) throws Exception {
		final String body = inputBody + "\r\n";
		final MimeMessage message = new MimeMessage(this.session);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
		message.setFrom(new InternetAddress("someone@somewhereelse.com"));
		message.setSubject("hello");
		message.setText(body, charset);
		message.setHeader("Content-Transfer-Encoding", "8bit");

		Transport.send(message);

		assertEquals(body, storingMessageListener.getMessages().get(0).getMimeMessage().getContent());
	}

	@Test
	void testMultipleRecipients() throws Exception {
		final MimeMessage message = new MimeMessage(this.session);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone2@anywhere.com"));
		message.setFrom(new InternetAddress("someone@somewhereelse.com"));
		message.setSubject("barf");
		message.setText("body");

		Transport.send(message);

		assertEquals(2, storingMessageListener.getMessages().size());
	}

	@Test
	void testLargeMessage() throws Exception {
		final MimeMessage message = new MimeMessage(this.session);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone2@anywhere.com"));
		message.setFrom(new InternetAddress("someone@somewhereelse.com"));
		message.setSubject("barf");
		message.setText("bodyalksdjflkasldfkasjldfkjalskdfjlaskjdflaksdjflkjasdlfkjl");

		Transport.send(message);

		assertEquals(2, storingMessageListener.getMessages().size());
		assertEquals("barf", storingMessageListener.getMessages().get(0).getMimeMessage().getSubject());
		assertEquals("barf", storingMessageListener.getMessages().get(1).getMimeMessage().getSubject());
	}

	@Test
	void testBinaryEightBitMessage() throws Exception {
		final byte[] body = new byte[64];
		new Random().nextBytes(body);

		final MimeMessage message = new MimeMessage(this.session);
		message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
		message.setFrom(new InternetAddress("someone@somewhereelse.com"));
		message.setSubject("hello");
		message.setHeader("Content-Transfer-Encoding", "8bit");
		message.setDataHandler(new DataHandler(new ByteArrayDataSource(body, "application/octet-stream")));

		Transport.send(message);

		final InputStream in = storingMessageListener.getMessages().get(0).getMimeMessage().getInputStream();
		final ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		final byte[] buf = new byte[64];
		int n;
		while ((n = in.read(buf)) != -1) {
			tmp.write(buf, 0, n);
		}
		in.close();

		assertArrayEquals(body, tmp.toByteArray());
	}
}
