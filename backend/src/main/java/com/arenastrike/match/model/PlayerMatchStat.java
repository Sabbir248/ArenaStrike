package com.arenastrike.match.model;

import com.arenastrike.player.model.Player;
import jakarta.persistence.*;

@Entity
@Table(name = "player_match_stats")
public class PlayerMatchStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchHistory match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private int kills;

    @Column(name = "headshot_kills", nullable = false)
    private int headshotKills;

    @Column(nullable = false)
    private int score;

    @Column(name = "weapon_used", nullable = false, length = 32)
    private String weaponUsed;

    protected PlayerMatchStat() {
    }

    public PlayerMatchStat(Player player, int kills, int headshotKills, int score, String weaponUsed) {
        this.player = player;
        this.kills = kills;
        this.headshotKills = headshotKills;
        this.score = score;
        this.weaponUsed = weaponUsed;
    }

    void setMatch(MatchHistory match) {
        this.match = match;
    }

    public Long getId() { return id; }
    public MatchHistory getMatch() { return match; }
    public Player getPlayer() { return player; }
    public int getKills() { return kills; }
    public int getHeadshotKills() { return headshotKills; }
    public int getScore() { return score; }
    public String getWeaponUsed() { return weaponUsed; }
}
