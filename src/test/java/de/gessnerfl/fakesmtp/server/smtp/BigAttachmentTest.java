package de.gessnerfl.fakesmtp.server.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.smtp.SMTPTransport;

/**
 * This class tests the transfer speed of emails that carry attached files.
 *
 * @author De Oliveira Edouard &lt;doe_wanted@yahoo.fr&gt;
 */
@Disabled("requires manual setup")
public class BigAttachmentTest {
	private final static Logger log = LoggerFactory.getLogger(BigAttachmentTest.class);

	private final static int SMTP_PORT = 1081;

	private final static String TO_CHANGE = "<path>/<your_bigfile.ext>";

	private final static int BUFFER_SIZE = 32768;

	// Set the full path name of the big file to use for the test.
	private final static String BIGFILE_PATH = TO_CHANGE;

	private Wiser server;

	@BeforeEach
	protected void setUp() throws Exception {
		this.server = new Wiser();
		this.server.setPort(SMTP_PORT);
		this.server.start();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		try {
			this.server.stop();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void testAttachments() throws Exception {
		if (BIGFILE_PATH.equals(TO_CHANGE)) {
			log.error(
					"BigAttachmentTest: To complete this test you must change the BIGFILE_PATH var to point out a file on your disk !");
		}
		assertNotSame(
				"BigAttachmentTest: To complete this test you must change the BIGFILE_PATH var to point out a file on your disk !",
				TO_CHANGE,
				BIGFILE_PATH);
		final Properties props = System.getProperties();
		props.setProperty("mail.smtp.host", "localhost");
		props.setProperty("mail.smtp.port", SMTP_PORT + "");
		final Session session = Session.getInstance(props);

		final MimeMessage baseMsg = new MimeMessage(session);
		final MimeBodyPart bp1 = new MimeBodyPart();
		bp1.setHeader("Content-Type", "text/plain");
		bp1.setContent("Hello World!!!", "text/plain; charset=\"ISO-8859-1\"");

		// Attach the file
		final MimeBodyPart bp2 = new MimeBodyPart();
		final FileDataSource fileAttachment = new FileDataSource(BIGFILE_PATH);
		final DataHandler dh = new DataHandler(fileAttachment);
		bp2.setDataHandler(dh);
		bp2.setFileName(fileAttachment.getName());

		final Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(bp1);
		multipart.addBodyPart(bp2);

		baseMsg.setFrom(new InternetAddress("Ted <ted@home.com>"));
		baseMsg.setRecipient(Message.RecipientType.TO, new InternetAddress("success@subethamail.org"));
		baseMsg.setSubject("Test Big attached file message");
		baseMsg.setContent(multipart);
		baseMsg.saveChanges();

		log.debug("Send started");
		final Transport t = new SMTPTransport(session, new URLName("smtp://localhost:" + SMTP_PORT));
		long started = System.currentTimeMillis();
		t.connect();
		t.sendMessage(baseMsg, new Address[] { new InternetAddress("success@subethamail.org") });
		t.close();
		started = System.currentTimeMillis() - started;
		log.info("Elapsed ms = " + started);

		final WiserMessage msg = this.server.getMessages().get(0);

		assertEquals(1, this.server.getMessages().size());
		assertEquals("success@subethamail.org", msg.getEnvelopeReceiver());

		final File compareFile = File.createTempFile("attached", ".tmp");
		log.debug("Writing received attachment ...");

		final FileOutputStream fos = new FileOutputStream(compareFile);
		((MimeMultipart) msg.getMimeMessage().getContent()).getBodyPart(1).getDataHandler().writeTo(fos);
		fos.close();
		log.debug("Checking integrity ...");
		assertTrue(this.checkIntegrity(new File(BIGFILE_PATH), compareFile));
		log.debug("Checking integrity DONE");
		compareFile.delete();
	}

	private boolean checkIntegrity(final File src, final File dest) throws IOException, NoSuchAlgorithmException {
		final BufferedInputStream ins = new BufferedInputStream(new FileInputStream(src));
		final BufferedInputStream ind = new BufferedInputStream(new FileInputStream(dest));
		final MessageDigest md1 = MessageDigest.getInstance("MD5");
		final MessageDigest md2 = MessageDigest.getInstance("MD5");

		int r = 0;
		final byte[] buf1 = new byte[BUFFER_SIZE];
		final byte[] buf2 = new byte[BUFFER_SIZE];

		while (r != -1) {
			r = ins.read(buf1);
			ind.read(buf2);

			md1.update(buf1);
			md2.update(buf2);
		}

		ins.close();
		ind.close();
		return MessageDigest.isEqual(md1.digest(), md2.digest());
	}
}
