ALTER TABLE `ts_feedback`
  ADD COLUMN `audit_status` varchar(16) DEFAULT NULL COMMENT '审核状态：pending待审核，approved通过，rejected驳回' AFTER `status`,
  ADD COLUMN `audit_reason` varchar(500) DEFAULT NULL COMMENT '审核驳回原因' AFTER `audit_status`,
  ADD COLUMN `audited_by` varchar(32) DEFAULT NULL COMMENT '审核人ID，对应sys_user.id' AFTER `audit_reason`,
  ADD COLUMN `audited_at` datetime DEFAULT NULL COMMENT '审核时间' AFTER `audited_by`;

UPDATE `ts_feedback`
SET `audit_status` = 'approved'
WHERE `audit_status` IS NULL;

ALTER TABLE `ts_feedback`
  MODIFY COLUMN `audit_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending待审核，approved通过，rejected驳回',
  ADD KEY `idx_ts_feedback_audit` (`audit_status`, `is_deleted`, `created_at`, `id`);

ALTER TABLE `ts_feedback_comment`
  ADD COLUMN `audit_status` varchar(16) DEFAULT NULL COMMENT '审核状态：pending待审核，approved通过，rejected驳回' AFTER `is_official`,
  ADD COLUMN `audit_reason` varchar(500) DEFAULT NULL COMMENT '审核驳回原因' AFTER `audit_status`,
  ADD COLUMN `audited_by` varchar(32) DEFAULT NULL COMMENT '审核人ID，对应sys_user.id' AFTER `audit_reason`,
  ADD COLUMN `audited_at` datetime DEFAULT NULL COMMENT '审核时间' AFTER `audited_by`;

UPDATE `ts_feedback_comment`
SET `audit_status` = 'approved'
WHERE `audit_status` IS NULL;

ALTER TABLE `ts_feedback_comment`
  MODIFY COLUMN `audit_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending待审核，approved通过，rejected驳回',
  ADD KEY `idx_ts_feedback_comment_audit` (`audit_status`, `is_deleted`, `created_at`, `id`);

ALTER TABLE `ts_feedback_append`
  ADD COLUMN `audit_status` varchar(16) DEFAULT NULL COMMENT '审核状态：pending待审核，approved通过，rejected驳回' AFTER `content`,
  ADD COLUMN `audit_reason` varchar(500) DEFAULT NULL COMMENT '审核驳回原因' AFTER `audit_status`,
  ADD COLUMN `audited_by` varchar(32) DEFAULT NULL COMMENT '审核人ID，对应sys_user.id' AFTER `audit_reason`,
  ADD COLUMN `audited_at` datetime DEFAULT NULL COMMENT '审核时间' AFTER `audited_by`;

UPDATE `ts_feedback_append`
SET `audit_status` = 'approved'
WHERE `audit_status` IS NULL;

ALTER TABLE `ts_feedback_append`
  MODIFY COLUMN `audit_status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending待审核，approved通过，rejected驳回',
  ADD KEY `idx_ts_feedback_append_audit` (`audit_status`, `is_deleted`, `created_at`, `id`);

CREATE TABLE IF NOT EXISTS `ts_feedback_audit_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '审核日志主键',
  `target_type` varchar(16) NOT NULL COMMENT '审核目标：feedback反馈，comment评论或回复，append追加内容',
  `target_id` bigint unsigned NOT NULL COMMENT '审核目标ID',
  `feedback_id` bigint unsigned NOT NULL COMMENT '所属反馈ID',
  `previous_status` varchar(16) NOT NULL COMMENT '审核前状态',
  `audit_status` varchar(16) NOT NULL COMMENT '审核后状态：approved通过，rejected驳回',
  `audit_reason` varchar(500) DEFAULT NULL COMMENT '审核原因',
  `auditor_id` varchar(32) NOT NULL COMMENT '审核人ID，对应sys_user.id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_feedback_audit_log_target` (`target_type`, `target_id`, `created_at`, `id`),
  KEY `idx_ts_feedback_audit_log_feedback` (`feedback_id`, `created_at`, `id`),
  KEY `idx_ts_feedback_audit_log_auditor` (`auditor_id`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈内容审核日志表';

-- 回滚说明：
-- 1. DROP TABLE IF EXISTS `ts_feedback_audit_log`;
-- 2. 分别删除三张内容表的审核索引。
-- 3. 分别删除 `audit_status`、`audit_reason`、`audited_by`、`audited_at` 字段。
