CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS player_stats (
    user_id BIGINT NOT NULL,
    lifetime_kills BIGINT NOT NULL,
    lifetime_deaths BIGINT NOT NULL,
    matches_played BIGINT NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_player_stats_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS match_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    map_name VARCHAR(32) NOT NULL,
    kills INT NOT NULL,
    deaths INT NOT NULL,
    played_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_match_history_user_played (user_id, played_at),
    CONSTRAINT fk_match_history_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lobbies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_code VARCHAR(6) NOT NULL,
    map VARCHAR(16) NOT NULL,
    player_limit INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_lobby_room_code UNIQUE (room_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lobby_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lobby_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_lobby_user UNIQUE (lobby_id, user_id),
    KEY idx_lobby_participants_user (user_id),
    CONSTRAINT fk_lobby_participants_lobby
        FOREIGN KEY (lobby_id) REFERENCES lobbies (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lobby_participants_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
