SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_points_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    balance BIGINT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
    total_income BIGINT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    total_expense BIGINT NOT NULL DEFAULT 0 COMMENT '累计消费积分',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_points_account_user (user_id)
) COMMENT='用户积分账户';

CREATE TABLE IF NOT EXISTS points_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_no VARCHAR(64) NOT NULL COMMENT '积分流水号',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    direction VARCHAR(16) NOT NULL COMMENT 'INCOME/EXPENSE',
    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    biz_id VARCHAR(128) NULL COMMENT '关联业务ID',
    amount BIGINT NOT NULL COMMENT '本次变化积分',
    before_balance BIGINT NOT NULL COMMENT '变动前余额',
    after_balance BIGINT NOT NULL COMMENT '变动后余额',
    status VARCHAR(16) NOT NULL COMMENT 'PENDING/SUCCESS/FAILED/REFUNDED/CANCELED',
    description VARCHAR(500) NULL COMMENT '流水说明',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等Key',
    original_transaction_no VARCHAR(64) NULL COMMENT '原消费流水号',
    operator_id VARCHAR(32) NULL COMMENT '后台操作人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_points_transaction_no (transaction_no),
    UNIQUE KEY uk_points_user_idempotency (user_id, idempotency_key),
    KEY idx_points_user_created (user_id, created_at, id),
    KEY idx_points_biz (biz_type, biz_id),
    KEY idx_points_original_transaction (original_transaction_no, status)
) COMMENT='积分流水';

CREATE TABLE IF NOT EXISTS points_recharge_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '积分商品名称',
    points BIGINT NOT NULL COMMENT '购买积分',
    gift_points BIGINT NOT NULL DEFAULT 0 COMMENT '赠送积分',
    original_amount DECIMAL(10,2) NOT NULL COMMENT '原价',
    actual_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT '币种',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_points_product_status_sort (status, sort, id)
) COMMENT='积分充值商品';

CREATE TABLE IF NOT EXISTS points_recharge_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL COMMENT '充值订单号',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '积分商品ID',
    points BIGINT NOT NULL COMMENT '购买积分',
    gift_points BIGINT NOT NULL DEFAULT 0 COMMENT '赠送积分',
    original_amount DECIMAL(10,2) NOT NULL COMMENT '原价',
    actual_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    currency VARCHAR(10) NOT NULL COMMENT '币种',
    payment_channel VARCHAR(32) NOT NULL COMMENT '支付渠道',
    status VARCHAR(32) NOT NULL COMMENT 'CREATING/PENDING/SUCCEEDED/FAILED/CANCELED',
    transaction_id VARCHAR(128) NULL COMMENT '渠道交易ID',
    points_transaction_no VARCHAR(64) NULL COMMENT '积分入账流水号',
    pay_time DATETIME NULL COMMENT '支付时间',
    callback_time DATETIME NULL COMMENT '回调处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_points_recharge_order_no (order_no),
    KEY idx_points_recharge_user_created (user_id, created_at, id),
    KEY idx_points_recharge_status_created (status, created_at)
) COMMENT='积分充值订单';

CREATE TABLE IF NOT EXISTS points_recharge_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '积分充值订单ID',
    provider VARCHAR(32) NOT NULL COMMENT 'STRIPE/PAYPAL',
    transaction_id VARCHAR(128) NULL COMMENT '渠道交易ID',
    payment_intent_id VARCHAR(128) NULL COMMENT '渠道支付意图或支付订单ID',
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(32) NOT NULL COMMENT 'CREATING/PENDING/SUCCEEDED/FAILED/CANCELED',
    raw_response LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_points_recharge_payment_order (order_id, id),
    UNIQUE KEY uk_points_recharge_provider_intent (provider, payment_intent_id),
    UNIQUE KEY uk_points_recharge_provider_transaction (provider, transaction_id)
) COMMENT='积分充值支付流水';

CREATE TABLE IF NOT EXISTS member_points_gift_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL COMMENT '会员等级ID',
    product_id BIGINT NOT NULL DEFAULT 0 COMMENT '会员套餐ID，0表示等级默认规则',
    gift_points BIGINT NOT NULL COMMENT '赠送积分',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_member_points_gift_scope (plan_id, product_id),
    KEY idx_member_points_gift_plan (plan_id, status)
) COMMENT='会员开通积分赠送规则';

-- 回滚顺序：
-- DROP TABLE IF EXISTS member_points_gift_rule;
-- DROP TABLE IF EXISTS points_recharge_payment;
-- DROP TABLE IF EXISTS points_recharge_order;
-- DROP TABLE IF EXISTS points_recharge_product;
-- DROP TABLE IF EXISTS points_transaction;
-- DROP TABLE IF EXISTS user_points_account;
