// This declares the package (folder) this file belongs to.
// It must match your folder structure: com/sterling/eureka/
package com.sterling.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// @SpringBootApplication tells Spring Boot:
// "This is the starting point of the application."
// It also enables auto-configuration — Spring Boot will automatically
// set up things like web server, logging etc. without you configuring them.
@SpringBootApplication

// @EnableEurekaServer is the magic annotation.
// This single line transforms your plain Spring Boot app 
// into a fully functional Eureka registry server.
// Without this, Eureka won't start — it'll just be a blank app.
@EnableEurekaServer

public class EurekaServerApplication {

    // main() is where Java starts executing your program.
    // SpringApplication.run() boots up the entire Spring context —
    // it starts the embedded web server (Tomcat), loads all configs,
    // registers all beans, and starts Eureka.
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}