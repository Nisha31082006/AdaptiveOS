package com.adaptiveos.controller;

import com.adaptiveos.dto.CompareResponse;
import com.adaptiveos.dto.SimulationRequest;
import com.adaptiveos.dto.SimulationResponse;
import com.adaptiveos.service.SimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** REST endpoints that run the AdaptiveOS engine. No scheduling logic lives here. */
@RestController
@RequestMapping("/api")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    /** Runs one simulation in the requested mode. */
    @PostMapping("/simulate")
    public SimulationResponse simulate(@RequestBody SimulationRequest request) {
        return service.simulate(request);
    }

    /** Runs PURE_RR, PURE_SRTF and ADAPTIVE on the same workload and parameters. */
    @PostMapping("/compare")
    public CompareResponse compare(@RequestBody SimulationRequest request) {
        return service.compare(request);
    }

    /** Liveness probe (used by the frontend and Docker). */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
