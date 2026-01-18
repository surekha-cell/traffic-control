package com.example.traffic.controller;

import com.example.traffic.model.IntersectionState;
import com.example.traffic.service.SimpleTrafficLightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    private final SimpleTrafficLightService service;

    public TrafficController(SimpleTrafficLightService service) {
        this.service = service;
    }

    @GetMapping("/state")
    public IntersectionState state() {
        return service.getCurrentState();
    }

    @GetMapping("/history")
    public List<IntersectionState> history(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return service.getHistory(Math.max(1, Math.min(limit, 500)));
    }

    @PostMapping("/pause")
    public ResponseEntity<Void> pause() {
        service.pause();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resume")
    public ResponseEntity<Void> resume() {
        service.resume();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/force-next")
    public ResponseEntity<Void> forceNext() {
        service.forceNextPhase();
        return ResponseEntity.ok().build();
    }
}
