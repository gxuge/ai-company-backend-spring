package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 角色公开发布记录表。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_role_public")
public class TsRolePublic implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 角色ID。 */
    @TableField("role_id")
    private Long roleId;
    /** 渠道编码。 */
    @TableField("channel_code")
    private String channelCode;
    /** 状态：draft/pending/online/offline/rejected。 */
    private String status;
    /** 展示标题。 */
    @TableField("display_title")
    private String displayTitle;
    /** 展示副标题。 */
    @TableField("display_subtitle")
    private String displaySubtitle;
    /** 展示封面。 */
    @TableField("cover_image_url")
    private String coverImageUrl;
    /** 展示简介。 */
    @TableField("intro_text")
    private String introText;
    /** 排序值。 */
    @TableField("sort_order")
    private Integer sortOrder;
    /** 上架时间。 */
    @TableField("published_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishedAt;
    /** 下架时间。 */
    @TableField("offline_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date offlineAt;
    /** 驳回原因。 */
    @TableField("reject_reason")
    private String rejectReason;
    /** 扩展JSON。 */
    @TableField("ext_json")
    private String extJson;
    /** 创建人。 */
    @TableField("create_by")
    private String createBy;
    /** 创建时间。 */
    @TableField("create_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /** 更新人。 */
    @TableField("update_by")
    private String updateBy;
    /** 更新时间。 */
    @TableField("update_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
