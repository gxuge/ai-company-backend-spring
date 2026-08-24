package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 广告内容。 */
@Data
@Accessors(chain = true)
@TableName("ts_ad_content")
public class TsAdContent {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 广告位ID。 */
    private Long slotId;
    /** 广告内容编码。 */
    private String contentCode;
    /** 标题。 */
    private String title;
    /** 副标题。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String subtitle;
    /** 素材来源：SELF/EXTERNAL/AD_NETWORK。 */
    private String sourceType;
    /** 媒体类型：IMAGE/VIDEO/CARD。 */
    private String mediaType;
    /** 规范化素材地址；卡片类型为空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String mediaUrl;
    /** 视频封面地址。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String posterUrl;
    /** 卡片类型：PROMOTION/ROLE/STORY/CUSTOM。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String cardType;
    /** 卡片内容JSON对象。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String payloadJson;
    /** 兼容旧版本的图片地址。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String imageUrl;
    /** 动作类型：NONE/URL/ROUTE/ROLE/STORY/DEEP_LINK。 */
    private String actionType;
    /** 动作目标。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String actionPayload;
    /** 兼容旧版本的跳转类型。 */
    private String linkType;
    /** 兼容旧版本的跳转目标。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String linkValue;
    /** 状态：DRAFT/PUBLISHED/OFFLINE。 */
    private String status;
    /** 排序，越小越靠前。 */
    private Integer sortOrder;
    /** 投放开始时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Date startTime;
    /** 投放结束时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Date endTime;
    /** 扩展展示参数JSON。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String extJson;
    /** 最近发布时间。 */
    private Date publishAt;
    /** 最近下线时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Date offlineAt;
    /** 逻辑删除：0正常，1删除。 */
    @TableLogic
    private Integer isDeleted;
    /** 创建人。 */
    private String createdBy;
    /** 更新人。 */
    private String updatedBy;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
