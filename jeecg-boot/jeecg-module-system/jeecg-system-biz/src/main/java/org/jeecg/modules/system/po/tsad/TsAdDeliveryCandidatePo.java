package org.jeecg.modules.system.po.tsad;

import lombok.Data;

/** 投放候选内容查询模型。 */
@Data
public class TsAdDeliveryCandidatePo {
    /** 广告位ID。 */
    private Long slotId;
    /** 广告位编码。 */
    private String slotCode;
    /** 广告位类型。 */
    private String slotType;
    /** 建议宽度。 */
    private Integer width;
    /** 建议高度。 */
    private Integer height;
    /** 广告位最多返回数量。 */
    private Integer maxItems;
    /** 广告内容ID。 */
    private Long contentId;
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
    /** 平台数组JSON，规则缺失时为空。 */
    private String platformJson;
    /** 受众类型，规则缺失时为空。 */
    private String audienceType;
    /** 会员等级数组JSON，规则缺失时为空。 */
    private String memberLevelJson;
    /** 指定用户ID数组JSON。 */
    private String userIdJson;
}
