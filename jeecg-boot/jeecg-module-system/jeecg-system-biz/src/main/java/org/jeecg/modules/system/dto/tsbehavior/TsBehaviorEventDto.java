package org.jeecg.modules.system.dto.tsbehavior;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.jeecg.modules.system.jackson.TsEventOccurredAtDeserializer;

import java.util.Date;
import java.util.Map;

/** 推荐行为事件上报参数。 */
@Data
public class TsBehaviorEventDto {
    /** 客户端生成的事件幂等ID。 */
    @NotBlank
    @Size(max = 64)
    private String eventId;
    /** 事件类型，例如 view、click、like。 */
    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9_]{1,63}")
    private String eventType;
    /** 事件结构版本，默认1。 */
    @Min(1)
    @Max(100)
    private Integer eventVersion;
    /** 客户端访问会话ID。 */
    @NotBlank
    @Size(max = 64)
    private String sessionId;
    /** 匿名访客ID，预留用于后续身份拼接。 */
    @Size(max = 64)
    private String anonymousId;
    /** 资源类型，例如 story、role、feedback。 */
    @Pattern(regexp = "[a-z][a-z0-9_]{1,63}")
    private String resourceType;
    /** 资源ID。 */
    @Size(max = 64)
    private String resourceId;
    /** 推荐曝光链路ID。 */
    @Size(max = 64)
    private String impressionId;
    /** 内容在列表中的位置，从0开始。 */
    @Min(0)
    @Max(100000)
    private Integer position;
    /** 事件发生页面路径。 */
    @Size(max = 500)
    private String pagePath;
    /** 平台：WEB/IOS/ANDROID，默认WEB。 */
    @Size(max = 16)
    private String platform;
    /** 停留时长毫秒，最大24小时。 */
    @Min(0)
    @Max(86400000)
    private Long durationMs;
    /** 扩展属性，不允许存放敏感明文。 */
    private Map<String, Object> properties;
    /** 客户端事件发生时间，为空时使用服务端时间。 */
    @JsonDeserialize(using = TsEventOccurredAtDeserializer.class)
    private Date occurredAt;
}
