SET NAMES utf8mb4;

CREATE TABLE activity_sign_milestone_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '签到任务ID',
    milestone_day INT NOT NULL COMMENT '周期内里程碑天数：1-7',
    reward_type VARCHAR(32) NOT NULL COMMENT '奖励类型',
    reward_value BIGINT NOT NULL COMMENT '奖励数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sign_milestone_task_day (task_id, milestone_day),
    KEY idx_sign_milestone_status_task_day (status, task_id, milestone_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到周期里程碑奖励规则';

ALTER TABLE user_sign_record
    ADD COLUMN cycle_day INT NOT NULL DEFAULT 1
        COMMENT '当前7天周期内天数：1-7'
        AFTER continuous_days,
    ADD COLUMN milestone_day INT NULL DEFAULT NULL
        COMMENT '本次命中的里程碑天数'
        AFTER extra_reward_amount,
    ADD COLUMN milestone_reward_amount BIGINT NOT NULL DEFAULT 0
        COMMENT '签到里程碑奖励'
        AFTER milestone_day,
    ADD COLUMN milestone_points_transaction_no VARCHAR(64) NULL DEFAULT NULL
        COMMENT '签到里程碑积分流水号'
        AFTER points_transaction_no;

INSERT IGNORE INTO activity_sign_milestone_rule (
    task_id, milestone_day, reward_type, reward_value,
    status, created_at, updated_at
)
SELECT
    task.id,
    milestone.milestone_day,
    'STAR_DIAMOND',
    milestone.reward_value,
    1,
    NOW(),
    NOW()
FROM activity_task task
CROSS JOIN (
    SELECT 4 AS milestone_day, 10 AS reward_value
    UNION ALL
    SELECT 7 AS milestone_day, 20 AS reward_value
) milestone
WHERE task.task_type = 'SIGN'
  AND task.task_category = 'DAILY';

-- 影响范围：签到记录新增周期与里程碑字段；旧记录按默认周期第1天、无里程碑奖励兼容。
-- 默认数据：所有现有每日签到任务补充第4天10星钻、第7天20星钻，已有规则不覆盖。
-- 回滚前需确认已停止里程碑发奖并备份规则与流水关联数据：
-- ALTER TABLE user_sign_record
--     DROP COLUMN milestone_points_transaction_no,
--     DROP COLUMN milestone_reward_amount,
--     DROP COLUMN milestone_day,
--     DROP COLUMN cycle_day;
-- DROP TABLE activity_sign_milestone_rule;
