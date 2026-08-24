SET NAMES utf8mb4;

ALTER TABLE ts_ad_content
    ADD COLUMN source_type VARCHAR(16) NOT NULL DEFAULT 'SELF'
        COMMENT 'SELF/EXTERNAL/AD_NETWORK' AFTER content_code,
    ADD COLUMN media_url VARCHAR(1000) NULL
        COMMENT '规范化素材地址，卡片类型为空' AFTER media_type,
    ADD COLUMN poster_url VARCHAR(1000) NULL
        COMMENT '视频封面地址' AFTER media_url,
    ADD COLUMN card_type VARCHAR(32) NULL
        COMMENT 'PROMOTION/ROLE/STORY/CUSTOM' AFTER poster_url,
    ADD COLUMN payload_json JSON NULL
        COMMENT '卡片内容JSON对象' AFTER card_type,
    ADD COLUMN action_type VARCHAR(16) NOT NULL DEFAULT 'NONE'
        COMMENT 'NONE/URL/ROUTE/ROLE/STORY/DEEP_LINK' AFTER payload_json,
    ADD COLUMN action_payload VARCHAR(1000) NULL
        COMMENT '动作目标' AFTER action_type;

ALTER TABLE ts_ad_content
    MODIFY COLUMN image_url VARCHAR(1000) NULL COMMENT '兼容旧版本的图片地址';

UPDATE ts_ad_content
SET source_type = 'SELF',
    media_url = image_url,
    action_type = COALESCE(link_type, 'NONE'),
    action_payload = link_value
WHERE media_url IS NULL;

-- 影响范围：仅扩展 ts_ad_content 字段，并将既有图片和跳转字段复制到规范化字段。
-- 回滚前提：先导出并删除媒体类型为VIDEO/CARD的数据；否则无法安全恢复 image_url 非空约束。
-- 回滚示例：
-- ALTER TABLE ts_ad_content
--     MODIFY COLUMN image_url VARCHAR(1000) NOT NULL COMMENT '图片地址';
-- ALTER TABLE ts_ad_content
--     DROP COLUMN action_payload,
--     DROP COLUMN action_type,
--     DROP COLUMN payload_json,
--     DROP COLUMN card_type,
--     DROP COLUMN poster_url,
--     DROP COLUMN media_url,
--     DROP COLUMN source_type;
