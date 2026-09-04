SET NAMES utf8mb4;
START TRANSACTION;

DROP TABLE IF EXISTS `ts_content_tag_task`;
DROP TABLE IF EXISTS `ts_content_tag`;
DROP TABLE IF EXISTS `ts_tag`;
DROP TABLE IF EXISTS `ts_tag_type`;

CREATE TABLE `ts_tag_type` (
  `id` varchar(64) NOT NULL COMMENT '标签类型编码',
  `name` varchar(64) NOT NULL COMMENT '标签类型名称',
  `scope` varchar(16) NOT NULL COMMENT '内容类型：role|story',
  `description` varchar(500) DEFAULT NULL COMMENT '类型描述',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `version` int NOT NULL DEFAULT 1 COMMENT '词典版本',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_tag_type_scope_name` (`scope`, `name`),
  KEY `idx_ts_tag_type_scope_enabled` (`scope`, `enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色与故事固定标签类型';

CREATE TABLE `ts_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `scope` varchar(16) NOT NULL COMMENT '内容类型：role|story',
  `type_id` varchar(64) NOT NULL COMMENT '标签类型编码',
  `name` varchar(64) NOT NULL COMMENT '标签名称',
  `description` varchar(500) DEFAULT NULL COMMENT '标签说明',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0停用',
  `version` int NOT NULL DEFAULT 1 COMMENT '词典版本',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_tag_type_name` (`type_id`, `name`),
  KEY `idx_ts_tag_scope_type_enabled` (`scope`, `type_id`, `enabled`, `sort_order`),
  CONSTRAINT `fk_ts_tag_type_id` FOREIGN KEY (`type_id`) REFERENCES `ts_tag_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色与故事固定标签词典';

CREATE TABLE `ts_content_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content_type` varchar(16) NOT NULL COMMENT '内容类型：role|story',
  `content_id` bigint NOT NULL COMMENT '角色或故事ID',
  `content_version` int NOT NULL COMMENT '内容版本',
  `tag_id` bigint NOT NULL COMMENT '固定标签ID',
  `score` decimal(5,4) NOT NULL COMMENT '标签匹配分数：0到1',
  `source` varchar(32) NOT NULL COMMENT '来源：generation|ai_fallback|manual',
  `model_version` varchar(128) DEFAULT NULL COMMENT '模型或提示词版本',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容快照哈希',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_content_tag` (`content_type`, `content_id`, `tag_id`),
  KEY `idx_ts_content_tag_version` (`content_type`, `content_id`, `content_version`),
  KEY `idx_ts_content_tag_tag` (`tag_id`, `score`),
  CONSTRAINT `fk_ts_content_tag_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `ts_tag` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色与故事内容标签';

CREATE TABLE `ts_content_tag_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `review_id` bigint NOT NULL COMMENT '作品审核快照ID',
  `content_type` varchar(16) NOT NULL COMMENT '内容类型：role|story',
  `content_id` bigint NOT NULL COMMENT '角色或故事ID',
  `content_version` int NOT NULL COMMENT '内容版本',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '审核快照哈希',
  `status` varchar(16) NOT NULL DEFAULT 'pending' COMMENT '状态：pending|running|success|failed|skipped',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '执行次数',
  `last_error_message` varchar(500) DEFAULT NULL COMMENT '最近错误摘要',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_content_tag_task_review` (`review_id`),
  KEY `idx_ts_content_tag_task_retry` (`status`, `retry_count`, `updated_at`),
  KEY `idx_ts_content_tag_task_content` (`content_type`, `content_id`, `content_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容标签异步任务';

INSERT INTO `ts_tag_type` (`id`, `name`, `scope`, `description`, `sort_order`) VALUES
  ('personality', '性格', 'role', '角色稳定人格和处事倾向', 10),
  ('interaction_style', '互动风格', 'role', '角色与用户互动时的主要方式', 20),
  ('aura', '外在气质', 'role', '角色整体形象和外在感受', 30),
  ('genre', '题材', 'story', '故事所属题材方向', 110),
  ('mood', '情绪基调', 'story', '故事整体情绪氛围', 120),
  ('pace', '节奏', 'story', '故事主要推进节奏', 130),
  ('experience', '内容体验', 'story', '用户阅读和互动时的核心体验', 140);

INSERT INTO `ts_tag` (`scope`, `type_id`, `name`, `sort_order`) VALUES
  ('role', 'personality', '温柔', 10),
  ('role', 'personality', '冷淡', 20),
  ('role', 'personality', '傲娇', 30),
  ('role', 'personality', '活泼', 40),
  ('role', 'personality', '成熟', 50),
  ('role', 'personality', '强势', 60),
  ('role', 'personality', '腹黑', 70),
  ('role', 'personality', '内向', 80),
  ('role', 'interaction_style', '治愈', 10),
  ('role', 'interaction_style', '黏人', 20),
  ('role', 'interaction_style', '主动', 30),
  ('role', 'interaction_style', '宠溺', 40),
  ('role', 'interaction_style', '幽默', 50),
  ('role', 'interaction_style', '陪伴', 60),
  ('role', 'interaction_style', '毒舌', 70),
  ('role', 'aura', '可爱', 10),
  ('role', 'aura', '清冷', 20),
  ('role', 'aura', '成熟', 30),
  ('role', 'aura', '优雅', 40),
  ('role', 'aura', '阳光', 50),
  ('role', 'aura', '神秘', 60),
  ('role', 'aura', '御姐', 70),
  ('story', 'genre', '恋爱', 10),
  ('story', 'genre', '校园', 20),
  ('story', 'genre', '悬疑', 30),
  ('story', 'genre', '奇幻', 40),
  ('story', 'genre', '都市', 50),
  ('story', 'genre', '冒险', 60),
  ('story', 'mood', '温馨', 10),
  ('story', 'mood', '治愈', 20),
  ('story', 'mood', '欢乐', 30),
  ('story', 'mood', '悲伤', 40),
  ('story', 'mood', '压抑', 50),
  ('story', 'mood', '紧张', 60),
  ('story', 'pace', '慢热', 10),
  ('story', 'pace', '快节奏', 20),
  ('story', 'pace', '日常', 30),
  ('story', 'pace', '强剧情', 40),
  ('story', 'pace', '反转', 50),
  ('story', 'experience', '甜蜜', 10),
  ('story', 'experience', '爽感', 20),
  ('story', 'experience', '虐心', 30),
  ('story', 'experience', '烧脑', 40),
  ('story', 'experience', '刺激', 50),
  ('story', 'experience', '沉浸', 60);

INSERT INTO `airag_prompts`
  (`id`, `name`, `prompt_key`, `description`, `content`, `category`, `tags`, `model_id`,
   `model_param`, `status`, `version`, `del_flag`, `create_by`, `create_time`, `update_by`,
   `update_time`, `sys_org_code`, `tenant_id`)
VALUES (
  REPLACE(UUID(), '-', ''),
  'ts_content_tagging_v1',
  'ts_content_tagging',
  '根据固定标签词典为角色或故事内容打分',
  'TEMPLATE_BEGIN::ts_content_tagging::v1

SECTION::meta
code=ts_content_tagging
version=v1
scenario=content_tagging
description=从固定词典中选择与内容匹配的标签。
output_mode=tool_call
tool_name=submit_content_tags
strict=true

SECTION::developer_prompt
你是内容标签分类器。
只能从输入的固定标签词典中选择标签，不得创造新标签。
每个标签类型最多返回3个标签，只返回匹配分数大于等于0.5的结果。
score表示内容与标签的匹配程度，范围为0到1。
必须且只能调用 submit_content_tags 工具。

SECTION::user_prompt_template
{
  "content_type": "{{content_type}}",
  "tag_dictionary": {{tag_dictionary_json}},
  "content_snapshot": {{snapshot_json}}
}

SECTION::tool_schema
{
  "name": "submit_content_tags",
  "strict": true,
  "parameters": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "tags": {
        "type": "array",
        "items": {
          "type": "object",
          "additionalProperties": false,
          "properties": {
            "type_code": {"type": "string"},
            "name": {"type": "string"},
            "score": {"type": "number", "minimum": 0, "maximum": 1}
          },
          "required": ["type_code", "name", "score"]
        }
      }
    },
    "required": ["tags"]
  }
}

TEMPLATE_END::ts_content_tagging::v1',
  'tagging', NULL, NULL, NULL, '1', 'v1', 0, 'admin', NOW(), 'admin', NOW(), NULL, NULL
)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `description` = VALUES(`description`),
  `content` = VALUES(`content`),
  `status` = '1',
  `del_flag` = 0,
  `update_by` = 'admin',
  `update_time` = NOW();

COMMIT;
