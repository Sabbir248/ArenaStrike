ALTER TABLE match_history ADD COLUMN ended_at TIMESTAMP NULL;

UPDATE match_history
SET ended_at = COALESCE(ended_at, played_at)
WHERE ended_at IS NULL;

ALTER TABLE match_history
    MODIFY COLUMN ended_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
