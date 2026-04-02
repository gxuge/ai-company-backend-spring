# API 文档与接口开发统一规范

## 1. 适用范围
- 适用于 `jeecg-boot` 项目所有 API 文档编写与 Spring Boot 接口开发。
- 覆盖 `Controller/Service/Mapper/DTO/PO/VO/Entity` 以及 Mapper XML 的实现规范。

## 2. API 文档目录规则
- 按业务域拆分文档文件。
- 业务域文档命名规范：`<领域>-api.md`。
- 接口资产盘点文档固定命名：`hardness-api-inventory.md`。
- 示例：`sys-auth-api.md`、`airag-api.md`、`ts-api.md`、`hardness-api-inventory.md`。

## 3. 单接口文档描述模板
每个接口至少包含：
- 接口路径与方法：`GET/POST/PUT/PATCH/DELETE`
- 用途说明
- 鉴权/权限要求
- 关键请求参数
- 返回结果说明（含异常分支）
- 兼容性说明（新增/修改/废弃）

## 4. Spring Boot 接口开发标准

### 4.1 核心目标
- 基于现有 JEECG Boot 结构，交付可维护、可扩展、可联调的四层接口。
- 统一 `Controller/Service/Mapper/DTO-PO-VO` 职责边界，避免“能跑但不统一”。
- 优先复用 `TsRole` 的成熟实现模式，减少风格漂移。

### 4.2 项目路径约定
- 业务代码：`jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system`
- Mapper XML：`jeecg-module-system/jeecg-system-biz/src/main/java/org/jeecg/modules/system/mapper/xml`

### 4.3 TsRole 基线结构（默认）
- `controller/TsXxxController.java`
- `service/ITsXxxService.java`
- `service/impl/TsXxxServiceImpl.java`
- `mapper/TsXxxMapper.java`
- `mapper/xml/TsXxxMapper.xml`
- `dto/tsxxx/TsXxxQueryDto.java`
- `dto/tsxxx/TsXxxSaveDto.java`
- `po/tsxxx/TsXxxQueryPo.java`
- `po/tsxxx/TsXxxSavePo.java`
- `vo/tsxxx/TsXxxVo.java`
- `vo/tsxxx/TsXxxVoConverter.java`
- `entity/TsXxx.java`

### 4.4 开发流程（强约束）
1. 明确接口类型：通用 CRUD 优先 MP；复杂查询与聚合优先自定义 DTO + Mapper XML。
2. 先建模型：DTO/PO/VO/Entity 先齐，再落 Controller/Service/Mapper。
3. Controller 只做参数接收、登录用户提取、服务调用，不写复杂业务。
4. 登录认证统一由 Controller 层 `@RequiresAuthentication` 负责；Service 不重复做 `JeecgBoot401Exception` 登录校验，默认直接使用 Controller 传入的 `LoginUser`。
5. Service 负责业务规则、归属校验、事务边界；写操作统一 `@Transactional(rollbackFor = Exception.class)`。
6. 异常统一抛出：业务异常 `JeecgBootException`。认证异常 `JeecgBoot401Exception` 仅用于未接入 `@RequiresAuthentication` 的特殊入口。
7. 分页统一：默认 `pageNo=1`、默认 `pageSize=10`、最大 `pageSize=100`。
8. 用户私有数据必须显式归属过滤：`user_id = #{userId}`。
9. 列表查询必须显式 `ORDER BY`，避免分页抖动。

### 4.5 接口语义与 ID 传参约定
- 保持 REST 语义：`GET` 查、`POST` 增、`PUT/PATCH` 改、`DELETE` 删。
- 资源命名优先复数，如 `/sys/ts-roles`。
- 单资源 `GET/DELETE` 统一 query 传 `id`。
- `PUT/PATCH` 在请求体中传 `id`。
- `POST` 创建不传 `id`。

### 4.6 注释强制规则（保留）
- DTO/VO/PO/Entity 的每个字段必须有中文注释。
- 状态值、枚举值、0/1 布尔语义必须在注释中写明取值含义。
- Controller/Service/Mapper/PO 的每个方法（含 private helper）必须有中文注释。
- 涉及权限、事务、分页、SQL 条件拼装的方法，注释中必须说明边界条件。

