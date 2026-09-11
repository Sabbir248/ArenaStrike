package com.arenastrike.realtime.service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

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
        private int kills;
        private int deaths;
        private int damageDealt;
        private int headshotKills;

        public PlayerScore(Long playerId) {
            this.playerId = playerId;
        }

        public Long getPlayerId() { return playerId; }
        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public int getDamageDealt() { return damageDealt; }
        public int getHeadshotKills() { return headshotKills; }

        public synchronized void incrementKills() { kills++; }
        public synchronized void incrementDeaths() { deaths++; }
        public synchronized void addDamage(int amount) { damageDealt += amount; }
        public synchronized void incrementHeadshots() { headshotKills++; }
    }
}
