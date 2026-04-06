package de.gessnerfl.fakesmtp.smtp.server;

import de.gessnerfl.fakesmtp.smtp.MessageContext;
import de.gessnerfl.fakesmtp.smtp.MessageHandlerFactory;
import de.gessnerfl.fakesmtp.smtp.command.CommandHandler;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BaseSmtpServerTlsDefaultsTest {

    @Test
    void shouldEnableOnlyTls13AndTls12ByDefault() throws IOException {
        var tlsSocket = new RecordingSslSocket(
                new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1"},
                new String[]{"TLS_AES_128_GCM_SHA256"},
                new String[]{"TLS_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA"}
        );
        var sut = createServer();
        sut.setSslContext(new TestSslContext(new RecordingSslSocketFactory(tlsSocket)));

        var result = sut.createSSLSocket(new ConnectedTestSocket());

        assertThat(result).isSameAs(tlsSocket);
        assertThat(tlsSocket.getRecordedEnabledProtocols()).containsExactly("TLSv1.3", "TLSv1.2");
        assertThat(tlsSocket.wasEnabledCipherSuitesExplicitlyConfigured()).isFalse();
        assertThat(tlsSocket.getUseClientMode()).isFalse();
        assertThat(tlsSocket.getNeedClientAuth()).isFalse();
    }

    @Test
    void shouldUseConfiguredTlsProtocolOverride() throws IOException {
        var tlsSocket = new RecordingSslSocket(
                new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1"},
                new String[]{"TLS_AES_128_GCM_SHA256"},
                new String[]{"TLS_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA"}
        );
        var sut = createServer();
        sut.setSslContext(new TestSslContext(new RecordingSslSocketFactory(tlsSocket)));
        sut.setTlsProtocols(List.of("TLSv1.2"));

        sut.createSSLSocket(new ConnectedTestSocket());

        assertThat(tlsSocket.getRecordedEnabledProtocols()).containsExactly("TLSv1.2");
    }

    private static BaseSmtpServer createServer() {
        return new BaseSmtpServer(
                "FakeSMTPServer Test",
                new NoOpMessageHandlerFactory(),
                mock(CommandHandler.class),
                () -> "test-session",
                false
        );
    }

    private static final class NoOpMessageHandlerFactory implements MessageHandlerFactory {
        @Override
        public de.gessnerfl.fakesmtp.smtp.MessageHandler create(MessageContext ctx) {
            throw new UnsupportedOperationException("Not needed in TLS configuration tests");
        }
    }

    private static final class ConnectedTestSocket extends Socket {
        @Override
        public InetSocketAddress getRemoteSocketAddress() {
            return new InetSocketAddress("localhost", 2525);
        }

        @Override
        public int getPort() {
            return 2525;
        }
    }

    private static final class TestSslContext extends SSLContext {
        TestSslContext(SSLSocketFactory socketFactory) {
            super(new TestSslContextSpi(socketFactory), new TestProvider(), "TLS");
        }
    }

    private static final class TestProvider extends Provider {
        TestProvider() {
            super("test-provider", "1.0", "test provider");
        }
    }

    private static final class TestSslContextSpi extends SSLContextSpi {
        private final SSLSocketFactory socketFactory;

        TestSslContextSpi(SSLSocketFactory socketFactory) {
            this.socketFactory = socketFactory;
        }

        @Override
        protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) {
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return socketFactory;
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return null;
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return null;
        }

        @Override
        protected SSLParameters engineGetDefaultSSLParameters() {
            return new SSLParameters();
        }

        @Override
        protected SSLParameters engineGetSupportedSSLParameters() {
            return new SSLParameters();
        }
    }

    private static final class RecordingSslSocketFactory extends SSLSocketFactory {
        private final SSLSocket socket;

        RecordingSslSocketFactory(SSLSocket socket) {
            this.socket = socket;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return socket.getEnabledCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return socket.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) {
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port) {
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort) {
            return socket;
        }

        @Override
        public Socket createSocket(java.net.InetAddress host, int port) {
            return socket;
        }

        @Override
        public Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) {
            return socket;
        }
    }

    private static final class RecordingSslSocket extends SSLSocket {
        private final String[] supportedProtocols;
        private String[] enabledProtocols;
        private final String[] supportedCipherSuites;
        private String[] enabledCipherSuites;
        private boolean enabledCipherSuitesExplicitlyConfigured;
        private boolean useClientMode;
        private boolean needClientAuth;
        private boolean wantClientAuth;
        private boolean enableSessionCreation = true;

        RecordingSslSocket(String[] supportedProtocols, String[] enabledProtocols, String[] supportedCipherSuites) {
            this.supportedProtocols = supportedProtocols;
            this.enabledProtocols = enabledProtocols;
            this.supportedCipherSuites = supportedCipherSuites;
            this.enabledCipherSuites = new String[]{"TLS_AES_128_GCM_SHA256"};
        }

        String[] getRecordedEnabledProtocols() {
            return enabledProtocols;
        }

        boolean wasEnabledCipherSuitesExplicitlyConfigured() {
            return enabledCipherSuitesExplicitlyConfigured;
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return supportedCipherSuites;
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return enabledCipherSuites;
        }

        @Override
        public void setEnabledCipherSuites(String[] suites) {
            this.enabledCipherSuites = suites;
            this.enabledCipherSuitesExplicitlyConfigured = true;
        }

        @Override
        public String[] getSupportedProtocols() {
            return supportedProtocols;
        }

        @Override
        public String[] getEnabledProtocols() {
            return enabledProtocols;
        }

        @Override
        public void setEnabledProtocols(String[] protocols) {
            this.enabledProtocols = protocols;
        }

        @Override
        public void startHandshake() {
        }

        @Override
        public void setUseClientMode(boolean mode) {
            this.useClientMode = mode;
        }

        @Override
        public boolean getUseClientMode() {
            return useClientMode;
        }

        @Override
        public void setNeedClientAuth(boolean need) {
            this.needClientAuth = need;
        }

        @Override
        public boolean getNeedClientAuth() {
            return needClientAuth;
        }

        @Override
        public void setWantClientAuth(boolean want) {
            this.wantClientAuth = want;
        }

        @Override
        public boolean getWantClientAuth() {
            return wantClientAuth;
        }

        @Override
        public void setEnableSessionCreation(boolean flag) {
            this.enableSessionCreation = flag;
        }

        @Override
        public boolean getEnableSessionCreation() {
            return enableSessionCreation;
        }

        @Override
        public javax.net.ssl.SSLSession getSession() {
            return null;
        }

        @Override
        public void addHandshakeCompletedListener(javax.net.ssl.HandshakeCompletedListener listener) {
        }

        @Override
        public void removeHandshakeCompletedListener(javax.net.ssl.HandshakeCompletedListener listener) {
        }
    }
}
