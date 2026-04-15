-- 为 ts_role_image_generate_record 补齐 apply_status 字段（兼容老库）
-- 说明：通过 information_schema 做条件判断，避免重复执行时报错。

SET @has_col := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ts_role_image_generate_record'
      AND COLUMN_NAME = 'apply_status'
);

SET @ddl := IF(
    @has_col = 0,
    'ALTER TABLE ts_role_image_generate_record ADD COLUMN apply_status VARCHAR(32) DEFAULT NULL COMMENT ''应用状态（pending/applied/failed）''',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;