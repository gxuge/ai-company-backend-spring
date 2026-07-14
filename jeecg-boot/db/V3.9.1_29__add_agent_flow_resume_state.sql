ALTER TABLE `ts_agent_chat_session`
  ADD COLUMN `active_node_name` varchar(128) DEFAULT NULL
    COMMENT '下一轮恢复执行的节点名称' AFTER `active_agent_updated_at`,
  ADD COLUMN `active_stage` varchar(32) DEFAULT NULL
    COMMENT '当前子Agent流程阶段' AFTER `active_node_name`,
  ADD COLUMN `agent_flow_state_json` longtext DEFAULT NULL
    COMMENT '当前子Agent可恢复流程状态' AFTER `active_stage`;
