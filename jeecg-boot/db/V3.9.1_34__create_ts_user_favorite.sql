CREATE TABLE IF NOT EXISTS `ts_user_favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '收藏主键',
  `user_id` varchar(32) NOT NULL COMMENT '收藏所属用户ID，对应sys_user.id',
  `resource_type` varchar(16) NOT NULL COMMENT '资源类型：role角色，story故事',
  `resource_id` bigint unsigned NOT NULL COMMENT '角色或故事资源ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1已收藏，0已取消',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_user_favorite_resource` (`user_id`, `resource_type`, `resource_id`),
  KEY `idx_ts_user_favorite_page` (`user_id`, `resource_type`, `status`, `created_at`),
  KEY `idx_ts_user_favorite_resource` (`resource_type`, `resource_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色与故事收藏表';
