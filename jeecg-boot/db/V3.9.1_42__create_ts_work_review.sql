SET NAMES utf8mb4;
START TRANSACTION;

ALTER TABLE `ts_role_info`
  ADD COLUMN `content_version` int NOT NULL DEFAULT 1 COMMENT '作品内容版本，每次保存递增' AFTER `status`,
  ADD COLUMN `review_status` varchar(24) NOT NULL DEFAULT 'APPROVED' COMMENT '当前版本审核状态' AFTER `content_version`,
  ADD COLUMN `current_review_id` bigint unsigned DEFAULT NULL COMMENT '当前版本审核任务ID' AFTER `review_status`,
  ADD COLUMN `desired_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '作者期望公开：1是，0否' AFTER `current_review_id`,
  ADD KEY `idx_ts_role_review` (`review_status`, `status`, `id`);

UPDATE `ts_role_info`
SET `desired_public` = COALESCE(`is_public`, 0);

ALTER TABLE `ts_story_info`
  ADD COLUMN `content_version` int NOT NULL DEFAULT 1 COMMENT '作品内容版本，每次保存递增' AFTER `status`,
  ADD COLUMN `review_status` varchar(24) NOT NULL DEFAULT 'APPROVED' COMMENT '当前版本审核状态' AFTER `content_version`,
  ADD COLUMN `current_review_id` bigint unsigned DEFAULT NULL COMMENT '当前版本审核任务ID' AFTER `review_status`,
  ADD COLUMN `desired_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '作者期望公开：1是，0否' AFTER `current_review_id`,
  ADD KEY `idx_ts_story_review` (`review_status`, `is_deleted`, `status`, `id`);

UPDATE `ts_story_info`
SET `desired_public` = COALESCE(`is_public`, 0);

