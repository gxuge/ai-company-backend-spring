ALTER TABLE `ts_agent_chat_session`
    ADD COLUMN `sub_agent_history_json` longtext DEFAULT NULL COMMENT '子Agent最近执行历史快照' AFTER `memory_json`;
