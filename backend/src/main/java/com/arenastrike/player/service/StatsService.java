package com.arenastrike.player.service;

import com.arenastrike.match.model.MatchHistory;
import com.arenastrike.match.repository.MatchHistoryRepository;
import com.arenastrike.player.dto.PlayerStatsResponse;
import com.arenastrike.player.model.PlayerStats;
import com.arenastrike.player.model.User;
import com.arenastrike.player.repository.PlayerStatsRepository;
import com.arenastrike.player.repository.UserRepository;
import com.arenastrike.realtime.dto.MatchSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.ArrayList;

@Service
public class StatsService {
    private final UserRepository userRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final MatchHistoryRepository matchHistoryRepository;

    public StatsService(UserRepository userRepository, PlayerStatsRepository playerStatsRepository,
                        MatchHistoryRepository matchHistoryRepository) {
        this.userRepository = userRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.matchHistoryRepository = matchHistoryRepository;
    }

    @Transactional
    public void recordCompletedMatch(MatchSummary summary, int durationSeconds) {
        List<PlayerStats> updatedStats = new ArrayList<>(summary.results().size());
        summary.results().forEach(result -> {
            User user = userRepository.findById(result.playerId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Participant user not found: " + result.playerId()));
            PlayerStats stats = playerStatsRepository.findById(user.getId())
                    .orElseGet(() -> new PlayerStats(user));
            stats.recordMatch(result.kills(), result.deaths(),
                    summary.winnerIds().contains(result.playerId()), result.headshotKills());
            updatedStats.add(stats);
        });
        playerStatsRepository.saveAll(updatedStats);
        matchHistoryRepository.save(new MatchHistory(
                summary.roomId(), summary.map(), summary.winnerId(), durationSeconds));
    }

    @Async("dbThreadPool")
    @Transactional
    public void recordCompletedMatchAsync(MatchSummary summary, int durationSeconds) {
        recordCompletedMatch(summary, durationSeconds);
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse statsFor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        PlayerStats stats = playerStatsRepository.findById(userId)
                .orElseGet(() -> new PlayerStats(user));
        return response(user, stats);
    }

    @Transactional(readOnly = true)
    public List<PlayerStatsResponse> leaderboard() {
        return playerStatsRepository.findAllByOrderByTotalKillsDescWinsDesc().stream()
                .map(stats -> response(stats.getUser(), stats))
                .toList();
    }

    private PlayerStatsResponse response(User user, PlayerStats stats) {
        return new PlayerStatsResponse(user.getId(), user.getUsername(),
                stats.getTotalMatches(), stats.getWins(), stats.getTotalKills(),
                stats.getTotalDeaths(), stats.getKdr(), stats.getHeadshotKills());
    }
}
