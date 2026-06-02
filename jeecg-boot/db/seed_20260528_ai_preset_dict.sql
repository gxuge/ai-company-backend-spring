-- AI 预设管理相关字典初始化
-- 执行时间：2026-05-28

SET @dict_code_target := 'airag_target_type';
SET @dict_id_target := (SELECT id FROM sys_dict WHERE dict_code = @dict_code_target LIMIT 1);
SET @dict_id_target := IFNULL(@dict_id_target, REPLACE(UUID(), '-', ''));

INSERT INTO sys_dict (
  id, dict_name, dict_code, description, del_flag, create_by, create_time, update_by, update_time, type, tenant_id, low_app_id
) VALUES (
  @dict_id_target, '生成目标类型', @dict_code_target, 'generation_preset.target_type 字典', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, NULL
)
ON DUPLICATE KEY UPDATE
  dict_name = VALUES(dict_name),
  description = VALUES(description),
  del_flag = 0,
  update_by = 'admin',
  update_time = NOW();

DELETE FROM sys_dict_item WHERE dict_id = @dict_id_target AND item_value IN ('character', 'story', 'both');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, item_color, description, sort_order, status, create_by, create_time, update_by, update_time)
VALUES
  (REPLACE(UUID(), '-', ''), @dict_id_target, '角色', 'character', NULL, '角色生成', 1, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_target, '故事', 'story', NULL, '故事生成', 2, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_target, '角色+故事', 'both', NULL, '角色与故事均可', 3, 1, 'admin', NOW(), 'admin', NOW());

SET @dict_code_required := 'airag_required_flag';
SET @dict_id_required := (SELECT id FROM sys_dict WHERE dict_code = @dict_code_required LIMIT 1);
SET @dict_id_required := IFNULL(@dict_id_required, REPLACE(UUID(), '-', ''));

INSERT INTO sys_dict (
  id, dict_name, dict_code, description, del_flag, create_by, create_time, update_by, update_time, type, tenant_id, low_app_id
) VALUES (
  @dict_id_required, '是否必选', @dict_code_required, 'generation_preset_tag.required 字典', 0, 'admin', NOW(), 'admin', NOW(), 1, 0, NULL
)
ON DUPLICATE KEY UPDATE
  dict_name = VALUES(dict_name),
  description = VALUES(description),
  del_flag = 0,
  update_by = 'admin',
  update_time = NOW();

DELETE FROM sys_dict_item WHERE dict_id = @dict_id_required AND item_value IN ('0', '1');
INSERT INTO sys_dict_item (id, dict_id, item_text, item_value, item_color, description, sort_order, status, create_by, create_time, update_by, update_time)
VALUES
  (REPLACE(UUID(), '-', ''), @dict_id_required, '否', '0', NULL, '非必选', 1, 1, 'admin', NOW(), 'admin', NOW()),
  (REPLACE(UUID(), '-', ''), @dict_id_required, '是', '1', NULL, '必选', 2, 1, 'admin', NOW(), 'admin', NOW());

