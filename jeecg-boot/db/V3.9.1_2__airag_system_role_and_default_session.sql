-- =========================================================
-- Airag: 系统内置角色 + 用户默认系统会话
-- 目标：
-- 1) 预置内置系统角色（SYSTEM_ASSISTANT）
-- 2) 保证每个用户可初始化默认系统会话（幂等）
-- =========================================================

-- 1) 角色表新增内置角色字段
ALTER TABLE ts_role_info
    ADD COLUMN role_code VARCHAR(64) DEFAULT NULL COMMENT '角色编码，内置角色固定值',
    ADD COLUMN is_builtin TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否内置角色：1=内置，0=普通';

CREATE UNIQUE INDEX uk_ts_role_info_role_code ON ts_role_info (role_code);

-- 2) 会话表新增默认系统会话幂等键
ALTER TABLE ts_chat_session
    ADD COLUMN system_session_key VARCHAR(32) DEFAULT NULL COMMENT '默认系统会话幂等键';

CREATE UNIQUE INDEX uk_ts_chat_session_user_system_key_status
    ON ts_chat_session (user_id, system_session_key, session_status);

-- 3) 插入内置系统角色（优先挂在 admin 用户下；无 admin 时挂到第一个用户）
INSERT INTO ts_role_info (
    user_id,
    role_code,
    is_builtin,
    role_name,
    role_subtitle,
    intro_text,
    persona_text,
    background_story,
    is_public,
    status,
    created_at,
    updated_at
)
SELECT t.user_id,
       'SYSTEM_ASSISTANT',
       1,
       '系统助手',
       '默认系统对话角色',
       '用于承接每个用户的默认系统会话',
       '你是系统助手，负责提供通用问答与引导。',
       '内置角色，随系统初始化创建。',
       0,
       1,
       NOW(),
       NOW()
FROM (
         SELECT su.id AS user_id
         FROM sys_user su
         ORDER BY CASE WHEN su.username = 'admin' THEN 0 ELSE 1 END, su.create_time ASC, su.id ASC
         LIMIT 1
     ) t
WHERE t.user_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM ts_role_info r WHERE r.role_code = 'SYSTEM_ASSISTANT'
);

-- 4) 为历史用户补齐默认系统会话（幂等）
INSERT INTO ts_chat_session (
    user_id,
    session_type,
    session_title,
    target_role_id,
    session_status,
    ext_json,
    system_session_key,
    created_at,
    updated_at
)
SELECT su.id,
       'single',
       '系统',
       sr.id,
       1,
       '{"builtIn":true,"source":"migration"}',
       'DEFAULT_SYSTEM',
       NOW(),
       NOW()
FROM sys_user su
         JOIN ts_role_info sr ON sr.role_code = 'SYSTEM_ASSISTANT'
         LEFT JOIN ts_chat_session s
                   ON s.user_id = su.id
                       AND s.system_session_key = 'DEFAULT_SYSTEM'
                       AND s.session_status != 0
WHERE s.id IS NULL;
