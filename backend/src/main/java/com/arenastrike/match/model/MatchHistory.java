package com.arenastrike.match.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "match_history")
public class MatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 6)
    private String roomId;

    @Column(nullable = false, length = 32)
    private String mapName;

    @Column(nullable = false)
    private Long winnerId;

    @Column(nullable = false, updatable = false)
    private int durationSeconds;

    @Column(nullable = false, updatable = false)
    private Instant endedAt;

    protected MatchHistory() {
    }

    public MatchHistory(String roomId, String mapName, Long winnerId, int durationSeconds) {
        this.roomId = roomId;
        this.mapName = mapName;
        this.winnerId = winnerId;
        this.durationSeconds = durationSeconds;
        this.endedAt = Instant.now();
    }

    @PrePersist
    void initializeEndedAt() {
        if (endedAt == null) {
            endedAt = Instant.now();
        }
    }
}
