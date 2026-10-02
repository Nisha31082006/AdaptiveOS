package com.adaptiveos.controller;

import com.adaptiveos.dto.WorkloadResponse;
import com.adaptiveos.service.SimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Predefined example workloads (taken from the engine's WorkloadFactory). */
@RestController
@RequestMapping("/api/workloads")
public class WorkloadController {

    private final SimulationService service;

    public WorkloadController(SimulationService service) {
        this.service = service;
    }

    @GetMapping
    public List<WorkloadResponse> list() {
        return service.listWorkloads();
    }

    @GetMapping("/{id}")
    public WorkloadResponse get(@PathVariable String id) {
        return service.getWorkload(id);
    }
}
