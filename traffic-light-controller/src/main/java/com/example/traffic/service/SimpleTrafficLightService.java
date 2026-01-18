package com.example.traffic.service;

import com.example.traffic.model.*;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class SimpleTrafficLightService {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "traffic-light-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final String intersectionId = "INT-1";
    private volatile boolean paused = false;

    private LightColor ns = LightColor.RED;
    private LightColor ew = LightColor.GREEN;

    private long greenMs = 5000; // configurable
    private long yellowMs = 2000; // configurable

    private long phaseDeadline = 0L;
    private Phase phase = Phase.EW_GREEN;

    private enum Phase { EW_GREEN, EW_YELLOW, NS_GREEN, NS_YELLOW }

    private final int HISTORY_MAX = 1000;
    private final ArrayDeque<IntersectionState> history = new ArrayDeque<>(HISTORY_MAX);

    @PostConstruct
    public void start() {
        scheduler.scheduleWithFixedDelay(this::tick, 0, 200, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    private void tick() {
        if (paused) return;
        long now = System.currentTimeMillis();

        lock.writeLock().lock();
        try {
            if (phaseDeadline == 0L) {
                enterPhase(phase, now);
                return;
            }
            if (now >= phaseDeadline) {
                switch (phase) {
                    case EW_GREEN -> enterPhase(Phase.EW_YELLOW, now);
                    case EW_YELLOW -> enterPhase(Phase.NS_GREEN, now);
                    case NS_GREEN -> enterPhase(Phase.NS_YELLOW, now);
                    case NS_YELLOW -> enterPhase(Phase.EW_GREEN, now);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void enterPhase(Phase next, long now) {
        switch (next) {
            case EW_GREEN -> {
                ew = LightColor.GREEN; ns = LightColor.RED;
                phaseDeadline = now + greenMs;
            }
            case EW_YELLOW -> {
                ew = LightColor.YELLOW; ns = LightColor.RED;
                phaseDeadline = now + yellowMs;
            }
            case NS_GREEN -> {
                ns = LightColor.GREEN; ew = LightColor.RED;
                phaseDeadline = now + greenMs;
            }
            case NS_YELLOW -> {
                ns = LightColor.YELLOW; ew = LightColor.RED;
                phaseDeadline = now + yellowMs;
            }
        }
        phase = next;
        validateNoConflictingGreen();
        recordHistorySnapshot();
    }

    private void validateNoConflictingGreen() {
        if (ns == LightColor.GREEN && ew == LightColor.GREEN) {
            throw new IllegalStateException("Conflict: NS and EW both GREEN!");
        }
    }

    private void recordHistorySnapshot() {
        var now = Instant.now();
        var snapshot = new IntersectionState(
                intersectionId,
                new LightState(Direction.NORTH_SOUTH, ns, now),
                new LightState(Direction.EAST_WEST, ew, now),
                paused,
                now
        );
        if (history.size() == HISTORY_MAX) history.removeFirst();
        history.addLast(snapshot);
    }

    // API methods
    public IntersectionState getCurrentState() {
        lock.readLock().lock();
        try {
            var now = Instant.now();
            return new IntersectionState(
                    intersectionId,
                    new LightState(Direction.NORTH_SOUTH, ns, now),
                    new LightState(Direction.EAST_WEST, ew, now),
                    paused,
                    now
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<IntersectionState> getHistory(int limit) {
        lock.readLock().lock();
        try {
            return history.stream()
                    .skip(Math.max(0, history.size() - limit))
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void pause() {
        lock.writeLock().lock();
        try {
            paused = true;
            recordHistorySnapshot();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void resume() {
        lock.writeLock().lock();
        try {
            paused = false;
            recordHistorySnapshot();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void forceNextPhase() {
        lock.writeLock().lock();
        try {
            long now = System.currentTimeMillis();
            switch (phase) {
                case EW_GREEN -> enterPhase(Phase.EW_YELLOW, now);
                case EW_YELLOW -> enterPhase(Phase.NS_GREEN, now);
                case NS_GREEN -> enterPhase(Phase.NS_YELLOW, now);
                case NS_YELLOW -> enterPhase(Phase.EW_GREEN, now);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Setters for timings (optional)
    public void setGreenMs(long greenMs) { this.greenMs = greenMs; }
    public void setYellowMs(long yellowMs) { this.yellowMs = yellowMs; }
}
