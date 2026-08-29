package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** ClickHouse 用户业务行为事件明细。 */
@Data
@Accessors(chain = true)
@TableName("ts_user_behavior_event")
public class TsUserBehaviorEvent {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
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
    /** 明细入库时间。 */
    private Date createdAt;
}
