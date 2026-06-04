-- 对齐 ts_story_info 故事字段：storySetting -> siteSetting，并新增 plotOutline
-- 执行时间：2026-06-03

ALTER TABLE ts_story_info
  ADD COLUMN site_setting TEXT NULL COMMENT '故事设定/场景设定' AFTER story_mode,
  ADD COLUMN plot_outline TEXT NULL COMMENT '剧情大纲' AFTER scene_name_snapshot;

UPDATE ts_story_info
SET site_setting = story_setting
WHERE (site_setting IS NULL OR site_setting = '')
  AND story_setting IS NOT NULL
  AND story_setting <> '';

UPDATE ts_story_info
SET plot_outline = remark
WHERE (plot_outline IS NULL OR plot_outline = '')
  AND remark IS NOT NULL
  AND remark <> '';
