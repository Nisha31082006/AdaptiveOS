package com.adaptiveos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP-level tests of the REST contract (routes, JSON shape, error format, CORS). Requires Spring Boot. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String BASELINE = """
            {"mode":"ADAPTIVE","processes":[
              {"pid":"P1","arrivalTime":0,"bursts":[{"type":"CPU","duration":10}]},
              {"pid":"P2","arrivalTime":1,"bursts":[{"type":"CPU","duration":3},{"type":"IO","duration":4},{"type":"CPU","duration":2}]},
              {"pid":"P3","arrivalTime":2,"bursts":[{"type":"CPU","duration":6}]}]}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void workloadsAreListedAndFetchable() throws Exception {
        mvc.perform(get("/api/workloads")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("baseline"));
        mvc.perform(get("/api/workloads/phase-change")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phase Change"));
        mvc.perform(get("/api/workloads/nope")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.path").value("/api/workloads/nope"));
    }

    @Test
    void simulateReturnsEngineResults() throws Exception {
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON).content(BASELINE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ADAPTIVE"))
                .andExpect(jsonPath("$.policyTransitions[0].time").value(15))
                .andExpect(jsonPath("$.policyTransitions[0].interactivityScore").value(306.0))
                .andExpect(jsonPath("$.metrics.contextSwitches").value(5))
                .andExpect(jsonPath("$.finalPolicy").value("SRTF"));
    }

    @Test
    void compareReturnsThreeModes() throws Exception {
        mvc.perform(post("/api/compare").contentType(MediaType.APPLICATION_JSON).content(BASELINE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pureRr.metrics.contextSwitches").value(6))
                .andExpect(jsonPath("$.pureSrtf.metrics.contextSwitches").value(4))
                .andExpect(jsonPath("$.adaptive.mode").value("ADAPTIVE"));
    }

    @Test
    void invalidInputGivesConsistentErrorBody() throws Exception {
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"ADAPTIVE\",\"alpha\":2,\"processes\":[{\"pid\":\"P1\",\"arrivalTime\":0,\"bursts\":[{\"type\":\"CPU\",\"duration\":1}]}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/simulate"));
    }

    @Test
    void malformedJsonGivesBadRequestWithoutStackTrace() throws Exception {
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable JSON request body"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void corsAllowsTheViteDevServer() throws Exception {
        mvc.perform(options("/api/simulate")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
