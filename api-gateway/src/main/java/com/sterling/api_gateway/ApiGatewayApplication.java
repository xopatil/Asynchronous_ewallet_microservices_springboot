package com.sterling.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Notice: No special @Enable annotation needed here.
// The Gateway behavior comes entirely from application.properties config.
// Spring Boot auto-detects the Gateway dependency and activates it.

// @EnableDiscoveryClient used to be required to connect to Eureka,
// but in modern Spring Cloud it's auto-enabled when the dependency is present.
// You can add it explicitly for clarity — both work.

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}