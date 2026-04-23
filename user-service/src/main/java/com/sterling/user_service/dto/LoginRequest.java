package com.sterling.user_service.dto;

import lombok.Data;

// Shape of data for login.
// Client sends: { "username": "john", "password": "1234" }
@Data
public class LoginRequest {
    private String username;
    private String password;
}