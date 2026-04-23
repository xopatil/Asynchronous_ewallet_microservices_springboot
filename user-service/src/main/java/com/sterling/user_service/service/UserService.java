package com.sterling.user_service.service;

import com.sterling.user_service.dto.LoginRequest;
import com.sterling.user_service.dto.RegisterRequest;
import com.sterling.user_service.model.User;
import com.sterling.user_service.repository.UserRepository;
import com.sterling.user_service.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// @Service marks this as a Spring-managed service component.
// Business logic lives here — not in the controller (which only handles HTTP),
// not in the repository (which only handles database).
@Slf4j
@Service
public class UserService {

    // @Autowired: Spring automatically finds the matching Bean and injects it here.
    // You never write "userRepository = new UserRepository()" — Spring does it for you.
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    // registerUser: Creates a new user account.
    public String registerUser(RegisterRequest request) {

        log.info("Registration attempt for username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed - username taken: {}", request.getUsername());
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email taken: {}", request.getEmail());
            throw new RuntimeException("Email already registered");
        }

        // Build the User object to save to database
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // NEVER save raw passwords.
        // passwordEncoder.encode() runs BCrypt hashing on the plain text.
        // "password123" → "$2a$10$..." (a hash that can't be reversed)
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Assign default role to all new users
        user.setRole("ROLE_USER");

        // Save to H2 database — generates SQL: INSERT INTO users (...)
        User saved = userRepository.save(user);

        log.info("User registered successfully. Username: {}, Id: {}",saved.getUsername(), saved.getId());


        return "User registered successfully";
    }

    // loginUser: Verifies credentials and returns a JWT token.
    public String loginUser(LoginRequest request) {

        // AuthenticationManager.authenticate() does two things automatically:
        // 1. Looks up the user by username in the database
        // 2. Compares the provided password against the stored BCrypt hash
        // If wrong → throws BadCredentialsException (Spring Security handles this)
        // If correct → authentication succeeds silently
        log.info("Login attempt for username: {}", request.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // If we reach here, credentials were valid.
        // Generate and return a JWT token for this user.
        log.info("Login successful for username: {}", request.getUsername());
        return jwtUtil.generateToken(request.getUsername());
    }

    // getUserByUsername: Fetches user details (for other services to call if needed)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                // If not found, throw an exception.
                // orElseThrow is cleaner than if(optional.isEmpty()) throw...
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}