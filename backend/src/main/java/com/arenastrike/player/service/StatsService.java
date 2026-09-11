package com.arenastrike.player.service;

import com.arenastrike.match.model.MatchHistory;
import com.arenastrike.match.model.PlayerMatchStat;
import com.arenastrike.match.repository.MatchHistoryRepository;
import com.arenastrike.player.dto.PlayerStatsResponse;
import com.arenastrike.player.model.Player;
import com.arenastrike.player.repository.PlayerRepository;
import com.arenastrike.realtime.dto.MatchSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.util.List;
import java.util.ArrayList;

@Service
public class StatsService {
    private final PlayerRepository playerRepository;
    private final MatchHistoryRepository matchHistoryRepository;

    public StatsService(PlayerRepository playerRepository,
                        MatchHistoryRepository matchHistoryRepository) {
        this.playerRepository = playerRepository;
        this.matchHistoryRepository = matchHistoryRepository;
    }

    @Transactional
    public void recordCompletedMatch(MatchSummary summary, int durationSeconds) {
        MatchHistory match = new MatchHistory(
                summary.roomId(), summary.map(), durationSeconds, summary.winnerId());

        List<Player> updatedPlayers = new ArrayList<>(summary.results().size());

        summary.results().forEach(result -> {
            Player player = playerRepository.findById(result.playerId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Participant player not found: " + result.playerId()));
            
            boolean won = summary.winnerIds() != null && summary.winnerIds().contains(result.playerId());
            player.recordMatch(result.kills(), result.deaths(), won);
            updatedPlayers.add(player);

            PlayerMatchStat stat = new PlayerMatchStat(
                    player, result.kills(), result.damageDealt(), result.weaponUsed());
            match.addPlayerStat(stat);
        });

        playerRepository.saveAll(updatedPlayers);
        matchHistoryRepository.save(match);
    }

    @Async("dbThreadPool")
    @Transactional
    public void recordCompletedMatchAsync(MatchSummary summary, int durationSeconds) {
        recordCompletedMatch(summary, durationSeconds);
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse statsFor(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        return response(player);
    }

    @Transactional(readOnly = true)
    public List<PlayerStatsResponse> leaderboard() {
        return playerRepository.findAllByOrderByWinsDescTotalKillsDesc().stream()
                .map(this::response)
                .toList();
    }

    private PlayerStatsResponse response(Player player) {
        return new PlayerStatsResponse(player.getId(), player.getUsername(),
                player.getMatchesPlayed(), player.getWins(), player.getTotalKills(),
                player.getTotalDeaths(), player.getKdr(), 0); // Headshots can be omitted or added to Player if needed later
    }
}
