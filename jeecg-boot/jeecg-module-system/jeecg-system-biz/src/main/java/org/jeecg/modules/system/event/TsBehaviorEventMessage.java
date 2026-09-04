package org.jeecg.modules.system.event;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/** Kafka 中传输的统一业务行为事件。 */
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
    /** 访问会话ID。 */
    private String sessionId;
    /** 资源类型。 */
    private String resourceType;
    /** 资源ID。 */
    private String resourceId;
    /** 行为发生时的内容版本。 */
    private Integer contentVersion;
    /** 行为发生时的固定标签ID快照。 */
    private List<Long> tagIds;
    /** 与标签ID顺序一致的匹配分数快照。 */
    private List<BigDecimal> tagScores;
    /** 页面路径。 */
    private String pagePath;
    /** 平台。 */
    private String platform;
    /** 扩展属性JSON。 */
    private String propertiesJson;
    /** 客户端发生时间。 */
    private Date occurredAt;
    /** 服务端接收时间。 */
    private Date receivedAt;
}
