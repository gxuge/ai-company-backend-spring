-- 表重命名为 ts_ 前缀，并同步标签类型主键/引用字段
-- 创建时间：2026-05-28

SET @db_name := DATABASE();

SET @tbl_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'generation_preset'
);
SET @sql_rename_tbl := IF(@tbl_exists > 0, 'RENAME TABLE `generation_preset` TO `ts_preset`;', 'SELECT 1;');
PREPARE stmt_rename_tbl FROM @sql_rename_tbl;
EXECUTE stmt_rename_tbl;
DEALLOCATE PREPARE stmt_rename_tbl;

SET @tbl_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'preset_tag'
);
SET @sql_rename_tbl := IF(@tbl_exists > 0, 'RENAME TABLE `preset_tag` TO `ts_tag`;', 'SELECT 1;');
PREPARE stmt_rename_tbl FROM @sql_rename_tbl;
EXECUTE stmt_rename_tbl;
DEALLOCATE PREPARE stmt_rename_tbl;

SET @tbl_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'generation_preset_tag'
);
SET @sql_rename_tbl := IF(@tbl_exists > 0, 'RENAME TABLE `generation_preset_tag` TO `ts_preset_tag`;', 'SELECT 1;');
PREPARE stmt_rename_tbl FROM @sql_rename_tbl;
EXECUTE stmt_rename_tbl;
DEALLOCATE PREPARE stmt_rename_tbl;

SET @tbl_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'tag_relation'
);
SET @sql_rename_tbl := IF(@tbl_exists > 0, 'RENAME TABLE `tag_relation` TO `ts_tag_relation`;', 'SELECT 1;');
PREPARE stmt_rename_tbl FROM @sql_rename_tbl;
EXECUTE stmt_rename_tbl;
DEALLOCATE PREPARE stmt_rename_tbl;

SET @tbl_exists := (
  SELECT COUNT(1) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'generation_tag_type'
);
SET @sql_rename_tbl := IF(@tbl_exists > 0, 'RENAME TABLE `generation_tag_type` TO `ts_tag_type`;', 'SELECT 1;');
PREPARE stmt_rename_tbl FROM @sql_rename_tbl;
EXECUTE stmt_rename_tbl;
DEALLOCATE PREPARE stmt_rename_tbl;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'ts_tag' AND COLUMN_NAME = 'type'
);
SET @sql_rename_col := IF(@col_exists > 0, 'ALTER TABLE `ts_tag` CHANGE COLUMN `type` `type_id` varchar(64) NOT NULL COMMENT ''标签类型ID'';', 'SELECT 1;');
PREPARE stmt_rename_col FROM @sql_rename_col;
EXECUTE stmt_rename_col;
DEALLOCATE PREPARE stmt_rename_col;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'ts_tag_type' AND COLUMN_NAME = 'code'
);
SET @sql_rename_col := IF(@col_exists > 0, 'ALTER TABLE `ts_tag_type` CHANGE COLUMN `code` `id` varchar(64) NOT NULL COMMENT ''类型ID'';', 'SELECT 1;');
PREPARE stmt_rename_col FROM @sql_rename_col;
EXECUTE stmt_rename_col;
DEALLOCATE PREPARE stmt_rename_col;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'ts_preset_tag' AND COLUMN_NAME = 'tag_type'
);
SET @sql_drop_col := IF(@col_exists > 0, 'ALTER TABLE `ts_preset_tag` DROP COLUMN `tag_type`;', 'SELECT 1;');
PREPARE stmt_drop_col FROM @sql_drop_col;
EXECUTE stmt_drop_col;
DEALLOCATE PREPARE stmt_drop_col;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'ts_preset_tag' AND INDEX_NAME = 'uk_generation_preset_tag'
);
SET @sql_drop_idx := IF(@idx_exists > 0, 'ALTER TABLE `ts_preset_tag` DROP INDEX `uk_generation_preset_tag`;', 'SELECT 1;');
PREPARE stmt_drop_idx FROM @sql_drop_idx;
EXECUTE stmt_drop_idx;
DEALLOCATE PREPARE stmt_drop_idx;

ALTER TABLE `ts_preset_tag`
  ADD UNIQUE KEY `uk_ts_preset_tag` (`preset_id`, `tag_id`);

