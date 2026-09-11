package com.arenastrike.lobby.dto;

public record LobbyReadyCommand(Long userId, boolean isReady) {}
