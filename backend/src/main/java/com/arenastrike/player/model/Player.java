package com.arenastrike.player.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private long totalKills;

    @Column(nullable = false)
    private long totalDeaths;

    @Column(nullable = false)
    private long matchesPlayed;

    @Column(nullable = false)
    private long wins;

    protected Player() {
    }

    public Player(String username) {
        this(username, "");
    }

    public Player(String username, String password) {
        this.username = username;
        this.password = password;
        this.createdAt = Instant.now();
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.matchesPlayed = 0;
        this.wins = 0;
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return username; }
    public String getPassword() { return password; }
    public Instant getCreatedAt() { return createdAt; }

    public long getTotalKills() { return totalKills; }
    public long getTotalDeaths() { return totalDeaths; }
    public long getMatchesPlayed() { return matchesPlayed; }
    public long getWins() { return wins; }
    public double getKdr() { return totalDeaths == 0 ? totalKills : (double) totalKills / totalDeaths; }

    public void recordMatch(int kills, int deaths, boolean won) {
        matchesPlayed++;
        totalKills += kills;
        totalDeaths += deaths;
        if (won) {
            wins++;
        }
    }
}
