SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS ts_recommend_etl_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    recommend_type VARCHAR(16) NOT NULL COMMENT 'ROLE/STORY',
    time_range_mode VARCHAR(16) NOT NULL DEFAULT 'RECENT_DAYS' COMMENT 'FIXED/RECENT_DAYS',
    start_time DATETIME NULL COMMENT '固定范围开始时间',
    end_time DATETIME NULL COMMENT '固定范围结束时间',
    recent_days INT NULL COMMENT '最近天数',
    script_path VARCHAR(1000) NOT NULL COMMENT 'Python脚本路径',
    output_dir VARCHAR(1000) NOT NULL COMMENT '输出目录',
    storage_type VARCHAR(16) NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL/OSS',
    train_ratio DECIMAL(6,5) NOT NULL DEFAULT 0.90000 COMMENT '训练集比例',
    eval_ratio DECIMAL(6,5) NOT NULL DEFAULT 0.10000 COMMENT '评估集比例',
    run_params_json TEXT NULL COMMENT '附加运行参数JSON',
    cron_expression VARCHAR(100) NULL COMMENT 'Quartz Cron表达式',
    enabled TINYINT NOT NULL DEFAULT 0 COMMENT '0停用/1启用',
    timeout_seconds INT NOT NULL DEFAULT 3600 COMMENT '执行超时秒数',
    running_execution_id BIGINT NULL COMMENT '当前运行记录ID',
    last_run_at DATETIME NULL COMMENT '最近触发时间',
    create_by VARCHAR(32) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(32) NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '0正常/1删除',
    KEY idx_recommend_etl_task_enabled (enabled, del_flag),
    KEY idx_recommend_etl_task_type (recommend_type, del_flag),
    KEY idx_recommend_etl_task_running (running_execution_id)
) COMMENT='推荐训练数据ETL任务';

CREATE TABLE IF NOT EXISTS ts_recommend_etl_execution (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT 'ETL任务ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称快照',
    recommend_type VARCHAR(16) NOT NULL COMMENT 'ROLE/STORY',
    trigger_type VARCHAR(16) NOT NULL COMMENT 'MANUAL/SCHEDULED',
    status VARCHAR(16) NOT NULL COMMENT 'WAITING/RUNNING/SUCCESS/FAILED',
    range_start_time DATETIME NOT NULL COMMENT '本次数据开始时间',
    range_end_time DATETIME NOT NULL COMMENT '本次数据结束时间',
    arguments_json TEXT NULL COMMENT '本次执行参数快照',
    started_at DATETIME NULL COMMENT '进程开始时间',
    finished_at DATETIME NULL COMMENT '进程结束时间',
    duration_ms BIGINT NULL COMMENT '执行耗时毫秒',
    process_exit_code INT NULL COMMENT 'Python进程退出码',
    train_count BIGINT NULL,
    eval_count BIGINT NULL,
    positive_count BIGINT NULL,
    negative_count BIGINT NULL,
    train_path VARCHAR(1000) NULL,
    eval_path VARCHAR(1000) NULL,
    result_json MEDIUMTEXT NULL COMMENT 'Python结果JSON',
    log_path VARCHAR(1000) NULL COMMENT '完整日志文件路径',
    log_content MEDIUMTEXT NULL COMMENT '截断后的运行日志',
    error_code VARCHAR(64) NULL COMMENT '机器错误码',
    error_message TEXT NULL COMMENT '错误信息',
    create_by VARCHAR(32) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_recommend_etl_execution_task (task_id, create_time, id),
    KEY idx_recommend_etl_execution_status (status, create_time)
) COMMENT='推荐训练数据ETL执行记录';

-- 探拾后台菜单，按 URL 和角色授权做幂等写入。
INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
    '2083000000000000001', parent.id, '推荐数据任务',
    '/tanshi/recommend-etl', 'system/tanshi/recommendEtl/index', 1, '', NULL,
    1, NULL, '0', 8.00, 0, 'ant-design:database-outlined', 1,
    0, 0, 0, '推荐训练数据ETL任务与执行记录管理', 'admin', NOW(),
    'admin', NOW(), 0, 0, NULL, 0
FROM sys_permission parent
WHERE parent.id = '2063098067229384705'
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission existing
      WHERE existing.url = '/tanshi/recommend-etl' AND existing.del_flag = 0
  );

INSERT INTO sys_role_permission (
    id, role_id, permission_id, data_rule_ids, operate_date, operate_ip
)
SELECT
    '2083000000000000101', role.id, '2083000000000000001',
    NULL, NOW(), '0:0:0:0:0:0:0:1'
FROM sys_role role
WHERE role.role_code = 'admin'
  AND EXISTS (
      SELECT 1 FROM sys_permission permission
      WHERE permission.id = '2083000000000000001' AND permission.del_flag = 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = '2083000000000000001'
  );

-- 影响范围：新增推荐 ETL 任务、执行记录和管理员菜单，不修改已有业务数据。
-- 回滚：
-- DELETE FROM sys_role_permission WHERE permission_id = '2083000000000000001';
-- DELETE FROM sys_permission WHERE id = '2083000000000000001';
-- DROP TABLE IF EXISTS ts_recommend_etl_execution;
-- DROP TABLE IF EXISTS ts_recommend_etl_task;
