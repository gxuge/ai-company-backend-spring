package org.jeecg.modules.system.vo.tsad;

import lombok.Data;

/** 前端广告内容响应。 */
@Data
public class TsAdDeliveryItemVo {
    /** 广告内容ID。 */
    private Long id;
    /** 内容编码。 */
    private String contentCode;
    /** 标题。 */
    private String title;
    /** 副标题。 */
    private String subtitle;
    /** 素材来源。 */
    private String sourceType;
    /** 媒体类型。 */
    private String mediaType;
    /** 规范化素材地址。 */
    private String mediaUrl;
    /** 视频封面地址。 */
    private String posterUrl;
    /** 卡片类型。 */
    private String cardType;
    /** 卡片内容JSON对象。 */
    private String payloadJson;
    /** 兼容旧版本的图片地址。 */
    private String imageUrl;
    /** 动作类型。 */
    private String actionType;
    /** 动作目标。 */
    private String actionPayload;
    /** 兼容旧版本的跳转类型。 */
    private String linkType;
    /** 兼容旧版本的跳转目标。 */
    private String linkValue;
    /** 扩展展示参数JSON。 */
    private String extJson;
}
