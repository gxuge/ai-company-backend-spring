SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS activity_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_type VARCHAR(32) NOT NULL COMMENT 'SIGN/TASK/ACHIEVEMENT/EVENT',
    task_category VARCHAR(32) NOT NULL COMMENT 'DAILY/WEEKLY/LONG_TERM',
    description VARCHAR(500) NULL COMMENT '任务描述',
    condition_type VARCHAR(32) NOT NULL COMMENT 'LOGIN/CHAT_COUNT/ROLE_CREATE/STORY_CREATE/IMAGE_GENERATE/VOICE_USE',
    condition_value BIGINT NOT NULL COMMENT '完成目标数量',
    reward_type VARCHAR(32) NOT NULL COMMENT 'STAR_DIAMOND/ITEM/TITLE/AVATAR_FRAME',
    reward_value BIGINT NOT NULL COMMENT '基础奖励数量',
    start_time DATETIME NULL COMMENT '开始时间',
    end_time DATETIME NULL COMMENT '结束时间',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_activity_task_active (status, start_time, end_time),
    KEY idx_activity_task_type_category (task_type, task_category, sort, id)
) COMMENT='活动任务';

CREATE TABLE IF NOT EXISTS user_task_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    cycle_key VARCHAR(32) NOT NULL COMMENT 'DAILY日期/WEEKLY周/LONG长期',
    current_value BIGINT NOT NULL DEFAULT 0 COMMENT '当前进度',
    target_value BIGINT NOT NULL COMMENT '目标进度快照',
    status VARCHAR(16) NOT NULL DEFAULT 'DOING' COMMENT 'DOING/COMPLETED',
    reward_status VARCHAR(16) NOT NULL DEFAULT 'UNCLAIMED' COMMENT 'UNCLAIMED/CLAIMED',
    complete_time DATETIME NULL COMMENT '完成时间',
    reward_time DATETIME NULL COMMENT '领取时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_task_cycle (user_id, task_id, cycle_key),
    KEY idx_user_task_status (user_id, status, reward_status, updated_at),
    KEY idx_task_progress_task (task_id, cycle_key, status)
) COMMENT='用户活动任务进度';

CREATE TABLE IF NOT EXISTS user_sign_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    task_id BIGINT NOT NULL COMMENT '签到任务ID',
    sign_date DATE NOT NULL COMMENT '签到日期',
    continuous_days INT NOT NULL DEFAULT 1 COMMENT '连续签到天数',
    base_reward_amount BIGINT NOT NULL DEFAULT 0 COMMENT '基础星钻',
    extra_reward_amount BIGINT NOT NULL DEFAULT 0 COMMENT '会员加成星钻',
    reward_amount BIGINT NOT NULL DEFAULT 0 COMMENT '最终奖励星钻',
    points_transaction_no VARCHAR(64) NULL COMMENT '积分流水号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_sign_date (user_id, sign_date),
    KEY idx_user_sign_created (user_id, created_at, id)
) COMMENT='用户签到记录';

CREATE TABLE IF NOT EXISTS activity_reward_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    task_id BIGINT NULL COMMENT '任务ID',
    reward_type VARCHAR(32) NOT NULL COMMENT 'STAR_DIAMOND/ITEM/TITLE/AVATAR_FRAME',
    base_reward_value BIGINT NOT NULL COMMENT '基础奖励',
    extra_reward_value BIGINT NOT NULL DEFAULT 0 COMMENT '会员额外奖励',
    reward_value BIGINT NOT NULL COMMENT '最终奖励',
    source_type VARCHAR(32) NOT NULL COMMENT 'SIGN/TASK/ACHIEVEMENT/EVENT',
    source_id VARCHAR(64) NOT NULL COMMENT '来源记录ID',
    member_level VARCHAR(16) NOT NULL COMMENT 'NORMAL/VIP/SVIP',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '奖励幂等Key',
    points_transaction_no VARCHAR(64) NULL COMMENT '积分流水号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_reward_idempotency (user_id, idempotency_key),
    KEY idx_activity_reward_user_created (user_id, created_at, id),
    KEY idx_activity_reward_task (task_id, created_at)
) COMMENT='活动奖励记录';

CREATE TABLE IF NOT EXISTS activity_task_reward_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    member_level VARCHAR(16) NOT NULL COMMENT 'NORMAL/VIP/SVIP',
    extra_reward_type VARCHAR(32) NOT NULL COMMENT '额外奖励类型',
    extra_reward_value BIGINT NOT NULL DEFAULT 0 COMMENT '额外奖励数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_task_member_level (task_id, member_level),
    KEY idx_activity_reward_rule_status (task_id, status)
) COMMENT='活动任务会员奖励加成规则';

CREATE TABLE IF NOT EXISTS activity_progress_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL COMMENT '用户ID',
    condition_type VARCHAR(32) NOT NULL COMMENT '行为类型',
    biz_id VARCHAR(128) NOT NULL COMMENT '业务幂等ID',
    count_value BIGINT NOT NULL COMMENT '本次增加数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_progress_event (user_id, condition_type, biz_id),
    KEY idx_activity_progress_created (user_id, created_at, id)
) COMMENT='活动任务行为去重事件';

-- 回滚顺序：
-- DROP TABLE IF EXISTS activity_progress_event;
-- DROP TABLE IF EXISTS activity_task_reward_rule;
-- DROP TABLE IF EXISTS activity_reward_record;
-- DROP TABLE IF EXISTS user_sign_record;
-- DROP TABLE IF EXISTS user_task_progress;
-- DROP TABLE IF EXISTS activity_task;
