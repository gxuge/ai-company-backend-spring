# 后端架构说明

## 1. 项目概览
- 项目根目录：`jeecg-boot`
- 技术栈：Spring Boot 3.x、MyBatis-Plus、Shiro（权限）、可选 Spring Cloud
- 运行模式：支持单体模式与云模式

## 2. 模块结构（基于根 `pom.xml`）

| 模块 | 职责 | 典型内容 |
|---|---|---|
| `jeecg-boot-base-core` | 公共基础能力 | 工具类、基础配置、通用封装 |
| `jeecg-module-system` | 系统管理域 | 登录、用户、角色、权限、系统配置 |
| `jeecg-boot-module` | 业务模块域 | `airag`、demo 等业务能力 |
| `jeecg-server-cloud` | 云化部署相关 | gateway、nacos、sentinel、xxl-job 等 |

## 3. 启动入口与运行形态

### 3.1 单体入口
- 启动类：`jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/JeecgSystemApplication.java`
- 应用名：`jeecg-system`
- 默认端口（dev）：`8080`
- 默认上下文：`/jeecg-boot`

### 3.2 云模式入口（按需）
- `jeecg-server-cloud/jeecg-system-cloud-start/.../JeecgSystemCloudApplication.java`
- `jeecg-server-cloud/jeecg-cloud-gateway/.../JeecgGatewayApplication.java`

## 4. 配置激活机制
- `application.yml` 使用 `spring.profiles.active: '@profile.name@'`。
- 根 `pom.xml` 开启 `resources filtering`，通过 Maven Profile 注入 `profile.name`。
- 可用 Profile：`dev`（默认）、`test`、`docker`、`prod`。
- `SpringCloud` Profile 会额外引入 `jeecg-server-cloud` 模块。

## 5. 请求链路与分层
1. 请求进入 `Controller`。
2. 参数校验、登录态/权限校验。
3. `Service` 承担业务编排与事务控制。
4. `Mapper` 访问数据库（`classpath*:org/jeecg/**/xml/*Mapper.xml`）。
5. 返回统一结果对象。

## 6. 当前核心业务域（文档已覆盖）
- 系统鉴权域：`LoginController`（路径前缀 `/sys`）
- AI 业务域：`airag` 模块（路径前缀 `/airag`）

详见：
- `docs/api/sys-auth-api.md`
- `docs/api/airag-api.md`

## 7. 非功能基线
- 安全：禁止提交真实密钥，敏感能力必须有权限与审计。
- 稳定性：外部依赖调用需控制超时与异常兜底。
- 可观测性：关键链路输出可追踪日志与必要指标。

## 8. 维护规则
当以下内容发生变化时，必须更新本文件：
- 模块边界变化
- 启动入口变化
- 配置激活机制变化
- 核心调用链变化
