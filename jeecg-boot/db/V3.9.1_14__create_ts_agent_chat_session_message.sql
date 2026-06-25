CREATE TABLE IF NOT EXISTS `ts_agent_chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_no` varchar(64) NOT NULL COMMENT '会话编号',
  `app_id` varchar(64) NOT NULL COMMENT '应用ID',
  `agent_code` varchar(64) NOT NULL COMMENT 'Agent编码',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `session_title` varchar(200) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
  `session_summary` longtext DEFAULT NULL COMMENT '会话摘要',
  `session_status` varchar(32) NOT NULL DEFAULT 'active' COMMENT '会话状态：active/archived/deleted',
  `memory_json` longtext DEFAULT NULL COMMENT '会话记忆快照',
  `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_at` datetime DEFAULT NULL COMMENT '最后一条消息时间',
  `message_count` int NOT NULL DEFAULT 0 COMMENT '消息总数',
  `turn_count` int NOT NULL DEFAULT 0 COMMENT '轮次数',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_agent_chat_session_session_no` (`session_no`),
  KEY `idx_ts_agent_chat_session_user_agent_status` (`user_id`, `agent_code`, `session_status`),
  KEY `idx_ts_agent_chat_session_app_agent` (`app_id`, `agent_code`),
  KEY `idx_ts_agent_chat_session_last_message_at` (`last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话表';

CREATE TABLE IF NOT EXISTS `ts_agent_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `message_no` bigint NOT NULL COMMENT '会话内消息序号',
  `role_type` varchar(16) NOT NULL COMMENT '消息角色：user/assistant/system/tool',
  `content` longtext DEFAULT NULL COMMENT '消息正文',
  `content_raw` longtext DEFAULT NULL COMMENT '原始内容',
  `content_format` varchar(16) NOT NULL DEFAULT 'text' COMMENT '内容格式：text/markdown/json',
  `message_status` varchar(16) NOT NULL DEFAULT 'success' COMMENT '消息状态：streaming/success/failed',
  `parent_message_id` bigint DEFAULT NULL COMMENT '父消息ID',
  `run_id` varchar(64) DEFAULT NULL COMMENT 'Agent运行ID',
  `prompt_code` varchar(64) DEFAULT NULL COMMENT '提示词编码',
  `model_id` varchar(64) DEFAULT NULL COMMENT '文本模型ID',
  `token_usage_json` longtext DEFAULT NULL COMMENT 'Token统计',
  `ext_json` longtext DEFAULT NULL COMMENT '扩展JSON',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_agent_chat_message_session_no` (`session_id`, `message_no`),
  KEY `idx_ts_agent_chat_message_session_created_at` (`session_id`, `created_at`),
  KEY `idx_ts_agent_chat_message_run_id` (`run_id`),
  KEY `idx_ts_agent_chat_message_session_role` (`session_id`, `role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent消息表';

ALTER TABLE `ts_chat_message_events`
  ADD COLUMN `session_id` bigint DEFAULT NULL COMMENT 'Agent会话ID' AFTER `message_id`,
  ADD COLUMN `agent_session_id` bigint DEFAULT NULL COMMENT 'Agent会话记录ID' AFTER `session_id`,
  ADD COLUMN `run_id` varchar(64) DEFAULT NULL COMMENT 'Agent运行ID' AFTER `agent_session_id`;

ALTER TABLE `ts_chat_message_events`
  ADD KEY `idx_ts_chat_message_events_session_id` (`session_id`),
  ADD KEY `idx_ts_chat_message_events_agent_session_id` (`agent_session_id`),
  ADD KEY `idx_ts_chat_message_events_run_id` (`run_id`);
