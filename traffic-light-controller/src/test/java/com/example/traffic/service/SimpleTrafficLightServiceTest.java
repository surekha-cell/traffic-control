package com.example.traffic.service;

import com.example.traffic.model.IntersectionState;
import com.example.traffic.model.LightColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTrafficLightServiceTest {

    private SimpleTrafficLightService svc;

    @BeforeEach
    void setUp() {
        svc = new SimpleTrafficLightService();
        // speed up timings
        svc.setGreenMs(300);
        svc.setYellowMs(150);
        svc.start();
    }

    @AfterEach
    void tearDown() {
        svc.stop();
    }

    @Test
    void cyclesAndNoConflictingGreen() throws Exception {
        Thread.sleep(800);
        IntersectionState state = svc.getCurrentState();
        var ns = state.northSouth().color();
        var ew = state.eastWest().color();
        assertFalse(ns == LightColor.GREEN && ew == LightColor.GREEN, "No conflicting greens");
    }

    @Test
    void pauseAndResume() throws Exception {
        svc.pause();
        var paused1 = svc.getCurrentState();
        assertTrue(paused1.paused());
        svc.resume();
        var s2 = svc.getCurrentState();
        assertFalse(s2.paused());
    }

    @Test
    void forceNextPhaseAdvances() throws Exception {
        var before = svc.getCurrentState();
        svc.forceNextPhase();
        Thread.sleep(50);
        var after = svc.getCurrentState();
        // color of at least one direction should change imminently
        boolean changed = before.northSouth().color() != after.northSouth().color() ||
                          before.eastWest().color() != after.eastWest().color();
        assertTrue(changed);
    }
}
