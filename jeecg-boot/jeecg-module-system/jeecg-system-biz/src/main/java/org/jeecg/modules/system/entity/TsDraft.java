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
 * 用户统一草稿实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_draft")
public class TsDraft implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 草稿主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 草稿所属用户 ID。 */
    @TableField("user_id")
    private String userId;

    /** 草稿类型：role 角色，story 故事。 */
    @TableField("draft_type")
    private String draftType;

    /** 草稿箱展示名称。 */
    @TableField("draft_name")
    private String draftName;

    /** 来源正式资源 ID，可为空。 */
    @TableField("source_id")
    private Long sourceId;

    /** 页面完整状态 JSON。 */
    @TableField("content_json")
    private String contentJson;

    /** 状态：1 正常，0 已删除。 */
    @TableField("status")
    private Integer status;

    /** 创建时间。 */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
