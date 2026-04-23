package com.sterling.user_service.dto;

import lombok.Data;

// This is the shape of data we EXPECT when a user registers.
// Client sends: { "username": "john", "email": "john@gmail.com", "password": "1234" }
// Spring Boot automatically maps that JSON to this Java object.
// No @Entity here — this is NOT a database table. It's just a data carrier.
@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
}