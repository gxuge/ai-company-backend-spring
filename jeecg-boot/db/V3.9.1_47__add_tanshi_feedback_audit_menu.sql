SET NAMES utf8mb4;

-- 为探拾后台补充反馈与评论审核菜单及按钮权限。
-- 迁移可重复执行：菜单按 URL 去重，按钮按权限码去重，管理员授权按角色和权限去重。

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
    '2081000000000000004',
    parent.id,
    '反馈审核',
    '/tanshi/feedbackAudit',
    'system/tanshi/feedbackAudit/index',
    1,
    '',
    NULL,
    1,
    NULL,
    '0',
    5.00,
    0,
    'ant-design:audit-outlined',
    1,
    0,
    0,
    0,
    '反馈、评论回复和追加内容统一审核及运营处理',
    'admin',
    NOW(),
    'admin',
    NOW(),
    0,
    0,
    NULL,
    0
FROM sys_permission parent
WHERE parent.id = '2063098067229384705'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission existing
      WHERE existing.url = '/tanshi/feedbackAudit'
        AND existing.del_flag = 0
  );

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
    '2081000000000000014',
    parent.id,
    '审核反馈内容',
    NULL,
    NULL,
    0,
    '',
    NULL,
    2,
    'feedback:admin:audit',
    '1',
    1.00,
    0,
    NULL,
    1,
    0,
    0,
    0,
    '查询审核队列并通过或驳回反馈内容',
    'admin',
    NOW(),
    'admin',
    NOW(),
    0,
    0,
    NULL,
    0
FROM sys_permission parent
WHERE parent.url = '/tanshi/feedbackAudit'
  AND parent.del_flag = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission existing
      WHERE existing.perms = 'feedback:admin:audit'
        AND existing.del_flag = 0
  );

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
    '2081000000000000015',
    parent.id,
    '更新反馈状态',
    NULL,
    NULL,
    0,
    '',
    NULL,
    2,
    'feedback:admin:status',
    '1',
    2.00,
    0,
    NULL,
    1,
    0,
    0,
    0,
    '更新反馈业务处理状态',
    'admin',
    NOW(),
    'admin',
    NOW(),
    0,
    0,
    NULL,
    0
FROM sys_permission parent
WHERE parent.url = '/tanshi/feedbackAudit'
  AND parent.del_flag = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission existing
      WHERE existing.perms = 'feedback:admin:status'
        AND existing.del_flag = 0
  );

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
    '2081000000000000016',
    parent.id,
    '发布官方回复',
    NULL,
    NULL,
    0,
    '',
    NULL,
    2,
    'feedback:admin:reply',
    '1',
    3.00,
    0,
    NULL,
    1,
    0,
    0,
    0,
    '向反馈发布审核通过的官方回复',
    'admin',
    NOW(),
    'admin',
    NOW(),
    0,
    0,
    NULL,
    0
FROM sys_permission parent
WHERE parent.url = '/tanshi/feedbackAudit'
  AND parent.del_flag = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_permission existing
      WHERE existing.perms = 'feedback:admin:reply'
        AND existing.del_flag = 0
  );

INSERT INTO sys_role_permission (
    id, role_id, permission_id, data_rule_ids, operate_date, operate_ip
)
SELECT
    '2081000000000000104',
    role.id,
    permission.id,
    NULL,
    NOW(),
    '0:0:0:0:0:0:0:1'
FROM sys_role role
JOIN sys_permission permission
  ON permission.url = '/tanshi/feedbackAudit'
 AND permission.del_flag = 0
WHERE role.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT INTO sys_role_permission (
    id, role_id, permission_id, data_rule_ids, operate_date, operate_ip
)
SELECT
    CASE permission.perms
        WHEN 'feedback:admin:audit' THEN '2081000000000000114'
        WHEN 'feedback:admin:status' THEN '2081000000000000115'
        ELSE '2081000000000000116'
    END,
    role.id,
    permission.id,
    NULL,
    NOW(),
    '0:0:0:0:0:0:0:1'
FROM sys_role role
JOIN sys_permission permission
  ON permission.perms IN (
      'feedback:admin:audit',
      'feedback:admin:status',
      'feedback:admin:reply'
  )
 AND permission.del_flag = 0
WHERE role.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

-- 回滚：
-- DELETE FROM sys_role_permission
-- WHERE permission_id IN (
--     SELECT id FROM sys_permission
--     WHERE url = '/tanshi/feedbackAudit'
--        OR perms IN ('feedback:admin:audit', 'feedback:admin:status', 'feedback:admin:reply')
-- );
-- DELETE FROM sys_permission
-- WHERE url = '/tanshi/feedbackAudit'
--    OR perms IN ('feedback:admin:audit', 'feedback:admin:status', 'feedback:admin:reply');
