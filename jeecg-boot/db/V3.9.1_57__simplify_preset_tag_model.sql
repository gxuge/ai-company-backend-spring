SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `ts_preset`
SET
  `description` = CONCAT('围绕“', `name`, '”展开的互动故事预设，由模型补全人物关系、世界设定、场景和长期剧情钩子。'),
  `updated_at` = NOW()
WHERE (`description` IS NULL OR TRIM(`description`) = '')
  AND `target_type` IN ('story', 'both');

DROP TABLE IF EXISTS `ts_preset_tag`;
DROP TABLE IF EXISTS `ts_tag_relation`;
DROP TABLE IF EXISTS `ts_user_role_tag`;
DROP TABLE IF EXISTS `ts_user_preference_tag`;
DROP TABLE IF EXISTS `ts_role_tag`;

COMMIT;
