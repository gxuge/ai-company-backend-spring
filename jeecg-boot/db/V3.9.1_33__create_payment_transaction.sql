CREATE TABLE IF NOT EXISTS member_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL,
    product_id BIGINT NOT NULL,
    order_no VARCHAR(64) UNIQUE NOT NULL,
    amount DECIMAL(10,2),
    payment_channel VARCHAR(50),
    provider VARCHAR(32) NULL COMMENT '真实支付渠道',
    transaction_id VARCHAR(128) NULL COMMENT '渠道交易ID',
    payment_status VARCHAR(32) NOT NULL DEFAULT 'CREATED'
        COMMENT 'CREATED/CREATING/PENDING/SUCCEEDED/FAILED/CANCELED',
    callback_time DATETIME NULL COMMENT '支付回调处理时间',
    status TINYINT DEFAULT 0 COMMENT '0待支付 1成功 2退款',
    pay_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_member_order_user (user_id, created_at),
    KEY idx_member_order_provider_transaction (provider, transaction_id)
) COMMENT='会员订单';

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'member_order'
       AND COLUMN_NAME = 'provider') = 0,
    'ALTER TABLE member_order ADD COLUMN provider VARCHAR(32) NULL COMMENT ''真实支付渠道'' AFTER payment_channel',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'member_order'
       AND COLUMN_NAME = 'transaction_id') = 0,
    'ALTER TABLE member_order ADD COLUMN transaction_id VARCHAR(128) NULL COMMENT ''渠道交易ID'' AFTER provider',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'member_order'
       AND COLUMN_NAME = 'payment_status') = 0,
    'ALTER TABLE member_order ADD COLUMN payment_status VARCHAR(32) NOT NULL DEFAULT ''CREATED'' COMMENT ''CREATED/CREATING/PENDING/SUCCEEDED/FAILED/CANCELED'' AFTER transaction_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'member_order'
       AND COLUMN_NAME = 'callback_time') = 0,
    'ALTER TABLE member_order ADD COLUMN callback_time DATETIME NULL COMMENT ''支付回调处理时间'' AFTER payment_status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*)
     FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'member_order'
       AND INDEX_NAME = 'idx_member_order_provider_transaction') = 0,
    'ALTER TABLE member_order ADD KEY idx_member_order_provider_transaction (provider, transaction_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '会员订单ID',
    provider VARCHAR(32) NOT NULL COMMENT 'STRIPE/PAYPAL',
    transaction_id VARCHAR(128) NULL COMMENT '渠道交易ID',
    payment_intent_id VARCHAR(128) NULL COMMENT '渠道支付意图或支付订单ID',
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(32) NOT NULL COMMENT 'CREATING/PENDING/SUCCEEDED/FAILED/CANCELED',
    raw_response LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_payment_transaction_order (order_id, id),
    UNIQUE KEY uk_payment_provider_intent (provider, payment_intent_id),
    UNIQUE KEY uk_payment_provider_transaction (provider, transaction_id)
) COMMENT='第三方支付流水';
