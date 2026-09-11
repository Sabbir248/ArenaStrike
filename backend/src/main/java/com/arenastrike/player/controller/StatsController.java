package com.arenastrike.player.controller;

import com.arenastrike.player.dto.PlayerStatsResponse;
import com.arenastrike.player.service.StatsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/leaderboard")
    public List<PlayerStatsResponse> leaderboard() {
        return statsService.leaderboard();
    }

    @GetMapping("/user/{id}/stats")
    public PlayerStatsResponse stats(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
        return statsService.statsFor(id);
    }
}
