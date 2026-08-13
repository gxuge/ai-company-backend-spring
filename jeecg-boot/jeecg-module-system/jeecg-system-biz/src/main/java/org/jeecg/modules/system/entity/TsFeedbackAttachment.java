package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 反馈附件实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_feedback_attachment")
public class TsFeedbackAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 附件主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属反馈 ID。 */
    @TableField("feedback_id")
    private Long feedbackId;

    /** 文件地址。 */
    @TableField("file_url")
    private String fileUrl;

    /** 文件类型：image、screenshot、log。 */
    @TableField("file_type")
    private String fileType;

    /** 逻辑删除：0 正常，1 已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间。 */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
