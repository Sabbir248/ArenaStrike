package com.arenastrike.match.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "match_history")
public class MatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 6)
    private String roomId;

    @Column(name = "map_name", nullable = false, length = 32)
    private String mapName;

    @Column(name = "duration", nullable = false, updatable = false)
    private int duration;

    @Column(name = "played_at", nullable = false, updatable = false)
    private Instant playedAt;

    @Column(name = "winner_player_id")
    private Long winnerPlayerId;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayerMatchStat> playerStats = new ArrayList<>();

    protected MatchHistory() {
    }

    public MatchHistory(String roomId, String mapName, int duration, Long winnerPlayerId) {
        this.roomId = roomId;
        this.mapName = mapName;
        this.duration = duration;
        this.playedAt = Instant.now();
        this.winnerPlayerId = winnerPlayerId;
    }

    public void addPlayerStat(PlayerMatchStat stat) {
        playerStats.add(stat);
        stat.setMatch(this);
    }

    public Long getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getMapName() { return mapName; }
    public int getDuration() { return duration; }
    public Instant getPlayedAt() { return playedAt; }
    public Long getWinnerPlayerId() { return winnerPlayerId; }
    public List<PlayerMatchStat> getPlayerStats() { return playerStats; }
}
