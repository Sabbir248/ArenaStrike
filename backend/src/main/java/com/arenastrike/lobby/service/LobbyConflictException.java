package com.arenastrike.lobby.service;

public class LobbyConflictException extends RuntimeException {
    public LobbyConflictException(String message) {
        super(message);
    }
}
