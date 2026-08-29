# 推荐训练数据 ETL 管理 API

## 通用约束
- 基础路径：`/sys/ts-recommend-etl`
- 权限：仅 `admin` 角色
- 统一响应：`Result<T>`
- 执行状态：`WAITING/RUNNING/SUCCESS/FAILED`
- 推荐类型：`ROLE/STORY`

## 任务管理

### `POST /task/page`
分页查询任务。请求支持 `pageNo/pageSize/keyword/recommendType/enabled`。

### `GET /task/detail?id={id}`
查询任务完整配置。

### `POST /task/create`
新增任务。主要字段：
- `taskName`、`recommendType`
- `timeRangeMode`：`FIXED/RECENT_DAYS`
- `startTime/endTime` 或 `recentDays`
- `scriptPath`、`outputDir`、`storageType`
- `trainRatio/evalRatio`，两者之和必须为 1
- `runParamsJson`：仅允许 JSON 对象
- `cronExpression`、`enabled`、`timeoutSeconds`

### `POST /task/update`
更新任务，请求体必须包含 `id`。运行中的任务禁止编辑，避免中途改变脚本和输出配置。

### `POST /task/delete`
请求体：`{"id": 1}`。运行中的任务禁止删除。

### `POST /task/toggle`
请求体：`{"id": 1, "enabled": 1}`。启用时必须已有合法 Cron。

### `POST /task/execute`
请求体：`{"id": 1}`。返回新建的 WAITING 执行记录；同一任务正在运行时拒绝重复触发。

## 执行记录

### `POST /execution/page`
分页查询执行记录。支持 `taskId/keyword/recommendType/status/triggerType`。

### `GET /execution/detail?id={id}`
查询时间范围、参数快照、退出码、样本数量、文件路径、错误、结果 JSON 和截断日志。

## Python 约定
Java 以参数数组启动 Python，不经过 Shell。固定参数：

```text
--start=yyyy-MM-dd HH:mm:ss
--end=yyyy-MM-dd HH:mm:ss
--type=role|story
--output=...
--storage=local|oss
--train-ratio=0.9
--eval-ratio=0.1
```

最后一条非空标准输出必须是结果 JSON：

```json
{
  "success": true,
  "train_count": 100000,
  "eval_count": 10000,
  "positive_count": 30000,
  "negative_count": 80000,
  "train_path": "...",
  "eval_path": "..."
}
```

非零退出码、`success=false`、字段缺失、超时或结果 JSON 不合法均记录为 FAILED。
