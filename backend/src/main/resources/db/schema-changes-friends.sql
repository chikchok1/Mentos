CREATE TABLE IF NOT EXISTS friend_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_friend_requests_receiver_status (receiver_id, status),
    INDEX idx_friend_requests_requester_status (requester_id, status),
    CONSTRAINT fk_friend_requests_requester
        FOREIGN KEY (requester_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_friend_requests_receiver
        FOREIGN KEY (receiver_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS friends (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_friends_user_friend (user_id, friend_id),
    INDEX idx_friends_user_id (user_id),
    INDEX idx_friends_friend_id (friend_id),
    CONSTRAINT fk_friends_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_friends_friend
        FOREIGN KEY (friend_id) REFERENCES users(id)
        ON DELETE CASCADE
);

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = @schema_name
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'spending_visibility'
        ),
        'SELECT ''users.spending_visibility already exists''',
        'ALTER TABLE users ADD COLUMN spending_visibility VARCHAR(30) NOT NULL DEFAULT ''PRIVATE'''
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
              AND COLUMN_NAME = 'character_visibility'
        ),
        'SELECT ''users.character_visibility already exists''',
        'ALTER TABLE users ADD COLUMN character_visibility VARCHAR(30) NOT NULL DEFAULT ''FRIENDS'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
