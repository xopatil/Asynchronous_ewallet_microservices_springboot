package com.sterling.wallet_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// @FeignClient: This interface becomes an HTTP client automatically.
// name = "user-service" → Feign looks up this service name in Eureka
//                         to find the actual address. No hardcoded ports.
// When you call a method on this interface, Feign makes a real HTTP
// request to User Service behind the scenes.
@FeignClient(name = "user-service")
public interface UserServiceClient {

    // This maps to GET /users/{username} on User Service.
    // When wallet service calls userServiceClient.getUserByUsername("john"),
    // Feign sends: GET http://user-service/users/john
    // and returns the response as a String (the user's JSON data)
    @GetMapping("/users/{username}")
    String getUserByUsername(@PathVariable("username") String username);
}