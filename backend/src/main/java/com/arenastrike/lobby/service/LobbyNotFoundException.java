package com.arenastrike.lobby.service;

public class LobbyNotFoundException extends RuntimeException {
    public LobbyNotFoundException(String roomCode) {
        super("Lobby not found: " + roomCode);
    }
}
