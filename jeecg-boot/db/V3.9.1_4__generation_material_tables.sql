-- AI 角色/故事一键生成素材系统：核心素材与规则表
-- 创建时间：2026-05-28

CREATE TABLE IF NOT EXISTS `preset_tag` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `scope` varchar(32) NOT NULL COMMENT '作用域：character|story|shared',
  `type` varchar(64) NOT NULL COMMENT '标签类型',
  `name` varchar(128) NOT NULL COMMENT '标签名称',
  `description` varchar(1000) DEFAULT NULL COMMENT '标签描述',
  `prompt_text` text COMMENT '提示词素材正文',
  `weight` int NOT NULL DEFAULT 100 COMMENT '默认权重',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_preset_tag_scope_type_name` (`scope`,`type`,`name`),
  KEY `idx_preset_tag_scope` (`scope`),
  KEY `idx_preset_tag_type` (`type`),
  KEY `idx_preset_tag_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成素材标签主表';

CREATE TABLE IF NOT EXISTS `generation_preset` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '预设名称',
  `description` varchar(1000) DEFAULT NULL COMMENT '预设描述',
  `target_type` varchar(32) NOT NULL COMMENT '目标类型：character|story|both',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_generation_preset_name` (`name`),
  KEY `idx_generation_preset_target_type` (`target_type`),
  KEY `idx_generation_preset_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成预设主表';

CREATE TABLE IF NOT EXISTS `generation_preset_tag` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `preset_id` varchar(32) NOT NULL COMMENT '预设ID',
  `tag_id` varchar(32) NOT NULL COMMENT '标签ID',
  `slot` varchar(64) DEFAULT NULL COMMENT '槽位标识',
  `required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否必选：1是，0否',
  `weight_override` int DEFAULT NULL COMMENT '权重覆盖值',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_generation_preset_tag` (`preset_id`,`tag_id`,`slot`),
  KEY `idx_generation_preset_tag_preset` (`preset_id`),
  KEY `idx_generation_preset_tag_tag` (`tag_id`),
  CONSTRAINT `fk_generation_preset_tag_preset` FOREIGN KEY (`preset_id`) REFERENCES `generation_preset` (`id`),
  CONSTRAINT `fk_generation_preset_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `preset_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预设与标签关联表';

CREATE TABLE IF NOT EXISTS `tag_relation` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `source_tag_id` varchar(32) NOT NULL COMMENT '源标签ID',
  `target_tag_id` varchar(32) NOT NULL COMMENT '目标标签ID',
  `relation_type` varchar(32) NOT NULL COMMENT '关系类型：compatible|incompatible|requires|boosts|blocks',
  `weight_delta` int DEFAULT 0 COMMENT '权重增量（可正可负）',
  `description` varchar(1000) DEFAULT NULL COMMENT '关系说明',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_relation_unique` (`source_tag_id`,`target_tag_id`,`relation_type`),
  KEY `idx_tag_relation_source` (`source_tag_id`),
  KEY `idx_tag_relation_target` (`target_tag_id`),
  KEY `idx_tag_relation_enabled` (`enabled`),
  CONSTRAINT `fk_tag_relation_source` FOREIGN KEY (`source_tag_id`) REFERENCES `preset_tag` (`id`),
  CONSTRAINT `fk_tag_relation_target` FOREIGN KEY (`target_tag_id`) REFERENCES `preset_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签关系规则表';

