-- Add audit and soft-delete columns to the entity tables.
-- Each DDL change is conditional so V6 can be repaired and rerun safely after
-- MySQL/MariaDB has committed only part of an earlier attempt.

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'players' AND column_name = 'updated_at') = 0,
    'ALTER TABLE players ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'players' AND column_name = 'deleted_at') = 0,
    'ALTER TABLE players ADD COLUMN deleted_at TIMESTAMP(6) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'lobbies' AND column_name = 'updated_at') = 0,
    'ALTER TABLE lobbies ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'lobbies' AND column_name = 'deleted_at') = 0,
    'ALTER TABLE lobbies ADD COLUMN deleted_at TIMESTAMP(6) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'lobby_participants' AND column_name = 'created_at') = 0,
    'ALTER TABLE lobby_participants ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'lobby_participants' AND column_name = 'updated_at') = 0,
    'ALTER TABLE lobby_participants ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'lobby_participants' AND column_name = 'deleted_at') = 0,
    'ALTER TABLE lobby_participants ADD COLUMN deleted_at TIMESTAMP(6) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'match_history' AND column_name = 'created_at') = 0,
    'ALTER TABLE match_history ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'match_history' AND column_name = 'updated_at') = 0,
    'ALTER TABLE match_history ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'match_history' AND column_name = 'deleted_at') = 0,
    'ALTER TABLE match_history ADD COLUMN deleted_at TIMESTAMP(6) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'player_match_stats' AND column_name = 'created_at') = 0,
    'ALTER TABLE player_match_stats ADD COLUMN created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'player_match_stats' AND column_name = 'updated_at') = 0,
    'ALTER TABLE player_match_stats ADD COLUMN updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'player_match_stats' AND column_name = 'deleted_at') = 0,
    'ALTER TABLE player_match_stats ADD COLUMN deleted_at TIMESTAMP(6) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

-- Permit a player to rejoin after an old membership is soft-deleted, while
-- retaining uniqueness for active memberships.
SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'lobby_participants' AND index_name = 'uk_lobby_user') > 0,
    'ALTER TABLE lobby_participants DROP INDEX uk_lobby_user',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'lobby_participants' AND column_name = 'active_user_id') = 0,
    'ALTER TABLE lobby_participants ADD COLUMN active_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN user_id ELSE NULL END) STORED',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;

SET @v6_sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'lobby_participants' AND index_name = 'uk_lobby_active_user') = 0,
    'ALTER TABLE lobby_participants ADD CONSTRAINT uk_lobby_active_user UNIQUE (lobby_id, active_user_id)',
    'SELECT 1'
);
PREPARE v6_stmt FROM @v6_sql;
EXECUTE v6_stmt;
DEALLOCATE PREPARE v6_stmt;
