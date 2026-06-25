-- 为 ts_story_info 补充场景图片字段 scene_image_url

ALTER TABLE ts_story_info
    ADD COLUMN scene_image_url VARCHAR(512) NULL COMMENT '故事场景图片URL' AFTER cover_url;
