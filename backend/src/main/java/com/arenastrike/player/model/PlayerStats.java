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
    private long totalMatches;

    @Column(nullable = false)
    private long wins;

    @Column(nullable = false)
    private long totalKills;

    @Column(nullable = false)
    private long totalDeaths;

    @Column(nullable = false)
    private long headshotKills;

    protected PlayerStats() {
    }

    public PlayerStats(User user) {
        this.user = user;
    }

    public Long getUserId() { return userId; }
    public User getUser() { return user; }
    public long getTotalMatches() { return totalMatches; }
    public long getWins() { return wins; }
    public long getTotalKills() { return totalKills; }
    public long getTotalDeaths() { return totalDeaths; }
    public long getHeadshotKills() { return headshotKills; }
    public double getKdr() { return totalDeaths == 0 ? totalKills : (double) totalKills / totalDeaths; }

    public void recordMatch(int kills, int deaths, boolean won, int headshots) {
        totalMatches++;
        totalKills += kills;
        totalDeaths += deaths;
        headshotKills += headshots;
        if (won) {
            wins++;
        }
    }
}
