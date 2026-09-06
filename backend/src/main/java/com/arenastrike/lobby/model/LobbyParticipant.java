package com.arenastrike.lobby.model;

import com.arenastrike.player.model.User;
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
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected LobbyParticipant() {
    }

    public LobbyParticipant(User user) {
        this.user = user;
        this.joinedAt = Instant.now();
    }

    void assignLobby(Lobby lobby) { this.lobby = lobby; }
    public User getUser() { return user; }
    public Instant getJoinedAt() { return joinedAt; }
}
