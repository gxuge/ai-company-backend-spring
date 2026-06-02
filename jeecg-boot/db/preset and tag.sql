SET NAMES utf8mb4;

-- =========================================================
-- AI 伴侣素材标签系统建表脚本（默认：方案 A）
-- 兼容建议：
-- 1) CHECK 约束在 MySQL 8.0.16+ 才会真正生效
-- 2) 若使用 MySQL 5.7，请在后端做同等枚举/一致性校验
-- =========================================================

-- 为了避免外键依赖报错，先按依赖顺序删除
DROP TABLE IF EXISTS `ts_tag_relation`;
DROP TABLE IF EXISTS `ts_preset_tag`;
DROP TABLE IF EXISTS `ts_tag`;
DROP TABLE IF EXISTS `ts_preset`;
DROP TABLE IF EXISTS `ts_tag_type`;

-- 1) 标签类型表（先建）
CREATE TABLE `ts_tag_type` (
  `id` varchar(64) NOT NULL COMMENT '标签类型ID（业务唯一），如 identity/story_background',
  `name` varchar(128) NOT NULL COMMENT '标签类型名称',
  `scope` varchar(32) NOT NULL COMMENT '适用范围：character|story|shared',
  `description` varchar(1000) DEFAULT NULL COMMENT '描述',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1是0否',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_tag_type_scope_name` (`scope`,`name`),
  KEY `idx_ts_tag_type_scope` (`scope`),
  KEY `idx_ts_tag_type_enabled` (`enabled`),
  CONSTRAINT `ck_ts_tag_type_scope`
    CHECK (`scope` IN ('character', 'story', 'shared'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签类型字典表';

INSERT INTO `ts_tag_type` (`id`, `name`, `scope`, `description`, `enabled`, `sort_order`)
VALUES
  ('identity', '身份/职业', 'character', '角色身份、职业定位', 1, 10),
  ('gender', '性别', 'character', '角色性别特征（如男/女/未知）', 1, 15),
  ('user_background', '人物背景', 'character', '成长经历、身份来历、人生背景', 1, 20),
  ('appearance', '外貌气质', 'character', '五官、气质、形象特征', 1, 30),
  ('dress', '穿着', 'character', '服装、配饰、风格化穿搭', 1, 40),
  ('personality', '性格', 'character', '稳定人格特征与处事倾向', 1, 50),
  ('behavior', '行为习惯', 'character', '生活习惯、动作偏好、日常行为模式', 1, 60),
  ('speech_style', '说话风格', 'character', '措辞、语气、表达方式', 1, 70),
  ('goal', '目标动机', 'character', '阶段目标、长期追求、驱动因素', 1, 80),
  ('secret', '隐藏信息', 'character', '秘密设定、隐藏身份、未公开信息', 1, 90),
  ('ability', '能力特长', 'character', '技能、专长、擅长领域', 1, 100),
  ('limitation', '能力限制', 'character', '短板、代价、能力边界', 1, 110),
  ('title', '主题', 'story', '故事标题主题、核心命题', 1, 210),
  ('story_background', '故事背景', 'story', '世界观与故事起点', 1, 220),
  ('story_rule', '故事规则', 'story', '世界机制、互动规则、设定约束', 1, 230),
  ('time_period', '时间阶段', 'story', '时代与时间线阶段', 1, 240),
  ('location', '主要场所', 'story', '关键地点与活动空间', 1, 250),
  ('user_role', '用户身份', 'story', '用户在故事中的身份定位', 1, 260),
  ('conflict', '核心冲突', 'story', '矛盾与冲突主线', 1, 270),
  ('plot_hook', '剧情钩子', 'story', '驱动后续推进的钩子事件', 1, 280),
  ('narrative_style', '叙事风格', 'story', '叙述方式与风格基调', 1, 290),
  ('progression_mode', '推进模式', 'story', '剧情推进节奏与方式', 1, 300),
  ('boundary_rule', '故事边界', 'story', '内容边界与禁止项', 1, 310)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `scope` = VALUES(`scope`),
  `description` = VALUES(`description`),
  `enabled` = VALUES(`enabled`),
  `sort_order` = VALUES(`sort_order`),
  `updated_at` = NOW();

-- 2) 预设主表
CREATE TABLE `ts_preset` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `name` varchar(128) NOT NULL COMMENT '预设名称',
  `description` varchar(1000) DEFAULT NULL COMMENT '预设描述',
  `target_type` varchar(32) NOT NULL COMMENT '目标类型：character|story|both',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1是0否',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_preset_name` (`name`),
  KEY `idx_ts_preset_target_type` (`target_type`),
  KEY `idx_ts_preset_enabled` (`enabled`),
  CONSTRAINT `ck_ts_preset_target_type`
    CHECK (`target_type` IN ('character', 'story', 'both'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成预设主表';

-- 3) 标签主表
-- 方案 A：保留结构，应用层校验 ts_tag.scope 与 ts_tag_type.scope 一致性
CREATE TABLE `ts_tag` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `scope` varchar(32) NOT NULL COMMENT '作用域：character|story|shared',
  `type_id` varchar(64) NOT NULL COMMENT '标签类型ID，关联 ts_tag_type.id',
  `name` varchar(128) NOT NULL COMMENT '标签名称',
  `description` varchar(1000) DEFAULT NULL COMMENT '标签描述',
  `prompt_text` text COMMENT '素材提示词正文',
  `weight` int NOT NULL DEFAULT 100 COMMENT '默认权重',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1是0否',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_tag_scope` (`scope`),
  KEY `idx_ts_tag_type_id` (`type_id`),
  KEY `idx_ts_tag_enabled` (`enabled`),
  CONSTRAINT `fk_ts_tag_type_id` FOREIGN KEY (`type_id`) REFERENCES `ts_tag_type` (`id`),
  CONSTRAINT `ck_ts_tag_scope`
    CHECK (`scope` IN ('character', 'story', 'shared'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签素材主表';

-- 4) 预设-标签关联表（纯关联 + 配置）
CREATE TABLE `ts_preset_tag` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `preset_id` varchar(32) NOT NULL COMMENT '预设ID',
  `tag_id` varchar(32) NOT NULL COMMENT '标签ID',
  `is_required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否必选：1是0否',
  `weight_override` int DEFAULT NULL COMMENT '权重覆盖',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_preset_tag` (`preset_id`,`tag_id`),
  KEY `idx_ts_preset_tag_preset_id` (`preset_id`),
  KEY `idx_ts_preset_tag_tag_id` (`tag_id`),
  CONSTRAINT `fk_ts_preset_tag_preset` FOREIGN KEY (`preset_id`) REFERENCES `ts_preset` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ts_preset_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `ts_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预设与标签关联表';

-- 5) 标签关系规则表
CREATE TABLE `ts_tag_relation` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `source_tag_id` varchar(32) NOT NULL COMMENT '源标签ID',
  `target_tag_id` varchar(32) NOT NULL COMMENT '目标标签ID',
  `relation_type` varchar(32) NOT NULL COMMENT '关系类型：compatible|incompatible|requires|boosts|blocks',
  `weight_delta` int DEFAULT 0 COMMENT '权重增量（可正可负）',
  `description` varchar(1000) DEFAULT NULL COMMENT '关系说明',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1是0否',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_tag_relation_unique` (`source_tag_id`,`target_tag_id`,`relation_type`),
  KEY `idx_ts_tag_relation_source` (`source_tag_id`),
  KEY `idx_ts_tag_relation_target` (`target_tag_id`),
  KEY `idx_ts_tag_relation_enabled` (`enabled`),
  CONSTRAINT `fk_ts_tag_relation_source` FOREIGN KEY (`source_tag_id`) REFERENCES `ts_tag` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ts_tag_relation_target` FOREIGN KEY (`target_tag_id`) REFERENCES `ts_tag` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_ts_tag_relation_type`
    CHECK (`relation_type` IN ('compatible', 'incompatible', 'requires', 'boosts', 'blocks')),
  CONSTRAINT `ck_ts_tag_relation_not_self`
    CHECK (`source_tag_id` <> `target_tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签关系规则表';

-- =========================================================
-- 方案 B（可选）：在 SQL 层强制 ts_tag.type_id + scope 与 ts_tag_type.id + scope 一致
-- 使用前提：MySQL 8.0+，并确认现有数据无脏数据
-- 说明：如启用本方案，应替换方案 A 中 ts_tag 的 FK 为复合 FK
-- =========================================================
/*
-- 1) 父表增加复合唯一键（供复合外键引用）
ALTER TABLE `ts_tag_type`
  ADD UNIQUE KEY `uk_ts_tag_type_id_scope` (`id`, `scope`);

-- 2) 子表增加复合索引（供复合外键使用）
ALTER TABLE `ts_tag`
  ADD KEY `idx_ts_tag_type_scope_fk` (`type_id`, `scope`);

-- 3) 删除原单列外键，改为复合外键
ALTER TABLE `ts_tag`
  DROP FOREIGN KEY `fk_ts_tag_type_id`,
  ADD CONSTRAINT `fk_ts_tag_type_id_scope`
    FOREIGN KEY (`type_id`, `scope`)
    REFERENCES `ts_tag_type` (`id`, `scope`);
*/
