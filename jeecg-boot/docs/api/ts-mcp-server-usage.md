# TS MCP Server 使用说明

## 1. 说明

当前项目已新增一个轻量级 TS MCP Server，用于对外暴露只读工具能力。

当前仅开放 1 个工具：

- `list_public_ts_roles`：获取公开角色列表

服务端入口代码：

- `org.jeecg.modules.system.controller.TsMcpServerController`

## 2. 配置项

配置值放在：

- `jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.properties`
- `jeecg-module-system/jeecg-system-start/src/main/resources/application-prod.properties`

当前配置项如下：

```properties
# MCP Server
JEECG_AIRAG_MCP_SERVER_ENABLED=true
JEECG_AIRAG_MCP_SERVER_API_KEY=
JEECG_AIRAG_MCP_SERVER_DEFAULT_ROLE_LIST_LIMIT=10
JEECG_AIRAG_MCP_SERVER_MAX_ROLE_LIST_LIMIT=50
```

字段说明：

- `JEECG_AIRAG_MCP_SERVER_ENABLED`：是否启用 TS MCP Server
- `JEECG_AIRAG_MCP_SERVER_API_KEY`：可选的 API Key；为空时不校验
- `JEECG_AIRAG_MCP_SERVER_DEFAULT_ROLE_LIST_LIMIT`：角色列表默认返回条数
- `JEECG_AIRAG_MCP_SERVER_MAX_ROLE_LIST_LIMIT`：角色列表最大返回条数

## 3. 访问地址

默认本地启动地址：

- 说明页：`http://127.0.0.1:8080/jeecg-boot/ts/mcp/info`
- SSE 入口：`http://127.0.0.1:8080/jeecg-boot/ts/mcp/sse`
- JSON-RPC 消息入口：`http://127.0.0.1:8080/jeecg-boot/ts/mcp/message`

如果你改了端口或 context-path，请按实际环境替换。

## 4. 鉴权方式

如果配置了：

```properties
JEECG_AIRAG_MCP_SERVER_API_KEY=your-key
```

则请求头需要带：

```http
X-MCP-KEY: your-key
```

如果 `JEECG_AIRAG_MCP_SERVER_API_KEY` 为空，则当前接口不校验 Key。

## 5. 快速自测

### 5.1 打开说明页

直接访问：

```text
http://127.0.0.1:8080/jeecg-boot/ts/mcp/info
```

若成功，会返回：

- `sseUrl`
- `messageUrl`
- `tools`
- `authHeader`

### 5.2 查询工具列表

```bash
curl -X POST "http://127.0.0.1:8080/jeecg-boot/ts/mcp/message" ^
  -H "Content-Type: application/json" ^
  -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}"
```

如果配置了 API Key：

```bash
curl -X POST "http://127.0.0.1:8080/jeecg-boot/ts/mcp/message" ^
  -H "Content-Type: application/json" ^
  -H "X-MCP-KEY: your-key" ^
  -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}"
```

### 5.3 调用角色列表工具

```bash
curl -X POST "http://127.0.0.1:8080/jeecg-boot/ts/mcp/message" ^
  -H "Content-Type: application/json" ^
  -d "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"list_public_ts_roles\",\"arguments\":{\"keyword\":\"老师\",\"limit\":5}}}"
```

带 API Key 的版本：

```bash
curl -X POST "http://127.0.0.1:8080/jeecg-boot/ts/mcp/message" ^
  -H "Content-Type: application/json" ^
  -H "X-MCP-KEY: your-key" ^
  -d "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"list_public_ts_roles\",\"arguments\":{\"keyword\":\"老师\",\"limit\":5}}}"
```

## 6. 当前工具说明

### `list_public_ts_roles`

用途：

- 查询公开角色列表
- 仅返回安全摘要字段

支持参数：

- `keyword`：可选，按角色名、角色副标题、简介模糊搜索
- `limit`：可选，返回数量限制

返回字段示例：

- `id`
- `roleName`
- `roleSubtitle`
- `avatarUrl`
- `gender`
- `occupation`
- `introText`
- `updatedAt`

## 7. 备注

当前版本是最小可用 MCP Server：

- 仅开放只读能力
- 不涉及角色生成、故事生成等写操作
- 适合先做本地联调和线上接入验证

后续如果需要，可以继续扩展：

- 更多只读工具
- 带权限控制的业务工具
- 更完整的 MCP 工具白名单和审计能力
