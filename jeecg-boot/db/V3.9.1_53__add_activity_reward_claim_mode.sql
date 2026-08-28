SET NAMES utf8mb4;

ALTER TABLE activity_task
    ADD COLUMN reward_claim_mode VARCHAR(16) NOT NULL DEFAULT 'MANUAL'
        COMMENT '奖励领取模式：MANUAL手动/AUTO自动'
        AFTER reward_value;

ALTER TABLE user_task_progress
    MODIFY COLUMN reward_status VARCHAR(16) NOT NULL DEFAULT 'UNCLAIMED'
        COMMENT 'UNCLAIMED未领取/GRANTING发放中/CLAIMED已领取';

-- 影响范围：旧任务默认保持 MANUAL，不改变既有领取行为。
-- 回滚前需先将 AUTO 任务改回 MANUAL，并确认不存在 GRANTING 状态：
-- ALTER TABLE user_task_progress
--     MODIFY COLUMN reward_status VARCHAR(16) NOT NULL DEFAULT 'UNCLAIMED'
--         COMMENT 'UNCLAIMED/CLAIMED';
-- ALTER TABLE activity_task DROP COLUMN reward_claim_mode;
