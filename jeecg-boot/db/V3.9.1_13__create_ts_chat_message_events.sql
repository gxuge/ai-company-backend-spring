CREATE TABLE IF NOT EXISTS `ts_chat_message_events` (
  `id` varchar(64) NOT NULL COMMENT '事件ID',
  `message_id` varchar(64) NOT NULL COMMENT '消息ID',
  `type` varchar(16) NOT NULL COMMENT '事件块类型：llm/tool',
  `name` varchar(128) NOT NULL COMMENT '节点名称',
  `content` text DEFAULT NULL COMMENT '主要文本内容',
  `status` tinyint DEFAULT NULL COMMENT '状态：1成功 0失败 2运行中或未知',
  `json` text DEFAULT NULL COMMENT '扩展JSON',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_chat_message_events_message_id` (`message_id`),
  KEY `idx_ts_chat_message_events_type_name` (`type`, `name`),
  KEY `idx_ts_chat_message_events_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息节点事件表';
