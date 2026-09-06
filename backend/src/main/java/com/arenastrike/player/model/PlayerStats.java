package com.arenastrike.player.model;

import jakarta.persistence.*;

@Entity
@Table(name = "player_stats")
public class PlayerStats {
    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private long lifetimeKills;

    @Column(nullable = false)
    private long lifetimeDeaths;

    @Column(nullable = false)
    private long matchesPlayed;

    protected PlayerStats() {
    }

    public PlayerStats(User user) {
        this.user = user;
    }

    public Long getUserId() { return userId; }
    public long getLifetimeKills() { return lifetimeKills; }
    public long getLifetimeDeaths() { return lifetimeDeaths; }
    public long getMatchesPlayed() { return matchesPlayed; }
}
