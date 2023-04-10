package de.gessnerfl.fakesmtp.smtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import de.gessnerfl.fakesmtp.smtp.server.SmtpServer;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@Transactional
@ActiveProfiles({"integrationtest", "default"})
@ExtendWith(SpringExtension.class)
@SpringBootTest
class SmtpComplianceIntegrationTest {
    private final static String FROM_ADDRESS = "from-addr@localhost";

    private final static String HOST_NAME = "localhost";

    private final static String TO_ADDRESS = "to-addr@localhost";

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpComplianceIntegrationTest.class);
    @Autowired
    private SmtpServer smtpServer;
    @Autowired
    private StoringMessageListener storingMessageListener;
    private BufferedReader input;
    private PrintWriter output;
    private Socket socket;

    @BeforeEach
    void setUp() throws Exception {
        this.socket = new Socket(HOST_NAME, smtpServer.getPort());
        this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.output = new PrintWriter(this.socket.getOutputStream(), true);
        storingMessageListener.reset();
    }

    @AfterEach
    void tearDown() {
        storingMessageListener.reset();
        try {
            this.input.close();
        } catch (final Exception e) {
            //ignore exceptions to close other resources
        }
        try {
            this.output.close();
        } catch (final Exception e) {
            //ignore exceptions to close other resources
        }
        try {
            this.socket.close();
        } catch (final Exception e) {
            //ignore exceptions to close other resources
        }
    }

    /**
     * See
     * <a href="http://sourceforge.net/tracker/index.php?func=detail&amp;aid=1474700&amp;group_id=78413&amp;atid=553186">...</a>
     * for discussion about this bug
     *
     * @throws IOException        on IO error
     * @throws MessagingException on messaging error
     */
    @Test
    void testMailFromAfterReset() throws IOException, MessagingException {
        LOGGER.info("testMailFromAfterReset() start");

        this.assertConnect();
        this.sendExtendedHello();
        this.sendMailFrom();
        this.sendReceiptTo();
        this.sendReset();
        this.sendMailFrom();
        this.sendReceiptTo();
        this.sendDataStart();
        this.sendMessageId();
        this.send("");
        this.send("Body");
        this.sendDataEnd();
        this.sendQuit();

        assertEquals(1, storingMessageListener.getMessages().size());
        final Iterator<StoredMessage> emailIter = storingMessageListener.getMessages().iterator();
        final StoredMessage email = emailIter.next();
        assertEquals("Body" + "\r\n", email.getMimeMessage().getContent().toString());
    }

    /**
     * See
     * <a href="http://sourceforge.net/tracker/index.php?func=detail&amp;aid=1474700&amp;group_id=78413&amp;atid=553186">...</a>
     * for discussion about this bug
     *
     * @throws IOException        on IO error
     * @throws MessagingException on messaging error
     */
    @Test
    void testMailFromWithInitialReset() throws IOException, MessagingException {
        this.assertConnect();
        this.sendReset();
        this.sendMailFrom();
        this.sendReceiptTo();
        this.sendDataStart();
        this.sendMessageId();
        this.send("");
        this.send("Body");
        this.sendDataEnd();
        this.sendQuit();

        assertEquals(1, storingMessageListener.getMessages().size());
        final Iterator<StoredMessage> emailIter = storingMessageListener.getMessages().iterator();
        final StoredMessage email = emailIter.next();
        assertEquals("Body" + "\r\n", email.getMimeMessage().getContent().toString());
    }

    @Test
    void testSendEncodedMessage() throws Exception {
        final String body = "\u3042\u3044\u3046\u3048\u304a"; // some Japanese letters
        final String charset = "iso-2022-jp";

        this.sendMessageWithCharset("sender@hereagain.com", "EncodedMessage", body, "receivingagain@there.com", charset);

        assertEquals(1, storingMessageListener.getMessages().size());
        final Iterator<StoredMessage> emailIter = storingMessageListener.getMessages().iterator();
        final StoredMessage email = emailIter.next();
        assertEquals(body + "\r\n", email.getMimeMessage().getContent().toString());
    }

    @Test
    void testSendMessageWithCarriageReturn() throws Exception {
        final String bodyWithCR = "\r\n\r\nKeep these\r\npesky\r\n\r\ncarriage returns\r\n";
        this.sendMessage("sender@hereagain.com", "CRTest", bodyWithCR, "receivingagain@there.com");

        assertEquals(1, storingMessageListener.getMessages().size());
        final Iterator<StoredMessage> emailIter = storingMessageListener.getMessages().iterator();
        final StoredMessage email = emailIter.next();
        assertEquals(email.getMimeMessage().getContent().toString(), bodyWithCR);
    }

    @Test
    void testSendTwoMessagesSameConnection() throws Exception {
        final MimeMessage[] mimeMessages = new MimeMessage[2];
        final Properties mailProps = this.getMailProperties(smtpServer.getPort());
        final Session session = Session.getInstance(mailProps, null);
        mimeMessages[0] = this.createMessage(session, "sender@whatever.com", "receiver@home.com", "Doodle1", "Bug1");
        mimeMessages[1] = this.createMessage(session, "sender@whatever.com", "receiver@home.com", "Doodle2", "Bug2");
        final Transport transport = session.getTransport("smtp");
        transport.connect("localhost", smtpServer.getPort(), null, null);

        for (final MimeMessage mimeMessage : mimeMessages) {
            transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
        }

        transport.close();

        assertEquals(2, storingMessageListener.getMessages().size());
    }

    @Test
    void testSendTwoMsgsWithLogin() throws MessagingException, IOException {
        final String From = "sender@here.com";
        final String To = "receiver@there.com";
        final String Subject = "Test";
        final String body = "Test Body";

        final Session session = Session.getInstance(this.getMailProperties(smtpServer.getPort()), null);
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
            transport.connect(HOST_NAME, smtpServer.getPort(), "ddd", "ddd");
            assertEquals(0, storingMessageListener.getMessages().size());
            transport.sendMessage(msg, InternetAddress.parse(To, false));
            assertEquals(1, storingMessageListener.getMessages().size());
            transport.sendMessage(msg, InternetAddress.parse("dimiter.bakardjiev@musala.com", false));
            assertEquals(2, storingMessageListener.getMessages().size());
        } finally {
            if (transport != null) {
                transport.close();
            }
        }

        final Iterator<StoredMessage> emailIter = storingMessageListener.getMessages().iterator();
        final StoredMessage email = emailIter.next();
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

    private void sendMessage(final String from,
                             final String subject,
                             final String body,
                             final String to) throws MessagingException {
        final Properties mailProps = this.getMailProperties(smtpServer.getPort());
        final Session session = Session.getInstance(mailProps, null);
        final MimeMessage msg = this.createMessage(session, from, to, subject, body);
        Transport.send(msg);
    }

    private MimeMessage createMessage(final Session session,
                                      final String from,
                                      final String to,
                                      final String subject,
                                      final String body) throws MessagingException {
        final MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(body);
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        return msg;
    }

    private void sendMessageWithCharset(
            final String from,
            final String subject,
            final String body,
            final String to,
            final String charset) throws MessagingException {
        final Properties mailProps = this.getMailProperties(smtpServer.getPort());
        final Session session = Session.getInstance(mailProps, null);
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

    private void sendExtendedHello() throws IOException {
        this.send("EHLO " + SmtpComplianceIntegrationTest.HOST_NAME);
        final String response = this.readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void sendMailFrom() throws IOException {
        this.send("MAIL FROM:<" + SmtpComplianceIntegrationTest.FROM_ADDRESS + ">");
        final String response = this.readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void sendMessageId() throws IOException {
        this.send("Message-ID: <message_id>");
    }

    private void sendQuit() throws IOException {
        this.send("QUIT");
        final String response = this.readInput();
        assertTrue(response, response.startsWith("221"));
    }

    private void sendReceiptTo() throws IOException {
        this.send("RCPT TO:<" + SmtpComplianceIntegrationTest.TO_ADDRESS + ">");
        final String response = this.readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void sendReset() throws IOException {
        this.send("RSET");
        final String response = this.readInput();
        assertTrue(response, response.startsWith("250"));
    }

    private void send(final String msg) {
        // Force \r\n since println() behaves differently on different platforms
        this.output.print(msg + "\r\n");
        this.output.flush();
    }

    private String readInput() throws IOException {
        final StringBuilder sb = new StringBuilder();
        do {
            sb.append(this.input.readLine()).append("\n");
        } while (this.input.ready());
        return sb.toString();
    }
}
