CREATE TABLE IF NOT EXISTS `ts_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '草稿主键',
  `user_id` varchar(64) NOT NULL COMMENT '草稿所属用户ID',
  `draft_type` varchar(16) NOT NULL COMMENT '草稿类型：role角色，story故事',
  `draft_name` varchar(200) NOT NULL COMMENT '草稿箱展示名称',
  `source_id` bigint DEFAULT NULL COMMENT '来源正式角色或故事ID',
  `content_json` longtext NOT NULL COMMENT '页面完整状态JSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1正常，0已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_draft_user_type_updated` (`user_id`, `draft_type`, `status`, `updated_at`),
  KEY `idx_ts_draft_user_source` (`user_id`, `draft_type`, `source_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色与故事统一草稿表';
