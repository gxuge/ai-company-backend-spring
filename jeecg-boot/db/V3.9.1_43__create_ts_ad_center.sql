SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS ts_ad_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slot_code VARCHAR(64) NOT NULL COMMENT '广告位编码',
    slot_name VARCHAR(100) NOT NULL COMMENT '广告位名称',
    slot_type VARCHAR(24) NOT NULL COMMENT 'BANNER/POSTER/POPUP/CAROUSEL',
    width INT NULL COMMENT '建议宽度',
    height INT NULL COMMENT '建议高度',
    max_items INT NOT NULL DEFAULT 1 COMMENT '单次最多返回内容数',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    description VARCHAR(500) NULL COMMENT '广告位说明',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1删除',
    created_by VARCHAR(32) NULL COMMENT '创建人',
    updated_by VARCHAR(32) NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ts_ad_slot_code (slot_code),
    KEY idx_ts_ad_slot_status (status, is_deleted, id)
) COMMENT='广告位配置';

CREATE TABLE IF NOT EXISTS ts_ad_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slot_id BIGINT NOT NULL COMMENT '广告位ID',
    content_code VARCHAR(64) NOT NULL COMMENT '广告内容编码',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    subtitle VARCHAR(500) NULL COMMENT '副标题',
    media_type VARCHAR(16) NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE',
    image_url VARCHAR(1000) NOT NULL COMMENT '图片地址',
    link_type VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/URL/ROUTE/ROLE/STORY',
    link_value VARCHAR(1000) NULL COMMENT '跳转目标',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    start_time DATETIME NULL COMMENT '投放开始时间',
    end_time DATETIME NULL COMMENT '投放结束时间',
    ext_json JSON NULL COMMENT '扩展展示参数',
    publish_at DATETIME NULL COMMENT '最近发布时间',
    offline_at DATETIME NULL COMMENT '最近下线时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1删除',
    created_by VARCHAR(32) NULL COMMENT '创建人',
    updated_by VARCHAR(32) NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ts_ad_content_code (content_code),
    KEY idx_ts_ad_content_delivery (slot_id, status, is_deleted, start_time, end_time),
    KEY idx_ts_ad_content_sort (slot_id, sort_order, id)
) COMMENT='广告内容';

CREATE TABLE IF NOT EXISTS ts_ad_delivery_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id BIGINT NOT NULL COMMENT '广告内容ID',
    platform_json JSON NOT NULL COMMENT '平台数组：ALL/WEB/IOS/ANDROID',
    audience_type VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/LOGIN/ANONYMOUS/USER_LIST',
    member_level_json JSON NOT NULL COMMENT '会员等级数组：ALL/FREE/PRO/ULTRA',
    user_id_json JSON NULL COMMENT '指定用户ID数组',
    created_by VARCHAR(32) NULL COMMENT '创建人',
    updated_by VARCHAR(32) NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ts_ad_rule_content (content_id)
) COMMENT='广告投放规则';

CREATE TABLE IF NOT EXISTS ts_ad_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL COMMENT '客户端事件幂等ID',
    content_id BIGINT NOT NULL COMMENT '广告内容ID',
    slot_code VARCHAR(64) NOT NULL COMMENT '广告位编码快照',
    event_type VARCHAR(16) NOT NULL COMMENT 'IMPRESSION/CLICK',
    user_id VARCHAR(32) NULL COMMENT '登录用户ID',
    visitor_id VARCHAR(64) NULL COMMENT '匿名访客ID',
    platform VARCHAR(16) NOT NULL COMMENT 'WEB/IOS/ANDROID',
    occurred_at DATETIME NOT NULL COMMENT '事件发生时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ts_ad_event_id (event_id),
    KEY idx_ts_ad_event_content_type_time (content_id, event_type, occurred_at),
    KEY idx_ts_ad_event_slot_type_time (slot_code, event_type, occurred_at)
) COMMENT='广告曝光点击事件';

-- 影响范围：仅新增 ts_ad_* 表，不修改现有作品、评论、会员与积分数据。
-- 回滚顺序：
-- DROP TABLE IF EXISTS ts_ad_event;
-- DROP TABLE IF EXISTS ts_ad_delivery_rule;
-- DROP TABLE IF EXISTS ts_ad_content;
-- DROP TABLE IF EXISTS ts_ad_slot;
