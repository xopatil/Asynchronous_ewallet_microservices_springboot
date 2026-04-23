package com.sterling.user_service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration: This class contains Spring configuration (bean definitions).
// Spring reads this class at startup and sets everything up.
@Configuration

// @EnableWebSecurity: Activates Spring Security's web security support.
// Without this, the security config is ignored.
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    // @Bean: This method's return value is managed by Spring.
    // Spring creates one instance of BCryptPasswordEncoder and
    // makes it available for @Autowired injection everywhere.
    // BCrypt is a password hashing algorithm. It's deliberately slow
    // to make brute-force attacks impractical.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationManager: Spring Security's core component that
    // handles the actual username/password verification during login.
    // We expose it as a Bean so UserService can inject and use it.
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // SecurityFilterChain: This is where you define your security RULES.
    // Which URLs are open? Which require login? What kind of auth?
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF = Cross-Site Request Forgery protection.
                // We DISABLE it because we're building a REST API using JWT tokens.
                // CSRF protection is designed for browser-based session apps,
                // not token-based APIs.
                .csrf(csrf -> csrf.disable())

                // Define authorization rules:
                .authorizeHttpRequests(auth -> auth
                        // permitAll() = no token required, anyone can access these:
                        // /users/register → new users need to be able to register without a token
                        // /users/login    → users need to be able to log in without a token
                        // /actuator/**    → health check endpoints should be public
                        // /h2-console/**  → H2 database viewer (development only)
                        .requestMatchers(
                                "/users/register",
                                "/users/login",
                                "/users/**",
                                "/actuator/**",
                                "/h2-console/**"
                        ).permitAll()
                        // anyRequest().authenticated() = everything else requires a valid JWT token
                        .anyRequest().authenticated()
                )

                // Session management: STATELESS means Spring Security will NOT
                // create HTTP sessions. Every request must carry its own JWT token.
                // This is the correct setting for microservices/REST APIs.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // H2 console is loaded in an HTML iframe.
                // Spring Security blocks iframes by default (clickjacking protection).
                // We disable the frameOptions restriction so H2 console loads correctly.
                .headers(headers ->
                        headers.frameOptions(frame -> frame.disable())
                )

                // Insert our JwtFilter BEFORE Spring's built-in login filter.
                // This means: for every request, check the JWT token first,
                // then let Spring Security do its own checks.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}