package com.arenastrike.lobby.model;

import com.arenastrike.player.model.Player;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "lobby_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_lobby_user", columnNames = {"lobby_id", "user_id"}))
public class LobbyParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lobby_id", nullable = false)
    private Lobby lobby;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Player user;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "is_ready", nullable = false)
    private boolean isReady;

    protected LobbyParticipant() {
    }

    public LobbyParticipant(Player user) {
        this.user = user;
        this.joinedAt = Instant.now();
    }

    void assignLobby(Lobby lobby) { this.lobby = lobby; }
    public Player getUser() { return user; }
    public Instant getJoinedAt() { return joinedAt; }
    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { this.isReady = ready; }
}
