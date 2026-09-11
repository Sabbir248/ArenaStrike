package com.arenastrike.realtime;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

@Component
public class GameRoomManager {
    private final ConcurrentHashMap<String, GameRoom> activeMatches = new ConcurrentHashMap<>();

    public GameRoom computeIfAbsent(String roomId, java.util.function.Function<String, GameRoom> mappingFunction) {
        return activeMatches.computeIfAbsent(roomId, mappingFunction);
    }

    public GameRoom get(String roomId) {
        return activeMatches.get(roomId);
    }

    public GameRoom remove(String roomId) {
        return activeMatches.remove(roomId);
    }

    public Collection<GameRoom> getAllRooms() {
        return activeMatches.values();
    }

    public void clear() {
        activeMatches.clear();
    }
}
