package com.sterling.user_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

// This is the OAuth2-standard token response format.
// Real OAuth2 servers return exactly these fields.
// token_type, access_token, expires_in are the three
// required fields in OAuth2 Bearer Token response spec.
@Data
@AllArgsConstructor
public class TokenResponse {

    // The actual JWT token string
    // @JsonProperty maps the Java field name to the JSON key name
    // Java: accessToken → JSON: "access_token"
    @JsonProperty("access_token")
    private String accessToken;

    // Always "Bearer" for JWT-based auth
    // Tells the client how to use this token
    // Client must send: Authorization: Bearer <token>
    @JsonProperty("token_type")
    private String tokenType;

    // How many seconds until this token expires
    // 86400 = 24 hours
    @JsonProperty("expires_in")
    private long expiresIn;

    // The username this token belongs to
    @JsonProperty("username")
    private String username;
}