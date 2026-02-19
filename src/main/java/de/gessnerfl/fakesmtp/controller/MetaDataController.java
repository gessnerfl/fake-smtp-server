package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.config.WebappAuthenticationProperties;
import de.gessnerfl.fakesmtp.model.ApplicationMetaData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/meta-data")
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
public class MetaDataController {

    private final BuildProperties buildProperties;
    private final WebappAuthenticationProperties authProperties;
    private final CsrfTokenRepository csrfTokenRepository;

    @Value("${server.servlet.session.timeout:10m}")
    private Duration sessionTimeout;

    public MetaDataController(BuildProperties buildProperties,
                              WebappAuthenticationProperties authProperties,
                              CsrfTokenRepository csrfTokenRepository) {
        this.buildProperties = buildProperties;
        this.authProperties = authProperties;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping
    public ApplicationMetaData get(Authentication authentication, CsrfToken csrfToken,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        if (csrfToken != null) {
            // Access the token to ensure it's generated and then save it to the cookie
            csrfToken.getToken();
            // Explicitly save the token to trigger cookie creation for GET requests
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }
        return new ApplicationMetaData(
                buildProperties.getVersion(),
                authProperties.isAuthenticationEnabled(),
                isAuthenticated(authentication),
                (int) sessionTimeout.toMinutes()
        );
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

}
