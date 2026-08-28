SET NAMES utf8mb4;

-- 独立补丁：为现有数据库补充默认活动任务。
-- 本脚本不属于 Jeecg Boot 版本迁移，不需要重新执行完整数据库基线。

INSERT INTO activity_task (
    task_name, task_type, task_category, description,
    condition_type, condition_value, reward_type, reward_value,
    reward_claim_mode, start_time, end_time, status, sort,
    created_at, updated_at
)
SELECT
    '每日与AI角色聊天10次', 'TASK', 'DAILY', '每天与AI角色完成10次有效对话',
    'CHAT_COUNT', 10, 'STAR_DIAMOND', 5,
    'AUTO', NULL, NULL, 'ENABLED', 10, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM activity_task
    WHERE task_type = 'TASK'
      AND task_category = 'DAILY'
      AND condition_type = 'CHAT_COUNT'
      AND condition_value = 10
);

INSERT INTO activity_task (
    task_name, task_type, task_category, description,
    condition_type, condition_value, reward_type, reward_value,
    reward_claim_mode, start_time, end_time, status, sort,
    created_at, updated_at
)
SELECT
    '每日生成角色图片', 'TASK', 'DAILY', '每天成功生成一张角色图片',
    'ROLE_IMAGE_GENERATE', 1, 'STAR_DIAMOND', 10,
    'AUTO', NULL, NULL, 'ENABLED', 20, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM activity_task
    WHERE task_type = 'TASK'
      AND task_category = 'DAILY'
      AND condition_type = 'ROLE_IMAGE_GENERATE'
      AND condition_value = 1
);

INSERT INTO activity_task (
    task_name, task_type, task_category, description,
    condition_type, condition_value, reward_type, reward_value,
    reward_claim_mode, start_time, end_time, status, sort,
    created_at, updated_at
)
SELECT
    '每日生成故事背景', 'TASK', 'DAILY', '每天成功生成一张故事场景背景图片',
    'STORY_BACKGROUND_GENERATE', 1, 'STAR_DIAMOND', 10,
    'AUTO', NULL, NULL, 'ENABLED', 30, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM activity_task
    WHERE task_type = 'TASK'
      AND task_category = 'DAILY'
      AND condition_type = 'STORY_BACKGROUND_GENERATE'
      AND condition_value = 1
);

INSERT INTO activity_task (
    task_name, task_type, task_category, description,
    condition_type, condition_value, reward_type, reward_value,
    reward_claim_mode, start_time, end_time, status, sort,
    created_at, updated_at
)
SELECT
    '每日创建一个角色', 'TASK', 'DAILY', '每天成功创建一个角色',
    'ROLE_CREATE', 1, 'STAR_DIAMOND', 20,
    'AUTO', NULL, NULL, 'ENABLED', 40, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM activity_task
    WHERE task_type = 'TASK'
      AND task_category = 'DAILY'
      AND condition_type = 'ROLE_CREATE'
      AND condition_value = 1
);

INSERT INTO activity_task (
    task_name, task_type, task_category, description,
    condition_type, condition_value, reward_type, reward_value,
    reward_claim_mode, start_time, end_time, status, sort,
    created_at, updated_at
)
SELECT
    '每日创建一个故事', 'TASK', 'DAILY', '每天成功创建一个故事',
    'STORY_CREATE', 1, 'STAR_DIAMOND', 20,
    'AUTO', NULL, NULL, 'ENABLED', 50, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM activity_task
    WHERE task_type = 'TASK'
      AND task_category = 'DAILY'
      AND condition_type = 'STORY_CREATE'
      AND condition_value = 1
);

INSERT INTO activity_task (
    task_name, task_type, task_category, description,
    condition_type, condition_value, reward_type, reward_value,
    reward_claim_mode, start_time, end_time, status, sort,
    created_at, updated_at
)
SELECT
    '每日故事互动5次', 'TASK', 'DAILY', '每天在故事会话中完成5次有效互动',
    'STORY_INTERACTION_COUNT', 5, 'STAR_DIAMOND', 10,
    'AUTO', NULL, NULL, 'ENABLED', 60, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM activity_task
    WHERE task_type = 'TASK'
      AND task_category = 'DAILY'
      AND condition_type = 'STORY_INTERACTION_COUNT'
      AND condition_value = 5
);

INSERT INTO activity_task (
    task_name, task_type, task_category, description,
    condition_type, condition_value, reward_type, reward_value,
    reward_claim_mode, start_time, end_time, status, sort,
    created_at, updated_at
)
SELECT
    '每周累计聊天100次', 'TASK', 'WEEKLY', '每周与AI角色累计完成100次有效对话',
    'CHAT_COUNT', 100, 'STAR_DIAMOND', 20,
    'AUTO', NULL, NULL, 'ENABLED', 70, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM activity_task
    WHERE task_type = 'TASK'
      AND task_category = 'WEEKLY'
      AND condition_type = 'CHAT_COUNT'
      AND condition_value = 100
);

-- 影响范围：仅补充缺失的默认活动任务，不覆盖后台已有等价任务。
-- 回滚：按 condition_type、task_category 和 condition_value 精确停用或删除上述任务。
