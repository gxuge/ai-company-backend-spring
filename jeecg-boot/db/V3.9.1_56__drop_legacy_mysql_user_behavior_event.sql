SET NAMES utf8mb4;

-- 行为明细已迁移到 ClickHouse，删除不再使用的 MySQL 旧表。
DROP TABLE IF EXISTS ts_user_behavior_event;

-- 影响范围：仅删除 MySQL 主数据源中的空旧表，不影响 ClickHouse 同名表。
-- 回滚：按 V3.9.1_44__create_ts_user_behavior_event.sql 的建表语句手动重建。
