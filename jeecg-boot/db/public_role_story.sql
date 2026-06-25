-- 若表已存在，先按依赖顺序删除后重建
DROP TABLE IF EXISTS `ts_public_audit_log`;
DROP TABLE IF EXISTS `ts_story_public`;
DROP TABLE IF EXISTS `ts_role_public`;
DROP TABLE IF EXISTS `ts_public_channel`;

-- 1. 公开渠道表
CREATE TABLE `ts_public_channel` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `channel_code` varchar(64) NOT NULL COMMENT '渠道编码',
  `channel_name` varchar(100) NOT NULL COMMENT '渠道名称',
  `channel_image_url` varchar(500) DEFAULT NULL COMMENT '渠道图片',
  `target_type` varchar(16) NOT NULL COMMENT '适用对象：role/story/both',
  `status` varchar(16) NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_public_channel_code` (`channel_code`),
  KEY `idx_ts_public_channel_target_type` (`target_type`),
  KEY `idx_ts_public_channel_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公开渠道表';

-- 2. 角色公开发布表
CREATE TABLE `ts_role_public` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `channel_code` varchar(64) NOT NULL COMMENT '渠道编码',
  `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/pending/online/offline/rejected',
  `display_title` varchar(100) DEFAULT NULL COMMENT '展示标题',
  `display_subtitle` varchar(255) DEFAULT NULL COMMENT '展示副标题',
  `cover_image_url` varchar(500) DEFAULT NULL COMMENT '封面图',
  `intro_text` varchar(1000) DEFAULT NULL COMMENT '展示简介',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `published_at` datetime DEFAULT NULL COMMENT '上架时间',
  `offline_at` datetime DEFAULT NULL COMMENT '下架时间',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `ext_json` text DEFAULT NULL COMMENT '扩展配置',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_role_public_role_channel` (`role_id`, `channel_code`),
  KEY `idx_ts_role_public_status` (`status`),
  KEY `idx_ts_role_public_channel_code` (`channel_code`),
  KEY `idx_ts_role_public_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色公开发布表';

-- 3. 故事公开发布表
CREATE TABLE `ts_story_public` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `story_id` bigint NOT NULL COMMENT '故事ID',
  `channel_code` varchar(64) NOT NULL COMMENT '渠道编码',
  `status` varchar(16) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/pending/online/offline/rejected',
  `display_title` varchar(100) DEFAULT NULL COMMENT '展示标题',
  `display_subtitle` varchar(255) DEFAULT NULL COMMENT '展示副标题',
  `cover_image_url` varchar(500) DEFAULT NULL COMMENT '封面图',
  `intro_text` varchar(1000) DEFAULT NULL COMMENT '展示简介',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `published_at` datetime DEFAULT NULL COMMENT '上架时间',
  `offline_at` datetime DEFAULT NULL COMMENT '下架时间',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `ext_json` text DEFAULT NULL COMMENT '扩展配置',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_story_public_story_channel` (`story_id`, `channel_code`),
  KEY `idx_ts_story_public_status` (`status`),
  KEY `idx_ts_story_public_channel_code` (`channel_code`),
  KEY `idx_ts_story_public_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故事公开发布表';

-- 4. 公开操作审计表
CREATE TABLE `ts_public_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `target_type` varchar(16) NOT NULL COMMENT '对象类型：role/story',
  `public_id` bigint NOT NULL COMMENT '公开记录ID',
  `action_type` varchar(16) NOT NULL COMMENT '动作：submit/approve/reject/online/offline',
  `before_status` varchar(16) DEFAULT NULL COMMENT '变更前状态',
  `after_status` varchar(16) DEFAULT NULL COMMENT '变更后状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `operate_by` varchar(64) DEFAULT NULL COMMENT '操作人',
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_public_audit_log_target` (`target_type`, `public_id`),
  KEY `idx_ts_public_audit_log_action_type` (`action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公开操作审计表';
