package com.sterling.transaction_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Feign client pointing to user-service.
// Transaction service uses this to verify both sender
// and receiver exist before processing any transaction.
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users/{username}")
    String getUserByUsername(@PathVariable("username") String username);
}