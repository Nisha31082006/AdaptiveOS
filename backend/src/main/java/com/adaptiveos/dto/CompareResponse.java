package com.adaptiveos.dto;

/** The same workload and parameters run under all three modes. No ranking is attached on purpose. */
public record CompareResponse(ParametersResponse parameters, SimulationResponse pureRr,
                              SimulationResponse pureSrtf, SimulationResponse adaptive) { }
