SET NAMES utf8mb4;

-- 为探拾后台补充积分与统一账单管理菜单。
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
    '2081000000000000001',
    parent.id,
    '积分管理',
    '/tanshi/points',
    'system/tanshi/points/index',
    1,
    '',
    NULL,
    1,
    NULL,
    '0',
    2.00,
    0,
    'ant-design:gift-outlined',
    1,
    0,
    0,
    0,
    '探拾积分账户、流水、充值商品和会员赠送规则管理',
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
      WHERE existing.url = '/tanshi/points'
        AND existing.del_flag = 0
  );

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
    '2081000000000000002',
    parent.id,
    '账单管理',
    '/tanshi/billing',
    'system/tanshi/billing/index',
    1,
    '',
    NULL,
    1,
    NULL,
    '0',
    3.00,
    0,
    'ant-design:account-book-outlined',
    1,
    0,
    0,
    0,
    '探拾平台现金与积分统一账单管理',
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
      WHERE existing.url = '/tanshi/billing'
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
    '2081000000000000101',
    role.id,
    '2081000000000000001',
    NULL,
    NOW(),
    '0:0:0:0:0:0:0:1'
FROM sys_role role
WHERE role.role_code = 'admin'
  AND EXISTS (
      SELECT 1
      FROM sys_permission permission
      WHERE permission.id = '2081000000000000001'
        AND permission.del_flag = 0
  )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = '2081000000000000001'
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
    '2081000000000000102',
    role.id,
    '2081000000000000002',
    NULL,
    NOW(),
    '0:0:0:0:0:0:0:1'
FROM sys_role role
WHERE role.role_code = 'admin'
  AND EXISTS (
      SELECT 1
      FROM sys_permission permission
      WHERE permission.id = '2081000000000000002'
        AND permission.del_flag = 0
  )
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = '2081000000000000002'
  );

-- 回滚：
-- DELETE FROM sys_role_permission
-- WHERE permission_id IN ('2081000000000000001', '2081000000000000002');
-- DELETE FROM sys_permission
-- WHERE id IN ('2081000000000000001', '2081000000000000002');
