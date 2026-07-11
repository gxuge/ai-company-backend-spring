CREATE TABLE IF NOT EXISTS `kb_base` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '知识库名称',
  `description` varchar(512) DEFAULT NULL COMMENT '知识库描述',
  `biz_type` varchar(64) NOT NULL DEFAULT 'default' COMMENT '业务类型',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_kb_base_name` (`name`),
  KEY `idx_kb_base_biz_type` (`biz_type`),
  KEY `idx_kb_base_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库主表';

CREATE TABLE IF NOT EXISTS `kb_document` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `kb_id` varchar(64) NOT NULL COMMENT '知识库ID',
  `name` varchar(255) NOT NULL COMMENT '文档名称',
  `source_type` varchar(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型：manual/upload/url/import',
  `file_type` varchar(64) DEFAULT NULL COMMENT '文件类型',
  `file_url` varchar(512) DEFAULT NULL COMMENT '文件地址',
  `parse_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '解析状态：pending/processing/success/failed',
  `chunk_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '切分状态：pending/processing/success/failed',
  `embed_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '向量状态：pending/processing/success/failed',
  `metadata_json` longtext DEFAULT NULL COMMENT '元数据JSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_kb_document_kb_id` (`kb_id`),
  KEY `idx_kb_document_status` (`status`),
  KEY `idx_kb_document_parse_status` (`parse_status`),
  KEY `idx_kb_document_chunk_status` (`chunk_status`),
  KEY `idx_kb_document_embed_status` (`embed_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

CREATE TABLE IF NOT EXISTS `kb_chunk` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `kb_id` varchar(64) NOT NULL COMMENT '知识库ID',
  `document_id` varchar(64) NOT NULL COMMENT '文档ID',
  `content` longtext NOT NULL COMMENT '分段内容',
  `chunk_type` varchar(32) NOT NULL DEFAULT 'text' COMMENT '分段类型：text/table/code/qa等',
  `token_count` int NOT NULL DEFAULT 0 COMMENT 'Token数量',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `metadata_json` longtext DEFAULT NULL COMMENT '元数据JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_kb_chunk_kb_id` (`kb_id`),
  KEY `idx_kb_chunk_document_id` (`document_id`),
  KEY `idx_kb_chunk_status` (`status`),
  KEY `idx_kb_chunk_sort_no` (`sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分段表';

CREATE TABLE IF NOT EXISTS `kb_chunk_index` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `kb_id` varchar(64) NOT NULL COMMENT '知识库ID',
  `chunk_id` varchar(64) NOT NULL COMMENT '分段ID',
  `index_text` longtext NOT NULL COMMENT '索引文本',
  `index_type` varchar(32) NOT NULL DEFAULT 'default' COMMENT '索引类型：default/title/question/summary等',
  `embedding_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '向量状态：pending/processing/success/failed',
  `sort_no` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `metadata_json` longtext DEFAULT NULL COMMENT '元数据JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_kb_chunk_index_kb_id` (`kb_id`),
  KEY `idx_kb_chunk_index_chunk_id` (`chunk_id`),
  KEY `idx_kb_chunk_index_embedding_status` (`embedding_status`),
  KEY `idx_kb_chunk_index_sort_no` (`sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分段索引表';

CREATE TABLE IF NOT EXISTS `kb_search_config` (
  `id` varchar(64) NOT NULL COMMENT '主键ID',
  `kb_id` varchar(64) NOT NULL COMMENT '知识库ID',
  `search_mode` varchar(32) NOT NULL DEFAULT 'semantic' COMMENT '检索模式：semantic/hybrid/fulltext',
  `similarity_threshold` decimal(6,4) NOT NULL DEFAULT 0.5000 COMMENT '相似度阈值',
  `reference_limit` int NOT NULL DEFAULT 4000 COMMENT '参考文本上限',
  `top_k` int NOT NULL DEFAULT 5 COMMENT '返回条数',
  `use_rerank` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用Rerank：0否 1是',
  `use_query_optimization` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用Query Optimization：0否 1是',
  `config_json` longtext DEFAULT NULL COMMENT '扩展配置JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_kb_search_config_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库检索配置表';
