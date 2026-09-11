-- Drop old tables
DROP TABLE IF EXISTS player_stats;
DROP TABLE IF EXISTS match_history;

-- Rename users to players and add stat columns
RENAME TABLE users TO players;

ALTER TABLE players
    CHANGE COLUMN display_name username VARCHAR(32) NOT NULL,
    ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN total_kills BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN total_deaths BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN matches_played BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN wins BIGINT NOT NULL DEFAULT 0;

-- Update foreign keys that referenced users
ALTER TABLE lobby_participants DROP FOREIGN KEY fk_lobby_participants_user;
ALTER TABLE lobby_participants ADD CONSTRAINT fk_lobby_participants_player
    FOREIGN KEY (user_id) REFERENCES players (id) ON DELETE CASCADE;

-- Create match_history
CREATE TABLE match_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id VARCHAR(6) NOT NULL,
    map_name VARCHAR(32) NOT NULL,
    duration INT NOT NULL,
    played_at TIMESTAMP(6) NOT NULL,
    winner_player_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_match_history_winner
        FOREIGN KEY (winner_player_id) REFERENCES players (id)
        ON DELETE SET NULL
) ENGINE=InnoDB;

-- Create player_match_stats
CREATE TABLE player_match_stats (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    kills INT NOT NULL,
    score INT NOT NULL,
    weapon_used VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_player_match_stats_match
        FOREIGN KEY (match_id) REFERENCES match_history (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_player_match_stats_player
        FOREIGN KEY (player_id) REFERENCES players (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
