-- Code Optimization #8: Remove implicit ON UPDATE for timestamps
-- ALTER TABLE match_history
--     MODIFY COLUMN played_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Missing Feature #4: Add headshot_kills column mapping (from previous JPA entity update)
-- ALTER TABLE player_match_stats
--     ADD COLUMN headshot_kills INT NOT NULL DEFAULT 0 AFTER kills;

-- Code Optimization #9: Add missing indexes for leaderboards and queries
-- CREATE INDEX idx_players_leaderboard ON players(wins DESC, total_kills DESC);
-- CREATE INDEX idx_pms_player ON player_match_stats(player_id);
-- CREATE INDEX idx_mh_room ON match_history(room_id);

SELECT 1;
