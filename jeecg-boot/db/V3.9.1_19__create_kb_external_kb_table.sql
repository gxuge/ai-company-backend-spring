CREATE TABLE IF NOT EXISTS `kb_external_kb` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `external_kb_id` varchar(64) NOT NULL COMMENT '外部知识库ID',
  `name` varchar(128) NOT NULL COMMENT '名称',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
  `endpoint_url` varchar(512) NOT NULL COMMENT '接口地址',
  `auth_type` varchar(32) NOT NULL DEFAULT 'none' COMMENT '鉴权类型：none/api_key/bearer',
  `auth_config` longtext DEFAULT NULL COMMENT '鉴权配置JSON',
  `timeout_ms` int NOT NULL DEFAULT 5000 COMMENT '超时时间毫秒',
  `weight` decimal(10,4) NOT NULL DEFAULT 1.0000 COMMENT '权重',
  `metadata_json` longtext DEFAULT NULL COMMENT '元数据JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_external_kb_external_kb_id` (`external_kb_id`),
  KEY `idx_kb_external_kb_enabled` (`enabled`),
  KEY `idx_kb_external_kb_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部知识库配置表';
