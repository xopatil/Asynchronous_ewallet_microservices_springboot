package com.sterling.wallet_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication

// @EnableFeignClients: Tells Spring to scan for interfaces annotated
// with @FeignClient and create working HTTP client implementations for them.
// Without this, your UserServiceClient interface does nothing.
@EnableFeignClients
public class WalletServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}