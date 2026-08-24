SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS reward_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(128) NOT NULL COMMENT '业务确定的奖励事件幂等ID',
    event_type VARCHAR(48) NOT NULL COMMENT 'SIGN_COMPLETED/TASK_REWARD_RECEIVED/MEMBER_ACTIVATED',
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    biz_id VARCHAR(128) NOT NULL COMMENT '关联业务ID',
    payload_json LONGTEXT NOT NULL COMMENT '事件负载JSON',
    result_json LONGTEXT NULL COMMENT '执行结果JSON',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已执行次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大执行次数',
    last_error_code VARCHAR(64) NULL COMMENT '最近机器错误码',
    last_error_message VARCHAR(500) NULL COMMENT '最近错误信息',
    processed_at DATETIME NULL COMMENT '成功处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reward_event_id (event_id),
    KEY idx_reward_event_retry (status, retry_count, updated_at),
    KEY idx_reward_event_user_created (user_id, created_at, id),
    KEY idx_reward_event_biz (event_type, biz_id)
) COMMENT='统一奖励事件';

-- 影响范围：仅新增奖励事件审计与重试状态，不迁移或修改现有积分、活动、会员数据。
-- 回滚：
-- DROP TABLE IF EXISTS reward_event;
