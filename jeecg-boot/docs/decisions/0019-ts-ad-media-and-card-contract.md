# ADR 0019：广告内容媒体与卡片契约

## 状态

已接受

## 背景

初版广告内容仅支持图片和简单跳转，无法覆盖外部图片、视频、自有运营卡片，
也无法为后续广告网络接入保留统一边界。

## 决策

1. 使用 `sourceType` 区分 `SELF`、`EXTERNAL` 和预留的 `AD_NETWORK`。
2. 使用 `mediaType` 区分 `IMAGE`、`VIDEO`、`CARD`；视频独立保存 `posterUrl`，
   卡片使用 `cardType + payloadJson`。
3. 使用 `actionType + actionPayload` 统一表达外部链接、前端路由、角色/故事详情
   和深层链接。
4. 暂时保留 `imageUrl/linkType/linkValue`，创建和更新时由后端同步写入，保证旧版
   管理端和已有调用可以平滑迁移。
5. 当前管理端只开放自有和外部素材。`AD_NETWORK` 仅作为后端扩展位，未来接入时
   再增加供应商、广告位映射、回调和结算字段，不把供应商细节提前混入基础内容表。

## 约束与风险

- 外部素材和 URL 动作仅接受 HTTP/HTTPS 地址。
- 卡片内容必须是 JSON 对象，由客户端按 `cardType` 解释。
- `image_url` 改为可空，以支持没有媒体地址的卡片；回滚前必须先处理视频和卡片数据。

## 回滚

执行 `db/V3.9.1_51__expand_ts_ad_content_media.sql` 末尾的回滚步骤前，
先导出并删除或迁移 `VIDEO/CARD` 内容；已有图片内容可继续通过旧字段读取。
