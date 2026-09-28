package com.arenastrike.lobby.model;

import com.arenastrike.common.model.BaseEntity;
import com.arenastrike.player.model.Player;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "lobby_participants")
@SQLDelete(sql = "UPDATE lobby_participants SET deleted_at = CURRENT_TIMESTAMP(6), updated_at = CURRENT_TIMESTAMP(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class LobbyParticipant extends BaseEntity {
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
