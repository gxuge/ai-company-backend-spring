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
 * 公开渠道表。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_public_channel")
public class TsPublicChannel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 渠道编码。 */
    @TableField("channel_code")
    private String channelCode;
    /** 渠道名称。 */
    @TableField("channel_name")
    private String channelName;
    /** 渠道图片。 */
    @TableField("channel_image_url")
    private String channelImageUrl;
    /** 适用对象：role/story/both。 */
    @TableField("target_type")
    private String targetType;
    /** 状态：enabled/disabled。 */
    private String status;
    /** 排序值。 */
    @TableField("sort_order")
    private Integer sortOrder;
    /** 备注。 */
    private String remark;
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
