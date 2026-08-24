package org.jeecg.modules.system.dto.tsad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/** 广告曝光或点击事件上报参数。 */
@Data
public class TsAdEventReportDto {
    /** 客户端生成的事件幂等ID。 */
    @NotBlank
    private String eventId;
    /** 广告内容ID。 */
    @NotNull
    private Long contentId;
    /** 广告位编码。 */
    @NotBlank
    private String slotCode;
    /** 事件类型：IMPRESSION/CLICK。 */
    @NotBlank
    private String eventType;
    /** 匿名访客ID，匿名请求建议必传。 */
    private String visitorId;
    /** 平台：WEB/IOS/ANDROID。 */
    @NotBlank
    private String platform;
    /** 客户端事件发生时间，为空时使用服务端时间。 */
    private Date occurredAt;
}
