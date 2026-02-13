package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.config.WebappAuthenticationProperties;
import de.gessnerfl.fakesmtp.config.WebappSessionProperties;
import de.gessnerfl.fakesmtp.model.ApplicationMetaData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@RestController
@RequestMapping("/api/meta-data")
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
public class MetaDataController {

    private final BuildProperties buildProperties;
    private final WebappAuthenticationProperties authProperties;
    private final CsrfTokenRepository csrfTokenRepository;
    private final WebappSessionProperties sessionProperties;

    public MetaDataController(BuildProperties buildProperties,
                              WebappAuthenticationProperties authProperties,
                              CsrfTokenRepository csrfTokenRepository,
                              WebappSessionProperties sessionProperties) {
        this.buildProperties = buildProperties;
        this.authProperties = authProperties;
        this.csrfTokenRepository = csrfTokenRepository;
        this.sessionProperties = sessionProperties;
    }

    @GetMapping
    public ApplicationMetaData get(Authentication authentication, CsrfToken csrfToken,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        if (csrfToken != null) {
            csrfToken.getToken();
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }
        return new ApplicationMetaData(
                buildProperties.getVersion(),
                authProperties.isAuthenticationEnabled(),
                isAuthenticated(authentication),
                sessionProperties.getSessionTimeoutMinutes()
        );
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

}
