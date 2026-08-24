package org.jeecg.modules.system.event;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** Kafka 中传输的统一推荐行为事件。 */
@Data
@Accessors(chain = true)
public class TsBehaviorEventMessage {
    /** 事件幂等ID。 */
    private String eventId;
    /** 事件类型。 */
    private String eventType;
    /** 事件结构版本。 */
    private Integer eventVersion;
    /** 登录用户ID。 */
    private String userId;
    /** 匿名访客ID。 */
    private String anonymousId;
    /** 访问会话ID。 */
    private String sessionId;
    /** 资源类型。 */
    private String resourceType;
    /** 资源ID。 */
    private String resourceId;
    /** 推荐曝光链路ID。 */
    private String impressionId;
    /** 内容位置。 */
    private Integer position;
    /** 页面路径。 */
    private String pagePath;
    /** 平台。 */
    private String platform;
    /** 停留时长毫秒。 */
    private Long durationMs;
    /** 扩展属性JSON。 */
    private String propertiesJson;
    /** 客户端发生时间。 */
    private Date occurredAt;
    /** 服务端接收时间。 */
    private Date receivedAt;
}
