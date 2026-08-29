CREATE TABLE IF NOT EXISTS ts_user_behavior_event
(
    event_id String COMMENT '事件幂等ID',
    event_type LowCardinality(String) COMMENT '业务事件类型',
    event_version UInt16 DEFAULT 2 COMMENT '事件结构版本',
    user_id String COMMENT '登录用户ID',
    session_id String COMMENT '客户端或后端会话ID',
    resource_type Nullable(String) COMMENT 'role/story/role_image/story_background',
    resource_id Nullable(String) COMMENT '业务资源ID或生成快照ID',
    page_path Nullable(String) COMMENT '事件发生页面',
    platform LowCardinality(String) COMMENT 'WEB/IOS/ANDROID/SERVER',
    properties_json Nullable(String) COMMENT '限定扩展属性JSON',
    occurred_at DateTime64(3, 'Asia/Shanghai') COMMENT '事件发生时间',
    received_at DateTime64(3, 'Asia/Shanghai') COMMENT '服务端接收时间',
    created_at DateTime64(3, 'Asia/Shanghai') DEFAULT now64(3) COMMENT '明细写入时间'
)
ENGINE = ReplacingMergeTree(received_at)
PARTITION BY toYYYYMM(occurred_at)
ORDER BY (event_type, user_id, occurred_at, event_id)
TTL occurred_at + INTERVAL 365 DAY DELETE
COMMENT '用户业务行为分析明细';
