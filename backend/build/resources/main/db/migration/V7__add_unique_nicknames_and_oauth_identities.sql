-- Add optional unique nicknames and OAuth identity columns to existing players.
-- Conditional DDL makes the migration safe to rerun after a partially applied
-- MySQL DDL migration (MySQL commits ALTER TABLE statements independently).

SET @v7_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'players' AND column_name = 'nickname') = 0,
    'ALTER TABLE players ADD COLUMN nickname VARCHAR(32) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE v7_stmt FROM @v7_sql;
EXECUTE v7_stmt;
DEALLOCATE PREPARE v7_stmt;

SET @v7_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'players' AND column_name = 'nickname_key') = 0,
    'ALTER TABLE players ADD COLUMN nickname_key VARCHAR(32) GENERATED ALWAYS AS (LOWER(nickname)) STORED',
    'SELECT 1'
);
PREPARE v7_stmt FROM @v7_sql;
EXECUTE v7_stmt;
DEALLOCATE PREPARE v7_stmt;

SET @v7_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'players' AND column_name = 'oauth_provider') = 0,
    'ALTER TABLE players ADD COLUMN oauth_provider VARCHAR(16) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE v7_stmt FROM @v7_sql;
EXECUTE v7_stmt;
DEALLOCATE PREPARE v7_stmt;

SET @v7_sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'players' AND column_name = 'oauth_subject') = 0,
    'ALTER TABLE players ADD COLUMN oauth_subject VARCHAR(255) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE v7_stmt FROM @v7_sql;
EXECUTE v7_stmt;
DEALLOCATE PREPARE v7_stmt;

SET @v7_sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'players' AND index_name = 'uk_players_nickname') = 0,
    'ALTER TABLE players ADD CONSTRAINT uk_players_nickname UNIQUE (nickname)',
    'SELECT 1'
);
PREPARE v7_stmt FROM @v7_sql;
EXECUTE v7_stmt;
DEALLOCATE PREPARE v7_stmt;

SET @v7_sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'players' AND index_name = 'uk_players_nickname_key') = 0,
    'ALTER TABLE players ADD CONSTRAINT uk_players_nickname_key UNIQUE (nickname_key)',
    'SELECT 1'
);
PREPARE v7_stmt FROM @v7_sql;
EXECUTE v7_stmt;
DEALLOCATE PREPARE v7_stmt;

SET @v7_sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'players' AND index_name = 'uk_players_oauth_identity') = 0,
    'ALTER TABLE players ADD CONSTRAINT uk_players_oauth_identity UNIQUE (oauth_provider, oauth_subject)',
    'SELECT 1'
);
PREPARE v7_stmt FROM @v7_sql;
EXECUTE v7_stmt;
DEALLOCATE PREPARE v7_stmt;
