package com.sterling.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// One final thing needed: Spring Security requires a UserDetailsService bean
// to load user data during authentication.
// We need to implement it so Spring knows HOW to look up users.
// Add this to your UserService or create a separate class.
// I'll handle this below with a note.

@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}