CREATE TABLE IF NOT EXISTS `ts_ai_usage_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `invocation_id` varchar(64) NOT NULL COMMENT '单次AI调用唯一标识，用于幂等',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '跨服务链路追踪ID',
  `parent_invocation_id` varchar(64) DEFAULT NULL COMMENT '父调用唯一标识',
  `user_id` varchar(64) DEFAULT NULL COMMENT '内部关联用户ID',
  `tenant_id` int DEFAULT NULL COMMENT '租户ID',
  `source_type` varchar(32) NOT NULL COMMENT '调用来源：agent/chat/tool/system',
  `scene_code` varchar(64) NOT NULL COMMENT '业务场景编码',
  `modality` varchar(32) NOT NULL COMMENT '模态：text/image/audio/video/3d/multimodal',
  `operation_type` varchar(64) NOT NULL COMMENT '操作类型：chat_completion/image_generate/tts等',
  `provider` varchar(64) DEFAULT NULL COMMENT '模型或能力供应商',
  `model_id` varchar(64) DEFAULT NULL COMMENT '内部模型配置ID',
  `model_name` varchar(128) DEFAULT NULL COMMENT '实际调用模型名称',
  `session_id` bigint DEFAULT NULL COMMENT '业务会话ID',
  `message_id` bigint DEFAULT NULL COMMENT '业务消息ID',
  `run_id` varchar(64) DEFAULT NULL COMMENT 'Agent或任务运行ID',
  `agent_name` varchar(128) DEFAULT NULL COMMENT 'Agent名称',
  `node_name` varchar(128) DEFAULT NULL COMMENT '节点名称',
  `tool_name` varchar(128) DEFAULT NULL COMMENT 'Tool名称',
  `status` varchar(16) NOT NULL DEFAULT 'running' COMMENT '状态：running/success/failed/cancelled',
  `started_at` datetime(3) NOT NULL COMMENT '调用开始时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '调用结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '调用耗时毫秒',
  `error_code` varchar(64) DEFAULT NULL COMMENT '内部错误编码',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '脱敏后的内部错误摘要',
  `usage_raw_json` longtext DEFAULT NULL COMMENT '脱敏后的供应商原始用量JSON',
  `ext_json` longtext DEFAULT NULL COMMENT '扩展信息JSON，不保存Prompt、生成正文或密钥',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0否 1是',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_ai_usage_record_invocation_id` (`invocation_id`),
  KEY `idx_ts_ai_usage_record_trace_id` (`trace_id`),
  KEY `idx_ts_ai_usage_record_user_time` (`user_id`, `started_at`),
  KEY `idx_ts_ai_usage_record_run_id` (`run_id`),
  KEY `idx_ts_ai_usage_record_session_message` (`session_id`, `message_id`),
  KEY `idx_ts_ai_usage_record_source_scene` (`source_type`, `scene_code`),
  KEY `idx_ts_ai_usage_record_modality_operation` (`modality`, `operation_type`),
  KEY `idx_ts_ai_usage_record_provider_model` (`provider`, `model_name`),
  KEY `idx_ts_ai_usage_record_status_time` (`status`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI模型及多模态能力使用记录主表';

CREATE TABLE IF NOT EXISTS `ts_ai_usage_metric` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `usage_record_id` bigint NOT NULL COMMENT 'AI使用记录ID',
  `metric_code` varchar(64) NOT NULL COMMENT '指标编码，如input_tokens/image_count/points',
  `metric_value` decimal(24,6) NOT NULL COMMENT '指标值，兼容整数与小数',
  `metric_unit` varchar(32) NOT NULL COMMENT '单位：token/count/second/character/point/USD等',
  `metric_scope` varchar(32) NOT NULL DEFAULT 'total' COMMENT '指标范围：input/output/cache/total/billing',
  `ext_json` longtext DEFAULT NULL COMMENT '指标扩展JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ts_ai_usage_metric_record_code_scope` (`usage_record_id`, `metric_code`, `metric_scope`),
  KEY `idx_ts_ai_usage_metric_code_unit` (`metric_code`, `metric_unit`),
  KEY `idx_ts_ai_usage_metric_record_id` (`usage_record_id`),
  KEY `idx_ts_ai_usage_metric_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI模型及多模态能力计量指标明细表';

-- 示例数据默认不写入。测试环境可在执行本文件前运行：
-- SET @include_demo_data = 1;
SET @include_demo_data = COALESCE(@include_demo_data, 0);

INSERT IGNORE INTO `ts_ai_usage_record` (
  `id`, `invocation_id`, `trace_id`, `parent_invocation_id`, `user_id`, `tenant_id`,
  `source_type`, `scene_code`, `modality`, `operation_type`,
  `provider`, `model_id`, `model_name`,
  `session_id`, `message_id`, `run_id`, `agent_name`, `node_name`, `tool_name`,
  `status`, `started_at`, `finished_at`, `duration_ms`,
  `error_code`, `error_message`, `usage_raw_json`, `ext_json`
)
SELECT *
FROM (
  SELECT
    990000000000000001 AS `id`,
    'demo-agent-text-001' AS `invocation_id`,
    'demo-trace-agent-001' AS `trace_id`,
    NULL AS `parent_invocation_id`,
    'demo-user-001' AS `user_id`,
    0 AS `tenant_id`,
    'agent' AS `source_type`,
    'admin_chat' AS `scene_code`,
    'text' AS `modality`,
    'chat_completion' AS `operation_type`,
    'DEEPSEEK' AS `provider`,
    'demo-model-deepseek-001' AS `model_id`,
    'deepseek-v4-flash' AS `model_name`,
    900001 AS `session_id`,
    910001 AS `message_id`,
    'demo-run-agent-001' AS `run_id`,
    'role_task_agent' AS `agent_name`,
    'role_create_dialogue' AS `node_name`,
    NULL AS `tool_name`,
    'success' AS `status`,
    '2026-08-04 10:00:00.000' AS `started_at`,
    '2026-08-04 10:00:02.180' AS `finished_at`,
    2180 AS `duration_ms`,
    NULL AS `error_code`,
    NULL AS `error_message`,
    '{"prompt_tokens":2400,"completion_tokens":350,"total_tokens":2750,"prompt_cache_hit_tokens":800,"prompt_cache_miss_tokens":1600}' AS `usage_raw_json`,
    '{"finishReason":"STOP"}' AS `ext_json`
  UNION ALL
  SELECT
    990000000000000002,
    'demo-chat-text-001',
    'demo-trace-chat-001',
    NULL,
    'demo-user-001',
    0,
    'chat',
    'normal_chat',
    'text',
    'chat_completion',
    'DEEPSEEK',
    'demo-model-deepseek-002',
    'deepseek-chat',
    900002,
    910002,
    NULL,
    NULL,
    NULL,
    NULL,
    'success',
    '2026-08-04 10:05:00.000',
    '2026-08-04 10:05:01.060',
    1060,
    NULL,
    NULL,
    '{"prompt_tokens":800,"completion_tokens":120,"total_tokens":920,"prompt_cache_hit_tokens":300,"prompt_cache_miss_tokens":500}',
    '{"finishReason":"STOP"}'
  UNION ALL
  SELECT
    990000000000000003,
    'demo-role-image-001',
    'demo-trace-agent-001',
    'demo-agent-text-001',
    'demo-user-001',
    0,
    'tool',
    'role_image_generate',
    'image',
    'image_generate',
    'MINIMAX',
    'demo-model-image-001',
    'image-01',
    900001,
    910001,
    'demo-run-agent-001',
    'role_image_task_agent',
    'role_create_image',
    'role_generate_role_image',
    'success',
    '2026-08-04 10:00:03.000',
    '2026-08-04 10:00:18.420',
    15420,
    NULL,
    NULL,
    '{"image_count":1,"width":1024,"height":1536}',
    '{"async":true,"resolution":"1024x1536"}'
  UNION ALL
  SELECT
    990000000000000004,
    'demo-tts-001',
    'demo-trace-tts-001',
    NULL,
    'demo-user-001',
    0,
    'chat',
    'chat_tts',
    'audio',
    'tts',
    'MINIMAX',
    'demo-model-voice-001',
    'speech-02-hd',
    900002,
    910003,
    NULL,
    NULL,
    NULL,
    NULL,
    'success',
    '2026-08-04 10:06:00.000',
    '2026-08-04 10:06:03.300',
    3300,
    NULL,
    NULL,
    '{"text_characters":120,"output_audio_duration":12.6}',
    '{"voiceId":"demo-voice-001","format":"mp3"}'
  UNION ALL
  SELECT
    990000000000000005,
    'demo-video-001',
    'demo-trace-video-001',
    NULL,
    'demo-user-002',
    0,
    'tool',
    'story_video_generate',
    'video',
    'video_generate',
    'MINIMAX',
    'demo-model-video-001',
    'video-01',
    NULL,
    NULL,
    'demo-run-video-001',
    NULL,
    'story_video_generate',
    'story_generate_video',
    'success',
    '2026-08-04 10:10:00.000',
    '2026-08-04 10:11:12.500',
    72500,
    NULL,
    NULL,
    '{"video_count":1,"video_duration":5}',
    '{"async":true,"resolution":"1280x720"}'
  UNION ALL
  SELECT
    990000000000000006,
    'demo-3d-001',
    'demo-trace-3d-001',
    NULL,
    'demo-user-002',
    0,
    'tool',
    'role_3d_generate',
    '3d',
    '3d_generate',
    'DEMO_3D',
    'demo-model-3d-001',
    '3d-model-demo',
    NULL,
    NULL,
    'demo-run-3d-001',
    NULL,
    'role_3d_generate',
    'role_generate_3d',
    'failed',
    '2026-08-04 10:15:00.000',
    '2026-08-04 10:15:08.900',
    8900,
    'PROVIDER_TIMEOUT',
    'Provider request timed out',
    '{"request_count":1}',
    '{"async":true}'
) AS `demo_usage_record`
WHERE @include_demo_data = 1;

INSERT IGNORE INTO `ts_ai_usage_metric` (
  `id`, `usage_record_id`, `metric_code`, `metric_value`, `metric_unit`, `metric_scope`, `ext_json`
)
SELECT *
FROM (
  SELECT 991000000000000001 AS `id`, 990000000000000001 AS `usage_record_id`, 'input_tokens' AS `metric_code`, 2400.000000 AS `metric_value`, 'token' AS `metric_unit`, 'input' AS `metric_scope`, NULL AS `ext_json`
  UNION ALL SELECT 991000000000000002, 990000000000000001, 'output_tokens', 350.000000, 'token', 'output', NULL
  UNION ALL SELECT 991000000000000003, 990000000000000001, 'total_tokens', 2750.000000, 'token', 'total', NULL
  UNION ALL SELECT 991000000000000004, 990000000000000001, 'cache_hit_tokens', 800.000000, 'token', 'cache', NULL
  UNION ALL SELECT 991000000000000005, 990000000000000001, 'cache_miss_tokens', 1600.000000, 'token', 'cache', NULL
  UNION ALL SELECT 991000000000000006, 990000000000000001, 'request_count', 1.000000, 'count', 'total', NULL

  UNION ALL SELECT 991000000000000007, 990000000000000002, 'input_tokens', 800.000000, 'token', 'input', NULL
  UNION ALL SELECT 991000000000000008, 990000000000000002, 'output_tokens', 120.000000, 'token', 'output', NULL
  UNION ALL SELECT 991000000000000009, 990000000000000002, 'total_tokens', 920.000000, 'token', 'total', NULL
  UNION ALL SELECT 991000000000000010, 990000000000000002, 'cache_hit_tokens', 300.000000, 'token', 'cache', NULL
  UNION ALL SELECT 991000000000000011, 990000000000000002, 'cache_miss_tokens', 500.000000, 'token', 'cache', NULL
  UNION ALL SELECT 991000000000000012, 990000000000000002, 'request_count', 1.000000, 'count', 'total', NULL

  UNION ALL SELECT 991000000000000013, 990000000000000003, 'request_count', 1.000000, 'count', 'total', NULL
  UNION ALL SELECT 991000000000000014, 990000000000000003, 'image_count', 1.000000, 'count', 'output', NULL
  UNION ALL SELECT 991000000000000015, 990000000000000003, 'points', 8.000000, 'point', 'billing', NULL

  UNION ALL SELECT 991000000000000016, 990000000000000004, 'request_count', 1.000000, 'count', 'total', NULL
  UNION ALL SELECT 991000000000000017, 990000000000000004, 'text_characters', 120.000000, 'character', 'input', NULL
  UNION ALL SELECT 991000000000000018, 990000000000000004, 'output_audio_duration', 12.600000, 'second', 'output', NULL
  UNION ALL SELECT 991000000000000019, 990000000000000004, 'points', 2.000000, 'point', 'billing', NULL

  UNION ALL SELECT 991000000000000020, 990000000000000005, 'request_count', 1.000000, 'count', 'total', NULL
  UNION ALL SELECT 991000000000000021, 990000000000000005, 'video_count', 1.000000, 'count', 'output', NULL
  UNION ALL SELECT 991000000000000022, 990000000000000005, 'video_duration', 5.000000, 'second', 'output', NULL
  UNION ALL SELECT 991000000000000023, 990000000000000005, 'points', 20.000000, 'point', 'billing', NULL

  UNION ALL SELECT 991000000000000024, 990000000000000006, 'request_count', 1.000000, 'count', 'total', NULL
) AS `demo_usage_metric`
WHERE @include_demo_data = 1;
