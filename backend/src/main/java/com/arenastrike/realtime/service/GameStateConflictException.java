package com.arenastrike.realtime.service;

public class GameStateConflictException extends RuntimeException {
    public GameStateConflictException(String message) {
        super(message);
    }
}
