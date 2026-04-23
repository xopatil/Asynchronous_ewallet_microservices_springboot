package com.sterling.user_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

// @Component tells Spring: "Create one instance of this class
// and manage it. Make it available for @Autowired injection."
// It's like registering this class in Spring's container.
@Component
public class JwtUtil {

    // @Value reads from application.properties.
    // ${jwt.secret} → picks up the value of jwt.secret property.
    // This is called "dependency injection from config".
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Converts the plain-text secret string into a cryptographic Key object.
    // HS256 = HMAC with SHA-256. This is the signing algorithm.
    // A Key object is what the JWT library actually uses to sign/verify tokens.
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // generateToken: Creates a new JWT token for a user after successful login.
    // A JWT token has 3 parts separated by dots: header.payload.signature
    //   header    = algorithm info (HS256)
    //   payload   = claims (username, expiry, issued-at) — visible to anyone!
    //   signature = cryptographic proof this token wasn't tampered with
    public String generateToken(String username) {
        return Jwts.builder()
                // setSubject: who this token belongs to (the username)
                .setSubject(username)
                // setIssuedAt: timestamp of when this token was created
                .setIssuedAt(new Date())
                // setExpiration: when this token stops being valid
                // new Date() = right now. Adding expiration ms = future time.
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                // signWith: cryptographically sign the token with our secret key
                // This prevents anyone from forging or modifying tokens.
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                // compact: converts the builder into the actual token string
                .compact();
    }

    // extractUsername: Opens a token and reads who it belongs to.
    // This is called on every protected API request to know who is making the call.
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                // Tell the parser which key was used to sign the token
                .setSigningKey(getSigningKey())
                .build()
                // parseClaimsJws: validates the signature AND parses the token
                // If token was tampered → throws exception here
                .parseClaimsJws(token)
                // getBody() gives you the payload (claims)
                .getBody()
                // getSubject() gives you what we put in setSubject() — the username
                .getSubject();
    }

    // validateToken: Full validation — is this token real AND not expired?
    // Returns true if valid, false if not.
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            // If we reach this line, the token is valid (no exception thrown)
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException covers: expired tokens, invalid signature, malformed tokens
            // IllegalArgumentException covers: null or empty token strings
            return false;
        }
    }
}