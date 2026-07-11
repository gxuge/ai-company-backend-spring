ALTER TABLE `ts_agent_chat_session`
  ADD COLUMN `state_json` longtext NULL COMMENT '会话结构化状态' AFTER `memory_json`;

ALTER TABLE `ts_agent_chat_message`
  ADD COLUMN `sender_type` varchar(32) NOT NULL DEFAULT 'main_agent' COMMENT '发送方类型：user/main_agent/sub_agent/system/tool' AFTER `role_type`,
  ADD COLUMN `agent_code` varchar(64) NOT NULL DEFAULT 'main' COMMENT 'Agent编码' AFTER `sender_type`,
  ADD COLUMN `content_json` longtext NULL COMMENT '结构化内容JSON' AFTER `content_raw`,
  ADD COLUMN `visible_to_user` tinyint NOT NULL DEFAULT 1 COMMENT '是否用户可见：0否 1是' AFTER `content_json`,
  ADD KEY `idx_ts_agent_chat_message_visible` (`session_id`, `visible_to_user`, `message_no`),
  ADD KEY `idx_ts_agent_chat_message_agent` (`session_id`, `agent_code`, `sender_type`);

ALTER TABLE `ts_chat_message_events`
  ADD COLUMN `trace_id` varchar(64) NULL COMMENT '链路追踪ID' AFTER `run_id`,
  ADD COLUMN `parent_run_id` varchar(64) NULL COMMENT '父运行ID' AFTER `trace_id`,
  ADD COLUMN `parent_event_id` varchar(64) NULL COMMENT '父事件ID' AFTER `parent_run_id`,
  ADD COLUMN `turn_id` varchar(64) NULL COMMENT '对话轮次ID' AFTER `parent_event_id`,
  ADD COLUMN `sender_type` varchar(32) NULL COMMENT '发送方类型' AFTER `turn_id`,
  ADD COLUMN `agent_code` varchar(64) NULL COMMENT 'Agent编码' AFTER `sender_type`,
  ADD KEY `idx_ts_chat_message_events_trace_id` (`trace_id`),
  ADD KEY `idx_ts_chat_message_events_agent` (`agent_code`, `sender_type`);
