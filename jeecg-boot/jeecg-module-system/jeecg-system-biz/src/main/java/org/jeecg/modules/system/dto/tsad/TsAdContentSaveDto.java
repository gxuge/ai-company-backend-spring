package org.jeecg.modules.system.dto.tsad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/** 广告内容保存参数，更新时必须传ID。 */
@Data
public class TsAdContentSaveDto {
    /** 广告内容ID。 */
    private Long id;
    /** 广告位ID。 */
    @NotNull
    private Long slotId;
    /** 内容编码；创建时为空则由后端生成。 */
    private String contentCode;
    /** 标题。 */
    @NotBlank
    private String title;
    /** 副标题。 */
    private String subtitle;
    /** 素材来源：SELF/EXTERNAL/AD_NETWORK。 */
    private String sourceType;
    /** 媒体类型：IMAGE/VIDEO/CARD。 */
    private String mediaType;
    /** 素材地址；卡片类型为空。 */
    private String mediaUrl;
    /** 视频封面地址。 */
    private String posterUrl;
    /** 卡片类型：PROMOTION/ROLE/STORY/CUSTOM。 */
    private String cardType;
    /** 卡片内容JSON对象。 */
    private String payloadJson;
    /** 兼容旧版本的图片地址。 */
    private String imageUrl;
    /** 动作类型：NONE/URL/ROUTE/ROLE/STORY/DEEP_LINK。 */
    private String actionType;
    /** 动作目标。 */
    private String actionPayload;
    /** 兼容旧版本的跳转类型。 */
    private String linkType;
    /** 兼容旧版本的跳转目标。 */
    private String linkValue;
    /** 排序，越小越靠前。 */
    private Integer sortOrder;
    /** 投放开始时间。 */
    private Date startTime;
    /** 投放结束时间。 */
    private Date endTime;
    /** 扩展展示参数JSON对象或数组字符串。 */
    private String extJson;
}
