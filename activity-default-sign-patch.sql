SET NAMES utf8mb4;

-- 独立补丁：为现有数据库补充默认每日签到任务与七天周期里程碑奖励。
-- 本脚本不预置用户签到记录，不覆盖已有等价配置。

INSERT INTO activity_task (
    task_name, task_type, task_category, description,
    condition_type, condition_value, reward_type, reward_value,
    reward_claim_mode, start_time, end_time, status, sort,
    created_at, updated_at
)
SELECT
    '每日签到', 'SIGN', 'DAILY', '每日签到获得 10 星钻',
    'LOGIN', 1, 'STAR_DIAMOND', 10,
    'AUTO', NULL, NULL, 'ENABLED', 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM activity_task
    WHERE task_type = 'SIGN'
      AND task_category = 'DAILY'
      AND status = 'ENABLED'
);

INSERT IGNORE INTO activity_sign_milestone_rule (
    task_id, milestone_day, reward_type, reward_value,
    status, created_at, updated_at
)
SELECT
    task.id, milestone.milestone_day, 'STAR_DIAMOND',
    milestone.reward_value, 1, NOW(), NOW()
FROM activity_task task
CROSS JOIN (
    SELECT 4 AS milestone_day, 10 AS reward_value
    UNION ALL
    SELECT 7 AS milestone_day, 20 AS reward_value
) milestone
WHERE task.task_type = 'SIGN'
  AND task.task_category = 'DAILY'
  AND task.status = 'ENABLED';

-- 影响范围：仅补充缺失的默认签到配置，不写入用户签到记录。
-- 回滚：先停用默认签到任务，再按 task_id 删除对应第 4/7 天里程碑规则。
