package com.example.traffic.model;

import java.time.Instant;

public record LightState(Direction direction, LightColor color, Instant changedAt) {}
