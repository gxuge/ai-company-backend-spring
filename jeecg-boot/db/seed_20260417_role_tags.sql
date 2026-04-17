-- 官方角色标签初始化脚本（全局统一，不区分用户）
-- 使用方式：在目标 MySQL 库执行本文件

START TRANSACTION;

CREATE TABLE IF NOT EXISTS `ts_role_tag` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_role_tag_name` (`tag_name`),
  KEY `idx_ts_role_tag_status_sort` (`status`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='官方角色标签表';

INSERT INTO `ts_role_tag` (`tag_name`, `status`, `sort_no`)
VALUES
  ('傲娇', 1, 10),
  ('温柔', 1, 20),
  ('极客', 1, 30),
  ('高冷', 1, 40),
  ('毒舌', 1, 50),
  ('腹黑', 1, 60)
ON DUPLICATE KEY UPDATE
  `status` = VALUES(`status`),
  `sort_no` = VALUES(`sort_no`),
  `updated_at` = CURRENT_TIMESTAMP;

COMMIT;

