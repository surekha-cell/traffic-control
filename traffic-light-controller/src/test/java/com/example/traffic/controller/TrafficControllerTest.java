package com.example.traffic.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TrafficControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void stateEndpointWorks() throws Exception {
        mvc.perform(get("/api/traffic/state"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.intersectionId").value("INT-1"));
    }

    @Test
    void pauseResumeEndpointsWork() throws Exception {
        mvc.perform(post("/api/traffic/pause")).andExpect(status().isOk());
        mvc.perform(post("/api/traffic/resume")).andExpect(status().isOk());
    }
}
