-- Manual schema changes for user stats, notification parse logs, and inventory uniqueness.
-- Check duplicate inventory rows before adding the unique constraint:
SELECT user_id, item_id, COUNT(*) AS duplicate_count
FROM user_owned_items
GROUP BY user_id, item_id
HAVING COUNT(*) > 1;

-- If duplicates exist and the table has only user_id and item_id columns, review and deduplicate first.
-- Example deduplication approach:
-- CREATE TABLE user_owned_items_dedup AS
-- SELECT DISTINCT user_id, item_id
-- FROM user_owned_items;

-- After reviewing user_owned_items_dedup, replace the original table inside a maintenance window.
-- RENAME TABLE user_owned_items TO user_owned_items_backup,
--              user_owned_items_dedup TO user_owned_items;

ALTER TABLE users
    ADD COLUMN total_xp INT NOT NULL DEFAULT 0,
    ADD COLUMN level INT NOT NULL DEFAULT 1,
    ADD COLUMN current_xp INT NOT NULL DEFAULT 0,
    ADD COLUMN next_level_xp INT NOT NULL DEFAULT 50,
    ADD COLUMN monthly_budget BIGINT NOT NULL DEFAULT 1500000,
    ADD COLUMN job VARCHAR(64) NOT NULL DEFAULT 'beginner',
    ADD COLUMN job_reason VARCHAR(512) NOT NULL DEFAULT '이번 달 지출 내역이 없어 모험가로 시작했어요.',
    ADD COLUMN job_month VARCHAR(7) NOT NULL DEFAULT '1970-01';

CREATE TABLE notification_parse_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    diagnostic_id VARCHAR(512) NOT NULL,
    package_name VARCHAR(128) NOT NULL,
    title VARCHAR(512) NULL,
    raw_text LONGTEXT NULL,
    status VARCHAR(40) NOT NULL,
    failure_reason VARCHAR(512) NULL,
    parsed_amount BIGINT NULL,
    parsed_merchant VARCHAR(255) NULL,
    parsed_occurred_at DATETIME(6) NULL,
    client_transaction_id VARCHAR(512) NULL,
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_notification_parse_logs_user_created (user_id, created_at),
    INDEX idx_notification_parse_logs_user_received (user_id, received_at),
    INDEX idx_notification_parse_logs_status (status)
);

ALTER TABLE notification_parse_logs
    MODIFY COLUMN received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

ALTER TABLE user_owned_items
    ADD CONSTRAINT uk_user_owned_items_user_item UNIQUE (user_id, item_id);
