package com.arenastrike.realtime.service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Scoreboard {
    private final ConcurrentHashMap<Long, PlayerScore> scores = new ConcurrentHashMap<>();

    public void addPlayer(Long playerId) {
        scores.putIfAbsent(playerId, new PlayerScore(playerId));
    }

    public void removePlayer(Long playerId) {
        scores.remove(playerId);
    }

    public void addKill(Long playerId, boolean isHeadshot) {
        scores.computeIfPresent(playerId, (id, score) -> {
            score.incrementKills();
            if (isHeadshot) score.incrementHeadshots();
            return score;
        });
    }

    public void addDeath(Long playerId) {
        scores.computeIfPresent(playerId, (id, score) -> {
            score.incrementDeaths();
            return score;
        });
    }

    public void addDamage(Long playerId, int amount) {
        scores.computeIfPresent(playerId, (id, score) -> {
            score.addDamage(amount);
            return score;
        });
    }

    public PlayerScore getScore(Long playerId) {
        return scores.get(playerId);
    }

    public Collection<PlayerScore> getAllScores() {
        return scores.values();
    }

    public static class PlayerScore {
        private final Long playerId;
        private final AtomicInteger kills = new AtomicInteger(0);
        private final AtomicInteger deaths = new AtomicInteger(0);
        private final AtomicInteger damageDealt = new AtomicInteger(0);
        private final AtomicInteger headshotKills = new AtomicInteger(0);

        public PlayerScore(Long playerId) {
            this.playerId = playerId;
        }

        public Long getPlayerId() { return playerId; }
        public int getKills() { return kills.get(); }
        public int getDeaths() { return deaths.get(); }
        public int getDamageDealt() { return damageDealt.get(); }
        public int getHeadshotKills() { return headshotKills.get(); }

        public void incrementKills() { kills.incrementAndGet(); }
        public void incrementDeaths() { deaths.incrementAndGet(); }
        public void addDamage(int amount) { damageDealt.addAndGet(amount); }
        public void incrementHeadshots() { headshotKills.incrementAndGet(); }
    }
}
