ALTER TABLE `ts_agent_chat_session`
  ADD COLUMN `active_agent_code` varchar(64) NOT NULL DEFAULT 'main'
    COMMENT '当前接管会话的Agent编码' AFTER `agent_code`,
  ADD COLUMN `active_agent_updated_at` datetime DEFAULT NULL
    COMMENT '当前Agent最后切换时间' AFTER `active_agent_code`;