CREATE TABLE `ts_work_review` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '审核任务主键',
  `review_no` varchar(40) NOT NULL COMMENT '审核任务编号',
  `work_type` varchar(16) NOT NULL COMMENT '作品类型：ROLE角色，STORY故事',
  `work_id` bigint unsigned NOT NULL COMMENT '作品ID',
  `owner_user_id` varchar(32) NOT NULL COMMENT '作品作者用户ID',
  `work_version` int NOT NULL COMMENT '被审核作品版本',
  `requested_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '该版本作者期望公开',
  `snapshot_json` longtext NOT NULL COMMENT '后端重建的不可变作品快照JSON',
  `snapshot_hash` char(64) NOT NULL COMMENT '快照UTF-8字节SHA-256',
  `status` varchar(24) NOT NULL COMMENT 'PENDING_AI/PENDING_ADMIN/APPROVED/REJECTED/OBSOLETE',
  `ai_decision` varchar(16) DEFAULT NULL COMMENT 'AI结论：PASS/MANUAL/BLOCK',
  `ai_risk_level` varchar(16) DEFAULT NULL COMMENT 'AI风险：LOW/MEDIUM/HIGH',
  `ai_reason` varchar(1000) DEFAULT NULL COMMENT 'AI审核原因或调用错误',
  `ai_result_json` text DEFAULT NULL COMMENT 'AI原始结构化结果',
  `ai_reviewed_at` datetime DEFAULT NULL COMMENT 'AI审核完成时间',
  `admin_reviewer_id` varchar(32) DEFAULT NULL COMMENT '管理员审核人ID',
  `admin_reason` varchar(1000) DEFAULT NULL COMMENT '管理员审核意见',
  `admin_reviewed_at` datetime DEFAULT NULL COMMENT '管理员审核时间',
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交审核时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_work_review_no` (`review_no`),
  UNIQUE KEY `uk_ts_work_review_version` (`work_type`, `work_id`, `work_version`),
  KEY `idx_ts_work_review_status` (`status`, `submitted_at`, `id`),
  KEY `idx_ts_work_review_owner` (`owner_user_id`, `submitted_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色与故事作品审核任务表';

CREATE TABLE `ts_work_review_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '审核项主键',
  `review_id` bigint unsigned NOT NULL COMMENT '审核任务ID',
  `item_type` varchar(16) NOT NULL COMMENT '审核项类型：TEXT/IMAGE',
  `field_code` varchar(64) NOT NULL COMMENT '作品字段编码',
  `content_text` longtext DEFAULT NULL COMMENT '文本内容快照',
  `asset_url` varchar(1000) DEFAULT NULL COMMENT '图片正式地址快照',
  `content_hash` char(64) NOT NULL COMMENT '审核项内容SHA-256',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_work_review_item_review` (`review_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品审核文本与图片项';

CREATE TABLE `ts_work_review_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '审核日志主键',
  `review_id` bigint unsigned NOT NULL COMMENT '审核任务ID',
  `action_type` varchar(32) NOT NULL COMMENT 'SUBMIT/AI_PASS/AI_BLOCK/AI_ERROR/ADMIN_APPROVE/ADMIN_REJECT/OBSOLETE',
  `before_status` varchar(24) DEFAULT NULL COMMENT '变更前状态',
  `after_status` varchar(24) NOT NULL COMMENT '变更后状态',
  `operator_type` varchar(16) NOT NULL COMMENT 'SYSTEM/AI/ADMIN',
  `operator_id` varchar(32) DEFAULT NULL COMMENT '操作人ID',
  `reason` varchar(1000) DEFAULT NULL COMMENT '操作原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ts_work_review_log_review` (`review_id`, `created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品审核状态流转日志';

INSERT INTO airag_prompts (
    id, name, prompt_key, description, content, category, tags, model_id, model_param,
    status, version, del_flag, create_by, create_time, update_by, update_time, sys_org_code, tenant_id
)
VALUES (
    REPLACE(UUID(), '-', ''),
    'ts_work_review_v1',
    'ts_work_review',
    '角色与故事作品AI初审模板 v1',
    'TEMPLATE_BEGIN::ts_work_review::v1
SECTION::meta
code=ts_work_review
version=v1
scenario=work_review
description=审核角色或故事固定快照中的文本和图片地址。
output_mode=tool_call
tool_name=submit_work_review
strict=true

SECTION::developer_prompt
你是内容安全初审员。请基于作品固定快照判断是否存在色情、未成年人不当内容、极端暴力、自残、仇恨歧视、违法犯罪指导、隐私泄露、冒充现实人物或其他明显平台风险。
PASS 仅用于没有发现明显风险；BLOCK 仅用于明确违规；边界不清、需要结合图片人工查看或存在版权与现实人物疑点时使用 MANUAL。
不得修改作品，不得输出快照之外的信息。必须且只能调用一次 submit_work_review。

SECTION::user_prompt_template
{
  "work_type": "{{work_type}}",
  "snapshot_hash": "{{snapshot_hash}}",
  "snapshot_json": {{snapshot_json}}
}

SECTION::tool_schema
{
  "name": "submit_work_review",
  "strict": true,
  "description": "提交作品AI初审结果。",
  "parameters": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "decision": {
        "type": "string",
        "enum": ["PASS", "MANUAL", "BLOCK"],
        "description": "AI初审结论"
      },
      "risk_level": {
        "type": "string",
        "enum": ["LOW", "MEDIUM", "HIGH"],
        "description": "综合风险等级"
      },
      "reason": {
        "type": "string",
        "description": "简明说明依据；不得为空"
      }
    },
    "required": ["decision", "risk_level", "reason"]
  }
}
TEMPLATE_END::ts_work_review::v1',
    NULL, NULL, NULL, NULL, '1', 'v1', 0, 'admin', NOW(), 'admin', NOW(), NULL, NULL
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    content = VALUES(content),
    status = '1',
    del_flag = 0,
    update_by = 'admin',
    update_time = NOW();

COMMIT;

-- 影响范围：新增三张作品审核表，并为角色、故事增加当前版本审核门禁字段。
-- 回滚方案：删除 ts_work_review_log、ts_work_review_item、ts_work_review 和 ts_work_review Prompt；
-- 再删除两张作品表的 idx_ts_*_review 索引及 content_version、review_status、
-- current_review_id、desired_public 字段。回滚前应先确认是否需要导出审核历史。
