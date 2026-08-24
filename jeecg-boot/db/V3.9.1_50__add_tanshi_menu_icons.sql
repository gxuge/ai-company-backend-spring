SET NAMES utf8mb4;

-- 为探拾应用管理下已有菜单补充图标。
-- 仅更新菜单图标，不新增菜单、不修改菜单层级和权限。

UPDATE sys_permission
SET
    icon = CASE url
        WHEN '/tanshi/storyPublic' THEN 'ant-design:book-outlined'
        WHEN '/tanshi/rolePublic' THEN 'ant-design:user-outlined'
        WHEN '/tanshi/memberconfig' THEN 'ant-design:crown-outlined'
        WHEN '/tanshi/usermembership' THEN 'ant-design:team-outlined'
        WHEN '/tanshi/payment' THEN 'ant-design:credit-card-outlined'
        WHEN '/tanshi/activity' THEN 'ant-design:calendar-outlined'
    END,
    update_by = 'admin',
    update_time = NOW()
WHERE url IN (
    '/tanshi/storyPublic',
    '/tanshi/rolePublic',
    '/tanshi/memberconfig',
    '/tanshi/usermembership',
    '/tanshi/payment',
    '/tanshi/activity'
)
  AND parent_id = '2063098067229384705'
  AND del_flag = 0;

-- 图标对应关系：
-- /tanshi/storyPublic    公开故事管理：book
-- /tanshi/rolePublic     公开角色管理：user
-- /tanshi/memberconfig   会员配置：crown
-- /tanshi/usermembership 用户会员管理：team
-- /tanshi/payment        支付情况：credit-card
-- /tanshi/activity       活动配置：calendar

-- 已有图标菜单，不在本迁移中重复修改：
-- /tanshi/channel       渠道管理：ant-design:pic-left-outlined
-- /tanshi/points        积分管理：ant-design:gift-outlined
-- /tanshi/billing       账单管理：ant-design:account-book-outlined
-- /tanshi/reward        奖励事件：ant-design:thunderbolt-outlined
-- /tanshi/feedbackAudit 反馈审核：ant-design:audit-outlined
-- /tanshi/workReview    作品内容审核：ant-design:safety-certificate-outlined
-- /tanshi/adCenter      运营内容管理：ant-design:picture-outlined

-- 回滚：
-- UPDATE sys_permission
-- SET icon = NULL, update_by = 'admin', update_time = NOW()
-- WHERE url IN (
--     '/tanshi/storyPublic',
--     '/tanshi/rolePublic',
--     '/tanshi/memberconfig',
--     '/tanshi/usermembership',
--     '/tanshi/payment',
--     '/tanshi/activity'
-- )
--   AND parent_id = '2063098067229384705'
--   AND del_flag = 0;
