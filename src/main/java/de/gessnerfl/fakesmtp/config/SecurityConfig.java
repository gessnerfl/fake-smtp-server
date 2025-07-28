package de.gessnerfl.fakesmtp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FakeSmtpAuthenticationProperties authProperties;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    public SecurityConfig(FakeSmtpAuthenticationProperties authProperties,
                         CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.authProperties = authProperties;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (authProperties.isAuthenticationEnabled()) {
            http
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/api/meta-data").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/", "/emails/**").authenticated()
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                    exceptionHandling.authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(customAuthenticationEntryPoint))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        } else {
            http
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/api/auth/status").permitAll()
                    .anyRequest().permitAll()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }
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
}
