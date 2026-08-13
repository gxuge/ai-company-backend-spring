CREATE TABLE IF NOT EXISTS `ts_feedback` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '反馈主键',
  `user_id` varchar(32) NOT NULL COMMENT '反馈发布用户ID，对应sys_user.id',
  `type` varchar(16) NOT NULL COMMENT '反馈类型：feature功能建议，bug问题反馈，experience体验问题',
  `title` varchar(100) NOT NULL COMMENT '反馈标题',
  `content` text NOT NULL COMMENT '反馈内容',
  `status` varchar(16) NOT NULL DEFAULT 'received' COMMENT '状态：received已收到，processing处理中，completed已完成',
  `like_count` int unsigned NOT NULL DEFAULT 0 COMMENT '点赞数量',
  `comment_count` int unsigned NOT NULL DEFAULT 0 COMMENT '评论及回复总数',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_feedback_latest` (`is_deleted`, `created_at`, `id`),
  KEY `idx_ts_feedback_hot` (`is_deleted`, `like_count`, `created_at`, `id`),
  KEY `idx_ts_feedback_filter` (`is_deleted`, `type`, `status`, `created_at`, `id`),
  KEY `idx_ts_feedback_user` (`user_id`, `is_deleted`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

CREATE TABLE IF NOT EXISTS `ts_feedback_comment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '评论主键',
  `feedback_id` bigint unsigned NOT NULL COMMENT '反馈ID',
  `user_id` varchar(32) NOT NULL COMMENT '评论用户ID，对应sys_user.id',
  `parent_id` bigint unsigned DEFAULT NULL COMMENT '一级评论为空，二级回复保存一级评论ID',
  `reply_to_user_id` varchar(32) DEFAULT NULL COMMENT '被回复用户ID',
  `content` varchar(2000) NOT NULL COMMENT '评论内容',
  `like_count` int unsigned NOT NULL DEFAULT 0 COMMENT '点赞数量',
  `is_official` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否官方回复：0否，1是',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_feedback_comment_first` (`feedback_id`, `parent_id`, `is_deleted`, `created_at`, `id`),
  KEY `idx_ts_feedback_comment_hot` (`feedback_id`, `parent_id`, `is_deleted`, `like_count`, `created_at`, `id`),
  KEY `idx_ts_feedback_comment_reply` (`parent_id`, `is_deleted`, `created_at`, `id`),
  KEY `idx_ts_feedback_comment_user` (`user_id`, `is_deleted`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈评论与二级回复表';

CREATE TABLE IF NOT EXISTS `ts_feedback_append` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '追加反馈主键',
  `feedback_id` bigint unsigned NOT NULL COMMENT '反馈ID',
  `user_id` varchar(32) NOT NULL COMMENT '追加用户ID，对应sys_user.id',
  `content` text NOT NULL COMMENT '追加反馈内容',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_feedback_append_feedback` (`feedback_id`, `is_deleted`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈追加内容表';

CREATE TABLE IF NOT EXISTS `ts_feedback_like` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '点赞主键',
  `user_id` varchar(32) NOT NULL COMMENT '点赞用户ID，对应sys_user.id',
  `target_type` varchar(16) NOT NULL COMMENT '点赞目标类型：feedback反馈，comment评论',
  `target_id` bigint unsigned NOT NULL COMMENT '点赞目标ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_feedback_like_target` (`user_id`, `target_type`, `target_id`),
  KEY `idx_ts_feedback_like_target` (`target_type`, `target_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈中心点赞表';

CREATE TABLE IF NOT EXISTS `ts_feedback_attachment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '反馈附件主键',
  `feedback_id` bigint unsigned NOT NULL COMMENT '反馈ID',
  `file_url` varchar(1000) NOT NULL COMMENT '附件文件地址',
  `file_type` varchar(32) NOT NULL COMMENT '附件类型：image图片，screenshot截图，log日志文件',
  `is_deleted` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_feedback_attachment_feedback` (`feedback_id`, `is_deleted`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈附件表';
