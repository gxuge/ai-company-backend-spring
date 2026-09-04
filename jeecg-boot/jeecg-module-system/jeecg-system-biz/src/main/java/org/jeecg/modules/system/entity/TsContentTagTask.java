package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 内容标签异步任务。 */
@Data
@TableName("ts_content_tag_task")
public class TsContentTagTask implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("review_id")
    private Long reviewId;
    @TableField("content_type")
    private String contentType;
    @TableField("content_id")
    private Long contentId;
    @TableField("content_version")
    private Integer contentVersion;
    @TableField("content_hash")
    private String contentHash;
    private String status;
    @TableField("retry_count")
    private Integer retryCount;
    @TableField("last_error_message")
    private String lastErrorMessage;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;
}
