-- Add GEMINI provider to model_provider dictionary
INSERT INTO sys_dict_item (
  id, dict_id, item_text, item_value, description, sort_order, status, create_by, create_time, update_by, update_time, item_color
)
SELECT
  REPLACE(UUID(), '-', ''), d.id, 'Gemini', 'GEMINI', NULL, 1, 1, 'jeecg', NOW(), NULL, NULL, NULL
FROM sys_dict d
WHERE d.dict_code = 'model_provider'
  AND NOT EXISTS (
    SELECT 1 FROM sys_dict_item i WHERE i.dict_id = d.id AND i.item_value = 'GEMINI'
  );