### 4.7 Service/Controller/Mapper/XML 方法约束（强制）
- 登录态校验边界固定：Controller 使用 `@RequiresAuthentication`，Service 不重复写登录态空值判断。
- 登录用户对象使用边界固定：Service 默认直接使用 Controller 传入的 `LoginUser`（如 `user.getId()`）；禁止在 Service 里重复编写 `user == null`、`userId 为空` 这类登录态兜底分支。
- 资源 ID 与归属校验边界固定：`id/storyId/chapterId/roleId` 等资源标识的存在性与归属校验统一通过 AOP 注解处理（如 `@CheckTsStoryOwnership`、`@CheckTsStoryChapterOwnership`、`@CheckTsRoleOwnership`），Service 不重复编写 `if (id == null)`/`if (storyId == null)` 这类校验分支。
- 非归属型“资源存在性”同样 AOP 化：如 `voiceProfileId`、`modelId`、`templateId` 等仅需校验“是否存在”的外键资源，统一使用 `@CheckXxxExists` 注解在切面中校验；Service 不再直接写 `count/select + throw`。
- AOP 注解设计约束：每个注解必须支持 `message` 自定义错误文案；切面需支持从 `Long/Integer/String` 及常见 DTO getter（如 `getId()`、`getXxxId()`）提取资源 ID；校验通过后再进入 Service 业务逻辑。
- 切面提示与注释规范：AOP 切面中的注解默认 `message` 与兜底 `errorMsg` 必须使用中文；切面类、注解、环绕通知与关键提取方法必须补充中文注释说明功能。
- ServiceImpl 中仅保留接口声明对应的 `@Override` 业务方法。
- 非 `@Override` 的私有 helper 方法，优先下沉到 DTO/PO/VO（如 `fromRequest`、`applyTo`、`VoConverter`）或通过注解/AOP 复用。
- 若无法下沉或 AOP 化，则在对应 `@Override` 方法中直接编写普通代码，不再新增私有方法。
- Controller/Mapper 层同样遵循“最小方法面”：仅保留必要入口方法与映射声明，不堆积无复用私有方法。

### 4.8 Service 中文注释规范（强制）
- Service 接口与 ServiceImpl 的每个业务方法都必须有中文注释。
- 注释至少说明：用途、关键输入参数、返回语义、异常边界。
- 对分页、归属校验、事务、软删除等关键流程，必须在方法注释中写明。

### 4.9 Mapper XML 规范（强制）
- XML 统一放在 `src/main/java/.../mapper/xml`，禁止新建到 `src/main/resources/mapper/system`。
- `namespace` 必须与 Mapper 接口全限定名一致，`id` 与方法名一致。
- XML 文本节点中禁止直接写 `<`、`<=`、`<>`，避免解析失败。
- 非等于优先用 `!=`；必须用 `<` 时改 `&lt;` / `&lt;=` 或 `CDATA`。
- 软删除过滤默认写法优先：`status != 0` 或 `is_deleted = 0`。

### 4.10 文件编码规范（强制）
- 默认编码：`UTF-8`。
- SQL 导入脚本（尤其首行含 `SET NAMES`）必须使用 `UTF-8 without BOM`，避免不可见字符导致执行异常。
- 修改已有文件时必须保持原文件编码与 BOM 状态；禁止无感转码。
- 禁止在同一次业务改动中混入“批量转码”。如需转码，单独提交并专项验证。
- 乱码自检：若出现 `锟�`、`�`、中文注释异常显示，视为编码问题，必须先修复再继续开发。
- XML/Java/SQL/Markdown/Properties/YAML 均按以上规则执行，不得例外。

### 4.11 启动前验收闭环（强制）
1. 校验 Mapper XML：`namespace/id`、运算符转义、动态 SQL 标签是否闭合。
2. 清理产物：至少清理对应模块 `target`，避免旧 XML 残留。
3. 启动验证：观察是否出现 `Failed to parse mapping resource`。
4. 接口冒烟：分页、详情、新增、编辑、删除各跑一遍。
5. 归属验证：越权访问必须失败。

### 4.12 常见故障快速定位
- `Failed to parse mapping resource`：先查指定 XML 的语法与运算符，再查 `namespace/id`。
- `No space left on device (Errcode: 28)`：先处理磁盘空间与 MySQL 数据目录空间。
- 外键创建失败：先对齐字段类型、长度、字符集、排序规则。
- SQL 首行语句异常：先检查文件是否 BOM 和编码是否匹配。

### 4.13 交付清单
- Controller
- Service 接口
- ServiceImpl
- Mapper 接口
- Mapper XML（复杂查询必有）
- DTO（Query + Save）
- PO（Query + Save，按需）
- Entity（新表必有）
- VO + VoConverter（聚合输出必有）

## 5. 变更管理
- 任何 API 行为变化都要在 `docs/changelog.md` 记录一条变更。
- 破坏性修改必须写迁移说明（旧参数兼容期、替代接口）。
- 改动 `Controller` 时必须同步更新对应 `docs/api/*.md`。

## 6. 质量基线
- 示例字段需与真实 DTO/VO 一致。
- 不允许出现“隐式魔法值”而不解释。
- 废弃接口必须标注预计下线时间。
- 复杂查询必须有可复查 SQL（Mapper XML）与必要注释。

## 7. 当前已落地 API 文档
- `sys-auth-api.md`：登录与认证相关接口
- `airag-api.md`：AI/RAG 业务相关接口
- `ts-api.md`：`Ts*Controller` 相关接口清单（系统内 TS 业务域）
- `hardness-api-inventory.md`：接口资产扫描基线（含 AI 伴侣对话链路）
