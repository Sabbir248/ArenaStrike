package com.arenastrike.lobby.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lobbies", uniqueConstraints = @UniqueConstraint(name = "uk_lobby_room_code", columnNames = "room_code"))
public class Lobby {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, length = 6, updatable = false)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GameMap map;

    @Column(nullable = false)
    private int playerLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LobbyStatus status = LobbyStatus.WAITING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("joinedAt ASC")
    private List<LobbyParticipant> participants = new ArrayList<>();

    protected Lobby() {
    }

    public Lobby(String roomCode, GameMap map, int playerLimit) {
        this.roomCode = roomCode;
        this.map = map;
        this.playerLimit = playerLimit;
        this.createdAt = Instant.now();
    }

    public void addParticipant(LobbyParticipant participant) {
        if (status != LobbyStatus.WAITING) {
            throw new IllegalStateException("Lobby is not accepting players");
        }
        if (participants.size() >= playerLimit) {
            throw new IllegalStateException("Lobby is full");
        }
        participants.add(participant);
        participant.assignLobby(this);
    }

    public boolean containsUser(Long userId) {
        return participants.stream().anyMatch(p -> p.getUser().getId().equals(userId));
    }

    public Long getId() { return id; }
    public String getRoomCode() { return roomCode; }
    public GameMap getMap() { return map; }
    public int getPlayerLimit() { return playerLimit; }
    public LobbyStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<LobbyParticipant> getParticipants() { return participants; }

    public void resetForNextMatch() {
        participants.clear();
        status = LobbyStatus.WAITING;
    }
}
