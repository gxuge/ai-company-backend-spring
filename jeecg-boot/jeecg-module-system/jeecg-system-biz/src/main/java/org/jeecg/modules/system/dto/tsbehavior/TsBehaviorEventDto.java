package org.jeecg.modules.system.dto.tsbehavior;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.jeecg.modules.system.jackson.TsEventOccurredAtDeserializer;

import java.util.Date;
import java.util.Map;

/** 业务行为事件上报参数。 */
@Data
public class TsBehaviorEventDto {
    /** 客户端生成的事件幂等ID。 */
    @NotBlank
    @Size(max = 64)
    private String eventId;
    /** 业务事件类型。 */
    @NotBlank
    private String eventType;
    /** 事件结构版本，默认2。 */
    @Min(1)
    @Max(100)
    private Integer eventVersion;
    /** 客户端访问会话ID。 */
    @NotBlank
    @Size(max = 64)
    private String sessionId;
    /** 资源类型，例如 story、role。 */
    private String resourceType;
    /** 资源ID。 */
    @Size(max = 64)
    private String resourceId;
    /** 事件发生页面路径。 */
    @Size(max = 500)
    private String pagePath;
    /** 平台：WEB/IOS/ANDROID，默认WEB。 */
    @Size(max = 16)
    private String platform;
    /** 事件限定扩展属性，不允许存放敏感明文或标签。 */
    private Map<String, Object> properties;
    /** 客户端事件发生时间，为空时使用服务端时间。 */
    @JsonDeserialize(using = TsEventOccurredAtDeserializer.class)
    private Date occurredAt;
}
