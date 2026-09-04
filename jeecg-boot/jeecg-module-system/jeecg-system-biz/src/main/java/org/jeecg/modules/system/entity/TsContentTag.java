package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 角色与故事内容标签。 */
@Data
@TableName("ts_content_tag")
public class TsContentTag implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("content_type")
    private String contentType;
    @TableField("content_id")
    private Long contentId;
    @TableField("content_version")
    private Integer contentVersion;
    @TableField("tag_id")
    private Long tagId;
    private BigDecimal score;
    private String source;
    @TableField("model_version")
    private String modelVersion;
    @TableField("content_hash")
    private String contentHash;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;
}
