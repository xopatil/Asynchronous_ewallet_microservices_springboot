package com.sterling.user_service.controller;

import com.sterling.user_service.dto.LoginRequest;
import com.sterling.user_service.dto.RegisterRequest;
import com.sterling.user_service.dto.TokenResponse;
import com.sterling.user_service.model.User;
import com.sterling.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

// @RestController = @Controller + @ResponseBody combined.
// @Controller      → this class handles HTTP requests
// @ResponseBody    → return values are automatically converted to JSON
//                   (so you return a String or object, client receives JSON)
@RestController
@Slf4j
// @RequestMapping("/users"): All endpoints in this controller
// are prefixed with /users.
// So a method mapped to "/register" becomes "/users/register" in full.
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // @PostMapping("/register"): Handles HTTP POST requests to /users/register
    // POST is used for creating new resources (new user account here)
    // @RequestBody RegisterRequest request: Spring reads the JSON from the request body
    // and automatically converts it to a RegisterRequest object.
    // ResponseEntity is a wrapper around your response. It lets you set:
    //   - the response body (what you return)
    //   - the HTTP status code (200, 201, 400, 500 etc.)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            String message = userService.registerUser(request);
            // HttpStatus.CREATED = HTTP 201. Standard for "resource was created."
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (RuntimeException e) {
            // HTTP 400 Bad Request = client sent invalid data (duplicate username etc.)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        try {
            TokenResponse tokenResponse = userService.loginUser(request);
            log.info("POST /users/login success for: {}", request.getUsername());
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("POST /users/login failed for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // @GetMapping: Handles HTTP GET requests (fetching data, not creating)
    // @PathVariable: reads a value from the URL itself.
    // URL: /users/john → {username} = "john"
    @GetMapping("/{username}")
    public ResponseEntity<User> getUser(@PathVariable String username) {
        try {
            User user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            // HTTP 404 Not Found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Simple health check endpoint.
    // GET /users/health → returns "User Service is running"
    // Useful for quickly verifying the service started correctly.
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("User Service is running");
    }
}