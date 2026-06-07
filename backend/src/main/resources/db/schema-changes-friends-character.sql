CREATE TABLE IF NOT EXISTS user_equipped_items (
    user_id BIGINT NOT NULL,
    slot VARCHAR(50) NOT NULL,
    item_id VARCHAR(255) NOT NULL,
    layer_order INT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, slot),
    INDEX idx_user_equipped_items_user_order (user_id, layer_order),
    CONSTRAINT fk_user_equipped_items_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- Apply only if these columns are not already present in users.
-- ALTER TABLE users ADD COLUMN spending_visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE';
-- ALTER TABLE users ADD COLUMN character_visibility VARCHAR(30) NOT NULL DEFAULT 'FRIENDS';
