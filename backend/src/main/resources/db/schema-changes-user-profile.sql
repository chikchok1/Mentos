-- Manual schema changes for server-side user profile.
-- nickname is intentionally not unique; duplicate nicknames are allowed.
-- friend_code is a numeric string and must remain unique when present.

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'nickname'
        ),
        'SELECT ''users.nickname already exists''',
        'ALTER TABLE users ADD COLUMN nickname VARCHAR(30) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'friend_code'
        ),
        'SELECT ''users.friend_code already exists''',
        'ALTER TABLE users ADD COLUMN friend_code VARCHAR(10) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'users'
              AND INDEX_NAME = 'uk_users_friend_code'
        ),
        'SELECT ''uk_users_friend_code already exists''',
        'ALTER TABLE users ADD CONSTRAINT uk_users_friend_code UNIQUE (friend_code)'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verification:
-- SELECT id, email, nickname, friend_code FROM users;
