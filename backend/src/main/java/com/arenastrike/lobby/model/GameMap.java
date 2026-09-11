package com.arenastrike.lobby.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum GameMap {
    MAP_WAREHOUSE,
    MAP_BUNKER;

    @JsonCreator
    public static GameMap fromJson(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');
        return GameMap.valueOf(normalized);
    }
}
