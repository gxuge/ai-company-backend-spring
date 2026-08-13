CREATE TABLE IF NOT EXISTS `ts_user_browse_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '浏览记录主键',
  `user_id` varchar(32) NOT NULL COMMENT '浏览记录所属用户ID，对应sys_user.id',
  `resource_type` varchar(16) NOT NULL COMMENT '资源类型：role角色，story故事',
  `resource_id` bigint unsigned NOT NULL COMMENT '角色或故事资源ID',
  `view_count` bigint unsigned NOT NULL DEFAULT 1 COMMENT '累计浏览次数',
  `first_viewed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次浏览时间',
  `last_viewed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近浏览时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1有效，0已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_user_browse_history_resource` (`user_id`, `resource_type`, `resource_id`),
  KEY `idx_ts_user_browse_history_page` (`user_id`, `resource_type`, `status`, `last_viewed_at`),
  KEY `idx_ts_user_browse_history_resource` (`resource_type`, `resource_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色与故事浏览记录表';
