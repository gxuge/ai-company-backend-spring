-- generation_preset_tag 关联表移除 tag_type 字段
-- 创建时间：2026-05-28

SET @db_name := DATABASE();

SET @fk_exists := (
  SELECT COUNT(1)
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = @db_name
    AND TABLE_NAME = 'generation_preset_tag'
    AND CONSTRAINT_NAME = 'fk_generation_preset_tag_type'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql_drop_fk := IF(@fk_exists > 0,
  'ALTER TABLE `generation_preset_tag` DROP FOREIGN KEY `fk_generation_preset_tag_type`;',
  'SELECT 1;'
);
PREPARE stmt_drop_fk FROM @sql_drop_fk;
EXECUTE stmt_drop_fk;
DEALLOCATE PREPARE stmt_drop_fk;

SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'generation_preset_tag'
    AND INDEX_NAME = 'uk_generation_preset_tag'
);
SET @sql_drop_idx := IF(@idx_exists > 0,
  'ALTER TABLE `generation_preset_tag` DROP INDEX `uk_generation_preset_tag`;',
  'SELECT 1;'
);
PREPARE stmt_drop_idx FROM @sql_drop_idx;
EXECUTE stmt_drop_idx;
DEALLOCATE PREPARE stmt_drop_idx;

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'generation_preset_tag'
    AND COLUMN_NAME = 'tag_type'
);
SET @sql_drop_col := IF(@col_exists > 0,
  'ALTER TABLE `generation_preset_tag` DROP COLUMN `tag_type`;',
  'SELECT 1;'
);
PREPARE stmt_drop_col FROM @sql_drop_col;
EXECUTE stmt_drop_col;
DEALLOCATE PREPARE stmt_drop_col;

ALTER TABLE `generation_preset_tag`
  ADD UNIQUE KEY `uk_generation_preset_tag` (`preset_id`, `tag_id`);

