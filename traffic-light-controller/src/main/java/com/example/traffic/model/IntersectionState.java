package com.example.traffic.model;

import java.time.Instant;

public record IntersectionState(
        String intersectionId,
        LightState northSouth,
        LightState eastWest,
        boolean paused,
        Instant lastUpdated
) {}
