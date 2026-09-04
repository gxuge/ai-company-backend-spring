SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ts_user_follow` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '关注关系主键',
  `follower_user_id` varchar(32) NOT NULL COMMENT '发起关注的用户ID，对应sys_user.id',
  `followed_user_id` varchar(32) NOT NULL COMMENT '被关注的用户ID，对应sys_user.id',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1已关注，0已取消',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_user_follow_relation` (`follower_user_id`, `followed_user_id`),
  KEY `idx_ts_user_follow_following` (`follower_user_id`, `status`, `created_at`),
  KEY `idx_ts_user_follow_follower` (`followed_user_id`, `status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

CREATE TABLE IF NOT EXISTS `ts_user_resource_like` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '点赞关系主键',
  `user_id` varchar(32) NOT NULL COMMENT '点赞用户ID，对应sys_user.id',
  `resource_type` varchar(16) NOT NULL COMMENT '资源类型：role角色，story故事',
  `resource_id` bigint unsigned NOT NULL COMMENT '角色或故事资源ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1已点赞，0已取消',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_user_resource_like` (`user_id`, `resource_type`, `resource_id`),
  KEY `idx_ts_user_resource_like_user` (`user_id`, `resource_type`, `status`, `created_at`),
  KEY `idx_ts_user_resource_like_resource` (`resource_type`, `resource_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色与故事点赞表';

-- 影响范围：新增用户关注和角色/故事点赞关系表，不修改现有收藏与资源数据。
-- 回滚前需确认已停止相关接口并备份关系数据：
-- DROP TABLE IF EXISTS `ts_user_resource_like`;
-- DROP TABLE IF EXISTS `ts_user_follow`;
