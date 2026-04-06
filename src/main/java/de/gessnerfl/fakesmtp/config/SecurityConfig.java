package de.gessnerfl.fakesmtp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "base-uri 'self'",
            "object-src 'none'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self' data:",
            "connect-src 'self'",
            "frame-ancestors 'self'",
            "form-action 'self'"
    );

    private final WebappAuthenticationProperties authProperties;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final WebEndpointProperties webEndpointProperties;

    public SecurityConfig(WebappAuthenticationProperties authProperties,
                         CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                         WebEndpointProperties webEndpointProperties) {
        this.authProperties = authProperties;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.webEndpointProperties = webEndpointProperties;
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    // Allow unauthenticated GET access to the SPA shell so the React UI can render its own login form;
    // API requests under /api/** remain guarded by session auth.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository) throws Exception {
        if (authProperties.isAuthenticationEnabled()) {
            http
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(actuatorEndpoint("/health"), actuatorEndpoint("/info")).permitAll()
                    .requestMatchers(actuatorWildcard()).authenticated()
                    .requestMatchers("/api/meta-data").permitAll()
                    .requestMatchers("/api/auth/login", "/api/auth/logout", "/api/auth/status").permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/emails/**", "/assets/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/api/emails/events").authenticated()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/webjars/**").permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                    exceptionHandling.authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(formLogin -> formLogin
                    .loginProcessingUrl("/api/auth/login")
                    .successHandler((request, response, authentication) -> {
                        CsrfToken token = csrfTokenRepository.generateToken(request);
                        csrfTokenRepository.saveToken(token, request, response);
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    })
                    .failureHandler((request, response, exception) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                    .permitAll()
                )
                .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                )
                .sessionManagement(session -> session
                    .sessionFixation().changeSessionId()
                    .maximumSessions(authProperties.getConcurrentSessions())
                    .maxSessionsPreventsLogin(false)
                    .sessionRegistry(sessionRegistry())
                )
                .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                    .ignoringRequestMatchers("/h2-console/**")
                );
        } else {
            http
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(actuatorEndpoint("/health"), actuatorEndpoint("/info")).permitAll()
                    .requestMatchers(actuatorWildcard()).authenticated()
                    .anyRequest().permitAll()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/api/**", "/h2-console/**")
                );
        }

        configureSecurityHeaders(http);
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        if (authProperties.isAuthenticationEnabled()) {
            UserDetails user = User.builder()
                    .username(authProperties.getUsername())
                    .password(passwordEncoder().encode(authProperties.getPassword()))
                    .roles("USER")
                    .build();
            return new InMemoryUserDetailsManager(user);
        }
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    private static void configureSecurityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer ->
                        referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .addHeaderWriter((request, response) -> {
                    if (shouldApplyCsp(request)) {
                        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
                    }
                })
        );
    }

    private static boolean shouldApplyCsp(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path == null || path.isEmpty()) {
            return false;
        }
        return "/".equals(path)
                || "/index.html".equals(path)
                || "/emails".equals(path)
                || path.startsWith("/emails/");
    }

    private String actuatorEndpoint(String endpointPath) {
        String basePath = normalizeActuatorBasePath();
        if ("/".equals(basePath)) {
            return endpointPath;
        }
        return basePath + endpointPath;
    }

    private String actuatorWildcard() {
        String basePath = normalizeActuatorBasePath();
        if ("/".equals(basePath)) {
            return "/**";
        }
        return basePath + "/**";
    }

    private String normalizeActuatorBasePath() {
        String basePath = webEndpointProperties.getBasePath();
        if (!StringUtils.hasText(basePath)) {
            return "/actuator";
        }
        if (!basePath.startsWith("/")) {
            return "/" + basePath;
        }
        return basePath.endsWith("/") && basePath.length() > 1
                ? basePath.substring(0, basePath.length() - 1)
                : basePath;
    }

    /**
     * Custom CSRF token request handler for Single Page Applications (SPA).
     * Spring Security 6.x changed the default handler to XorCsrfTokenRequestAttributeHandler
     * which expects XOR-encoded tokens. This handler accepts raw tokens from headers (SPA use case)
     * while still providing BREACH protection for form submissions.
     */
    public static class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
        private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            // Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection
            this.delegate.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            /*
             * If the request contains a request header, use CsrfTokenRequestAttributeHandler
             * to resolve the CsrfToken. This applies when a single-page application includes
             * the header value automatically, which was obtained via a cookie containing the
             * raw CsrfToken.
             */
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return super.resolveCsrfTokenValue(request, csrfToken);
            }
            /*
             * In all other cases (e.g. if the request contains a request parameter), use
             * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This provides
             * BREACH protection for server-side rendered forms.
             */
            return this.delegate.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
