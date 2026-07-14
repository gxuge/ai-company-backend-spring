ALTER TABLE `ts_agent_chat_message_event`
  ADD COLUMN `node_name` varchar(128) DEFAULT NULL COMMENT '实际执行节点名称' AFTER `agent_code`,
  ADD COLUMN `node_type` varchar(32) DEFAULT NULL COMMENT '节点类型：llm/tool' AFTER `node_name`,
  ADD KEY `idx_ts_agent_chat_message_event_node` (`node_name`, `node_type`);

ALTER TABLE `ts_agent_chat_message`
  ADD COLUMN `source_node_name` varchar(128) DEFAULT NULL COMMENT '生成该消息的节点名称' AFTER `agent_code`,
  ADD COLUMN `source_event_id` varchar(64) DEFAULT NULL COMMENT '关联的SubAgent完整事件ID' AFTER `source_node_name`,
  ADD KEY `idx_ts_agent_chat_message_source_event` (`source_event_id`),
  ADD KEY `idx_ts_agent_chat_message_source_node` (`source_node_name`);
