package com.arenastrike.match.model;

import com.arenastrike.player.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "match_history")
public class MatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 32)
    private String mapName;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false, updatable = false)
    private Instant playedAt;

    protected MatchHistory() {
    }

    public MatchHistory(User user, String mapName, int kills, int deaths) {
        this.user = user;
        this.mapName = mapName;
        this.kills = kills;
        this.deaths = deaths;
        this.playedAt = Instant.now();
    }

    @PrePersist
    void initializePlayedAt() {
        if (playedAt == null) {
            playedAt = Instant.now();
        }
    }
}
