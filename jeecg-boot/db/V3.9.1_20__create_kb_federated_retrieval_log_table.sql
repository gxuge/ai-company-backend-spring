CREATE TABLE IF NOT EXISTS `kb_federated_retrieval_log` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `query` varchar(1024) NOT NULL COMMENT '原始query',
  `kb_ids_json` longtext DEFAULT NULL COMMENT '内部知识库ID JSON',
  `external_kb_ids_json` longtext DEFAULT NULL COMMENT '外部知识库ID JSON',
  `actual_params_json` longtext DEFAULT NULL COMMENT '请求与实际参数JSON',
  `result_count` int DEFAULT NULL COMMENT '返回条数',
  `result_json` longtext DEFAULT NULL COMMENT '结果JSON',
  `debug_json` longtext DEFAULT NULL COMMENT '调试JSON',
  `status` varchar(32) NOT NULL COMMENT '状态：success/failed',
  `error_message` longtext DEFAULT NULL COMMENT '错误信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_kb_federated_retrieval_log_query` (`query`(255)),
  KEY `idx_kb_federated_retrieval_log_status` (`status`),
  KEY `idx_kb_federated_retrieval_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多知识库联邦检索日志表';
