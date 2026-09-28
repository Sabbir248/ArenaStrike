package com.arenastrike.player.model;

import com.arenastrike.common.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.Locale;

@Entity
@Table(name = "players", uniqueConstraints = {
        @UniqueConstraint(name = "uk_players_oauth_identity", columnNames = {"oauth_provider", "oauth_subject"}),
        @UniqueConstraint(name = "uk_players_nickname_key", columnNames = "nickname_key")
})
@SQLDelete(sql = "UPDATE players SET deleted_at = CURRENT_TIMESTAMP(6), updated_at = CURRENT_TIMESTAMP(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Player extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(unique = true, length = 32)
    private String nickname;

    @Column(name = "nickname_key", length = 32, insertable = false, updatable = false)
    private String nicknameKey;

    @Column(name = "oauth_provider", length = 16)
    private String oauthProvider;

    @Column(name = "oauth_subject", length = 255)
    private String oauthSubject;

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
        this.username = username;
        this.totalKills = 0;
        this.totalDeaths = 0;
        this.matchesPlayed = 0;
        this.wins = 0;
    }

    public Player(String username, String oauthProvider, String oauthSubject) {
        this(username);
        this.oauthProvider = oauthProvider.toLowerCase(Locale.ROOT);
        this.oauthSubject = oauthSubject;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return nickname == null || nickname.isBlank() ? username : nickname; }
    public String getNickname() { return nickname; }
    public String getOauthProvider() { return oauthProvider; }
    public String getOauthSubject() { return oauthSubject; }
    public void setNickname(String nickname) { this.nickname = nickname; }
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
