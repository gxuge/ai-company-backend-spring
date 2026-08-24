SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS ts_user_behavior_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL COMMENT '事件幂等ID',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    event_version INT NOT NULL DEFAULT 1 COMMENT '事件结构版本',
    user_id VARCHAR(32) NULL COMMENT '登录用户ID',
    anonymous_id VARCHAR(64) NULL COMMENT '匿名访客ID，预留',
    session_id VARCHAR(64) NOT NULL COMMENT '访问会话ID',
    resource_type VARCHAR(64) NULL COMMENT '资源类型',
    resource_id VARCHAR(64) NULL COMMENT '资源ID',
    impression_id VARCHAR(64) NULL COMMENT '推荐曝光链路ID',
    position_index INT NULL COMMENT '内容在列表中的位置',
    page_path VARCHAR(500) NULL COMMENT '事件发生页面',
    platform VARCHAR(16) NOT NULL COMMENT 'WEB/IOS/ANDROID',
    duration_ms BIGINT NULL COMMENT '停留时长毫秒',
    properties_json JSON NULL COMMENT '事件扩展属性',
    occurred_at DATETIME NOT NULL COMMENT '客户端发生时间',
    received_at DATETIME NOT NULL COMMENT '服务端接收时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '明细入库时间',
    UNIQUE KEY uk_ts_behavior_event_id (event_id),
    KEY idx_ts_behavior_user_time (user_id, occurred_at),
    KEY idx_ts_behavior_resource_time (resource_type, resource_id, occurred_at),
    KEY idx_ts_behavior_type_time (event_type, occurred_at)
) COMMENT='推荐用户行为事件明细';

-- 影响范围：仅新增推荐行为明细表，不修改现有浏览历史、广告事件和业务数据。
-- 回滚：
-- DROP TABLE IF EXISTS ts_user_behavior_event;
