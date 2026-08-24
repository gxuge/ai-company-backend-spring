package org.jeecg.modules.system.vo.tsad;

import lombok.Data;

import java.util.Date;

/** 后台广告内容响应。 */
@Data
public class TsAdContentVo {
    /** 广告内容ID。 */
    private Long id;
    /** 广告位ID。 */
    private Long slotId;
    /** 广告位编码。 */
    private String slotCode;
    /** 广告位名称。 */
    private String slotName;
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
    /** 状态。 */
    private String status;
    /** 排序。 */
    private Integer sortOrder;
    /** 投放开始时间。 */
    private Date startTime;
    /** 投放结束时间。 */
    private Date endTime;
    /** 扩展展示参数JSON。 */
    private String extJson;
    /** 最近发布时间。 */
    private Date publishAt;
    /** 最近下线时间。 */
    private Date offlineAt;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
