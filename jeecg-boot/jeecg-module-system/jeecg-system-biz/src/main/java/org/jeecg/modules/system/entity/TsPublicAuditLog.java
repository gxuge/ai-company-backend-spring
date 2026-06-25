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
 * 公开操作审计表。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_public_audit_log")
public class TsPublicAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 对象类型：role/story。 */
    @TableField("target_type")
    private String targetType;
    /** 公开记录ID。 */
    @TableField("public_id")
    private Long publicId;
    /** 动作类型：submit/approve/reject/online/offline。 */
    @TableField("action_type")
    private String actionType;
    /** 变更前状态。 */
    @TableField("before_status")
    private String beforeStatus;
    /** 变更后状态。 */
    @TableField("after_status")
    private String afterStatus;
    /** 备注。 */
    private String remark;
    /** 操作人。 */
    @TableField("operate_by")
    private String operateBy;
    /** 操作时间。 */
    @TableField("operate_time")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operateTime;
}
