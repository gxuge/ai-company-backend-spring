SET NAMES utf8mb4;

-- 为探拾后台补充角色与故事作品内容审核菜单。
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
    '2081000000000000005',
    parent.id,
    '作品内容审核',
    '/tanshi/workReview',
    'system/tanshi/workReview/index',
    1,
    'SystemTanshiWorkReview',
    NULL,
    1,
    NULL,
    '0',
    4.00,
    0,
    'ant-design:safety-certificate-outlined',
    1,
    0,
    0,
    0,
    '角色内容、角色图片、故事内容和故事图片审核',
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
      WHERE existing.url = '/tanshi/workReview'
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
    '2081000000000000105',
    role.id,
    '2081000000000000005',
    NULL,
    NOW(),
    '0:0:0:0:0:0:0:1'
FROM sys_role role
WHERE role.role_code = 'admin'
  AND EXISTS (
      SELECT 1
      FROM sys_permission permission
      WHERE permission.id = '2081000000000000005'
        AND permission.del_flag = 0
  )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = '2081000000000000005'
  );

-- 影响范围：仅新增探拾作品内容审核菜单及 admin 角色授权，不修改业务数据。
-- 回滚：
-- DELETE FROM sys_role_permission WHERE permission_id = '2081000000000000005';
-- DELETE FROM sys_permission WHERE id = '2081000000000000005';
