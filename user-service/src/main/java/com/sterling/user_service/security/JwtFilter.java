package com.sterling.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

// OncePerRequestFilter: A Spring helper that guarantees this filter
// runs exactly ONCE per HTTP request (not multiple times).
// We extend it so we get that guarantee for free.
@Component
public class JwtFilter extends OncePerRequestFilter {

    // Inject our JwtUtil so we can use its methods here
    @Autowired
    private JwtUtil jwtUtil;

    // doFilterInternal is called automatically for every HTTP request.
    // request  = the incoming HTTP request (headers, body, URL)
    // response = the HTTP response we'll send back
    // filterChain = the chain of filters — we must call chain.doFilter()
    //               to pass the request to the next filter (or to the controller)
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Read the "Authorization" header from the request.
        // When a logged-in user makes a request, they send:
        // Authorization: Bearer eyJhbGc...   (the JWT token)
        // "Bearer" is a standard prefix meaning "I am presenting this token"
        String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        // Step 2: Check if the header exists and starts with "Bearer "
        // If no header → this is probably a public request (login/register) → let it pass
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract just the token part (remove "Bearer " prefix — 7 characters)
            token = authHeader.substring(7);
            // Extract the username from inside the token
            username = jwtUtil.extractUsername(token);
        }

        // Step 3: If we got a username from the token AND the user isn't
        // already authenticated in this request's security context...
        // SecurityContextHolder is Spring Security's storage for "who is currently logged in"
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Step 4: Validate the token is genuine and not expired
            if (jwtUtil.validateToken(token)) {

                // Step 5: Create an Authentication object.
                // UsernamePasswordAuthenticationToken = Spring Security's way of
                // representing a logged-in user.
                // Parameters: (principal, credentials, authorities)
                //   principal   = the username (who they are)
                //   credentials = null (we don't need the password anymore, token is enough)
                //   authorities = list of roles/permissions (empty ArrayList for now)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                username, null, new ArrayList<>()
                        );

                // Attach extra details like IP address to the auth object
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Step 6: Put the authenticated user into the Security Context.
                // This tells Spring Security "this request is authenticated as {username}"
                // All downstream code can now call SecurityContextHolder.getContext()
                // .getAuthentication() to know who is making this request.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Step 7: Always pass the request forward —
        // whether authenticated or not, the next filter decides what to do with it.
        // For unauthenticated requests to protected endpoints,
        // Spring Security will block them after all filters run.
        filterChain.doFilter(request, response);
    }
}