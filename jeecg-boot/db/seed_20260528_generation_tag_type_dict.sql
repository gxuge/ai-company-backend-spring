-- 生成标签类型字典初始化
-- 执行时间：2026-05-28

SET @dict_code_tag_type := 'airag_generation_tag_type';
SET @dict_id_tag_type := (SELECT id FROM sys_dict WHERE dict_code = @dict_code_tag_type LIMIT 1);
SET @dict_id_tag_type := IFNULL(@dict_id_tag_type, REPLACE(UUID(), '-', ''));

INSERT INTO sys_dict (
  id, dict_name, dict_code, description, del_flag, create_by, create_time, update_by, update_time, type, tenant_id, low_app_id
) VALUES (
  @dict_id_tag_type, 'AI生成标签类型', @dict_code_tag_type, 'ts_tag_type.id 字典', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, NULL
)
ON DUPLICATE KEY UPDATE
  dict_name = VALUES(dict_name),
  description = VALUES(description),
  del_flag = 0,
  update_by = 'admin',
  update_time = NOW();

DELETE FROM sys_dict_item
WHERE dict_id = @dict_id_tag_type
  AND item_value IN (
    'identity','gender','user_background','appearance','dress','personality','behavior','speech_style','goal','secret','ability','limitation',
    'title','story_background','story_rule','time_period','location','user_role','conflict','plot_hook','narrative_style','progression_mode','boundary_rule'
  );

INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, item_color, description, sort_order, status, create_by, create_time, update_by, update_time)
VALUES
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '身份/职业', 'identity', NULL, '角色身份、职业定位', 10, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '性别', 'gender', NULL, '角色性别特征（如男/女/未知）', 15, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '人物背景', 'user_background', NULL, '成长经历、身份来历、人生背景', 20, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '外貌气质', 'appearance', NULL, '五官、气质、形象特征', 30, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '穿着', 'dress', NULL, '服装、配饰、风格化穿搭', 40, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '性格', 'personality', NULL, '稳定人格特征与处事倾向', 50, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '行为习惯', 'behavior', NULL, '生活习惯、动作偏好、日常行为模式', 60, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '说话风格', 'speech_style', NULL, '措辞、语气、表达方式', 70, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '目标动机', 'goal', NULL, '阶段目标、长期追求、驱动因素', 80, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '隐藏信息', 'secret', NULL, '秘密设定、隐藏身份、未公开信息', 90, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '能力特长', 'ability', NULL, '技能、专长、擅长领域', 100, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '能力限制', 'limitation', NULL, '短板、代价、能力边界', 110, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '主题', 'title', NULL, '故事标题主题、核心命题', 210, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '故事背景', 'story_background', NULL, '世界观与故事起点', 220, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '故事规则', 'story_rule', NULL, '世界机制、互动规则、设定约束', 230, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '时间阶段', 'time_period', NULL, '时代与时间线阶段', 240, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '主要场所', 'location', NULL, '关键地点与活动空间', 250, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '用户身份', 'user_role', NULL, '用户在故事中的身份定位', 260, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '核心冲突', 'conflict', NULL, '矛盾与冲突主线', 270, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '剧情钩子', 'plot_hook', NULL, '驱动后续推进的钩子事件', 280, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '叙事风格', 'narrative_style', NULL, '叙述方式与风格基调', 290, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '推进模式', 'progression_mode', NULL, '剧情推进节奏与方式', 300, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_tag_type, '故事边界', 'boundary_rule', NULL, '内容边界与禁止项', 310, 1, 'admin', NOW(), 'admin', NOW());
