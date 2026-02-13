package de.gessnerfl.fakesmtp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("mockserver")
@SpringBootTest(properties = {
        "fakesmtp.webapp.authentication.username=testuser",
        "fakesmtp.webapp.authentication.password=testpass",
        "fakesmtp.webapp.rate-limiting.enabled=true",
        "fakesmtp.webapp.rate-limiting.max-attempts=2",
        "fakesmtp.webapp.rate-limiting.window-minutes=1",
        "fakesmtp.webapp.rate-limiting.whitelist-localhost=false",
        "fakesmtp.webapp.rate-limiting.trust-proxy-headers=false"
})
@AutoConfigureMockMvc
class RateLimitingFilterProxyHeaderTrustIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldIgnoreForwardedHeadersWhenTrustProxyHeadersIsDisabled() throws Exception {
        String remoteAddr = "203.0.113.10";

        MvcResult firstMeta = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        String firstCsrfToken = extractCsrfTokenFromCookies(firstMeta);
        jakarta.servlet.http.Cookie firstCsrfCookie = extractCsrfCookie(firstMeta);
        MockHttpSession firstSession = new MockHttpSession();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .session(firstSession)
                        .header("X-Forwarded-For", "198.51.100.10")
                        .param("username", "testuser")
                        .param("password", "wrongpass")
                        .cookie(firstCsrfCookie)
                        .header("X-XSRF-TOKEN", firstCsrfToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "2"));

        MvcResult secondMeta = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        String secondCsrfToken = extractCsrfTokenFromCookies(secondMeta);
        jakarta.servlet.http.Cookie secondCsrfCookie = extractCsrfCookie(secondMeta);
        MockHttpSession secondSession = new MockHttpSession();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .session(secondSession)
                        .header("X-Forwarded-For", "198.51.100.11")
                        .param("username", "testuser")
                        .param("password", "wrongpass")
                        .cookie(secondCsrfCookie)
                        .header("X-XSRF-TOKEN", secondCsrfToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-RateLimit-Remaining", "1"));

        MvcResult thirdMeta = mockMvc.perform(get("/api/meta-data"))
                .andExpect(status().isOk())
                .andReturn();
        String thirdCsrfToken = extractCsrfTokenFromCookies(thirdMeta);
        jakarta.servlet.http.Cookie thirdCsrfCookie = extractCsrfCookie(thirdMeta);
        MockHttpSession thirdSession = new MockHttpSession();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
                        .session(thirdSession)
                        .header("X-Forwarded-For", "198.51.100.12")
                        .param("username", "testuser")
                        .param("password", "wrongpass")
                        .cookie(thirdCsrfCookie)
                        .header("X-XSRF-TOKEN", thirdCsrfToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    private String extractCsrfTokenFromCookies(MvcResult result) {
        jakarta.servlet.http.Cookie cookie = extractCsrfCookie(result);
        return cookie != null ? cookie.getValue() : "";
    }

    private jakarta.servlet.http.Cookie extractCsrfCookie(MvcResult result) {
        if (result.getResponse().getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : result.getResponse().getCookies()) {
                if ("XSRF-TOKEN".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                    jakarta.servlet.http.Cookie copy = new jakarta.servlet.http.Cookie(cookie.getName(), cookie.getValue());
                    copy.setPath(cookie.getPath() != null ? cookie.getPath() : "/");
                    copy.setMaxAge(cookie.getMaxAge());
                    copy.setSecure(cookie.getSecure());
                    return copy;
                }
            }
        }
        return null;
    }
}
