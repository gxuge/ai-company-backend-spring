SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ts_tag'
    AND index_name = 'uk_ts_tag_scope_type_name'
);
SET @drop_sql := IF(@idx_exists > 0,
  'ALTER TABLE `ts_tag` DROP INDEX `uk_ts_tag_scope_type_name`',
  'SELECT 1');
PREPARE stmt_drop_idx FROM @drop_sql;
EXECUTE stmt_drop_idx;
DEALLOCATE PREPARE stmt_drop_idx;