package com.adaptiveos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot entry point: exposes the AdaptiveOS engine over REST on port 8080. */
@SpringBootApplication
public class AdaptiveOsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdaptiveOsApplication.class, args);
    }
}
