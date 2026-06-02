-- 生成标签类型表 + generation_preset_tag 从 slot 迁移到 tag_type
-- 创建时间：2026-05-28

CREATE TABLE IF NOT EXISTS `generation_tag_type` (
  `code` varchar(64) NOT NULL COMMENT '类型编码',
  `name` varchar(128) NOT NULL COMMENT '类型名称',
  `scope` varchar(32) NOT NULL COMMENT '适用目标：character|story|shared',
  `description` varchar(1000) DEFAULT NULL COMMENT '类型描述',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`code`),
  UNIQUE KEY `uk_generation_tag_type_name` (`name`),
  KEY `idx_generation_tag_type_scope` (`scope`),
  KEY `idx_generation_tag_type_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成标签类型字典表';

INSERT INTO `generation_tag_type` (`code`, `name`, `scope`, `description`, `enabled`, `sort_order`)
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

ALTER TABLE `generation_preset_tag`
  ADD COLUMN `tag_type` varchar(64) DEFAULT NULL COMMENT '标签类型' AFTER `tag_id`;

UPDATE `generation_preset_tag` gpt
LEFT JOIN `preset_tag` pt ON pt.id = gpt.tag_id
SET gpt.tag_type = COALESCE(NULLIF(gpt.slot, ''), pt.type)
WHERE gpt.tag_type IS NULL OR gpt.tag_type = '';

UPDATE `generation_preset_tag`
SET `tag_type` = 'identity'
WHERE `tag_type` IS NULL OR `tag_type` = '';

ALTER TABLE `generation_preset_tag`
  MODIFY COLUMN `tag_type` varchar(64) NOT NULL COMMENT '标签类型';

ALTER TABLE `generation_preset_tag`
  DROP INDEX `uk_generation_preset_tag`,
  ADD UNIQUE KEY `uk_generation_preset_tag` (`preset_id`, `tag_id`, `tag_type`);

ALTER TABLE `generation_preset_tag`
  DROP COLUMN `slot`;

ALTER TABLE `generation_preset_tag`
  ADD CONSTRAINT `fk_generation_preset_tag_type`
  FOREIGN KEY (`tag_type`) REFERENCES `generation_tag_type` (`code`);
