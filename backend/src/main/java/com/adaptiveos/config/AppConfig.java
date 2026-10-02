package com.adaptiveos.config;

import com.adaptiveos.service.SimulationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the (Spring-free) SimulationService as a bean. */
@Configuration
public class AppConfig {

    @Bean
    public SimulationService simulationService() {
        return new SimulationService();
    }
}
