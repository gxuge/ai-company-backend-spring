SET NAMES utf8mb4;

-- 为探拾后台补充海报、广告位和广告内容管理菜单。
-- 迁移可重复执行：菜单按 URL 去重，管理员授权按 role_id + permission_id 去重。

INSERT INTO sys_permission (
    id,
    parent_id,
    name,
    url,
    component,
    is_route,
    component_name,
    redirect,
    menu_type,
    perms,
    perms_type,
    sort_no,
    always_show,
    icon,
    is_leaf,
    keep_alive,
    hidden,
    hide_tab,
    description,
    create_by,
    create_time,
    update_by,
    update_time,
    del_flag,
    rule_flag,
    status,
    internal_or_external
)
SELECT
    '2081000000000000006',
    parent.id,
    '运营内容管理',
    '/tanshi/adCenter',
    'system/tanshi/adCenter/index',
    1,
    'SystemTanshiAdCenter',
    NULL,
    1,
    NULL,
    '0',
    7.00,
    0,
    'ant-design:picture-outlined',
    1,
    0,
    0,
    0,
    '海报、广告位、广告内容、投放规则和投放数据管理',
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
      WHERE existing.url = '/tanshi/adCenter'
        AND existing.del_flag = 0
  );

INSERT INTO sys_role_permission (
    id,
    role_id,
    permission_id,
    data_rule_ids,
    operate_date,
    operate_ip
)
SELECT
    '2081000000000000106',
    role.id,
    permission.id,
    NULL,
    NOW(),
    '0:0:0:0:0:0:0:1'
FROM sys_role role
JOIN sys_permission permission
  ON permission.url = '/tanshi/adCenter'
 AND permission.del_flag = 0
WHERE role.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

-- 影响范围：仅新增探拾运营内容管理菜单及 admin 角色授权，不修改业务数据。
-- 回滚：
-- DELETE FROM sys_role_permission
-- WHERE permission_id IN (
--     SELECT id FROM sys_permission WHERE url = '/tanshi/adCenter'
-- );
-- DELETE FROM sys_permission WHERE url = '/tanshi/adCenter';
